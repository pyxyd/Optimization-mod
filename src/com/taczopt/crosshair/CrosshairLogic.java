package com.taczopt.crosshair;

/**
 * 腰射准星的纯逻辑：扩散量换算 + 空心十字的几何。
 *
 * <p>不引用任何游戏类，方便离线单测（见 {@code tools\CrosshairCheck.java}）。
 *
 * <h2>样式来源</h2>
 * 照《三角洲行动》的腰射准星做的（那个游戏准星可自定义，推荐配置里最常见的是
 * "空心十字 + 中心点 + 外线间距 8"）：
 * <ul>
 *   <li>四条<b>空心</b>线段（中间不连起来），不是实心十字；</li>
 *   <li>可选中心点；</li>
 *   <li>线段离中心的距离（gap）随扩散变化 —— 站定收拢、移动/跳跃张开。</li>
 * </ul>
 *
 * <h2>坐标与尺寸</h2>
 * 全部以"准星中心"为原点，单位是 GUI 像素。屏幕坐标 y 向下为正，
 * 所以上方线段在负 y。
 */
public final class CrosshairLogic {

    private CrosshairLogic() {
    }

    /** 一条要画的矩形（像素，相对准星中心）。 */
    public record Bar(int x0, int y0, int x1, int y1) {
    }

    /**
     * 腰射扩散值 -> 线段离中心的距离（gap，像素）。
     *
     * <h2>公式就是"10 - 当前腰射精度"</h2>
     * TaCZ 枪械数据里的 {@code inaccuracy.stand} 实际上就是"扩散"，
     * 它和玩家看到的"腰射精度"是反的：
     * <pre>
     *   精度 = 10 - 扩散        （枪械数据面板就是这么显示的）
     *   gap  = 10 - 精度 = 扩散
     * </pre>
     * 所以<b>间距直接取扩散值本身</b>，不需要任何换算系数。
     *
     * <p>实测三个枪包里 117 把枪的 {@code stand} 落在 <b>1.25 ~ 8.0</b>
     * （m1911/lonetrail 这种精准手枪 1.25~1.5，m7_prismatic/aug 这种 7.8~8.0），
     * 直接当像素用刚好是合理的准星大小，所以默认倍率就是 1.0。
     *
     * <p>⚠ 早先版本按 {@code 基础间距 + 扩散 * 0.8} 算（拍脑袋的近似），
     * 准星大小和枪的实际精度没有对应关系 —— 打不准就是因为这个。
     *
     * @param inaccuracy 当前的扩散值（= 10 - 腰射精度）
     * @param multiplier 倍率，默认 1.0（想让准星整体更大/更小再调）
     * @param floor      下限像素（间距为 0 的话四条线会连成一坨）
     * @param max        上限像素，防止极端数据把准星甩出屏幕
     */
    public static int gapFor(float inaccuracy, double multiplier, int floor, int max) {
        double v = Math.max(0f, inaccuracy) * (multiplier <= 0 ? 1.0 : multiplier);
        int gap = (int) Math.round(v);
        int lo = Math.max(1, floor);
        int hi = Math.max(lo, max);
        return Math.max(lo, Math.min(gap, hi));
    }

    /**
     * 生成空心十字的四条线段。
     *
     * <p>每条线的长度固定为 {@code arm} 格，内侧离中心 {@code gap} 格，中间留空
     * （这就是"空心十字"，不是实心十字）。
     *
     * <h2>⚠ 关于"右/下各少 1 像素"——别再复刻原版</h2>
     * 原版 {@code Gui.renderCrosshair} 是这么画的（反编译原文）：
     * <pre>
     *   for (int i = -1; i &lt;= 4; ++i) { ... }   // 横向占 -1..4，共 6 像素
     *   if (blend) for (int i = -1; i &lt;= 5; ++i) { ... }  // 纵向占 -1..5，共 7 像素
     * </pre>
     * 注意两侧都是 {@code -1}，所以<b>原版准星本来就不对称</b>：
     * 右边比左边短 1 像素、下边比上边短 1 像素。
     *
     * <p>本模组第一版照抄了这个结构（{@code x1 = half - 1 + (t % 2)}），
     * 于是也继承了这个毛病 —— 实测被用户看出来"右边和下边的线往里靠了 1 像素"。
     * 所以这里<b>按真正的对称来算</b>：中心固定，四条臂逐像素镜像。
     *
     * <h2>对齐规则</h2>
     * <pre>
     *   横向：左半 = [-arm, -(gap+1)]      右半 = [gap+1, arm]
     *   纵向：上半 = [-arm, -(gap+1)]      下半 = [gap+1, arm]
     *   从中心往外的空隙：四边都恰好是 gap 格          （对称，不会一边多一边少）
     *   臂长：四边都恰好 arm 格
     * </pre>
     * 线本身占几列/几行由 {@code t} 决定，居中在轴上；{@code t} 为偶数时
     * 向负方向多占一格（像素网格的必然结果，但<b>两侧臂的几何是对称的</b>）。
     *
     * @param gap       线段内侧离中心的距离（格）
     * @param arm       线段长度（格）
     * @param thickness 线宽（像素）
     */
    public static Bar[] cross(int gap, int arm, int thickness) {
        int g = Math.max(0, gap);
        int a = Math.max(1, arm);
        // 从中心往外的距离（无符号）：留空 g 格，线占 a 格
        int inner = g + 1;
        int outer = g + a;
        int t = Math.max(1, thickness);
        // 线宽在轴上的占位：负侧起、正侧止（偶数宽时负侧多占一格）
        int neg = t / 2;
        int pos = t - t / 2 - 1;
        return new Bar[] {
                // 上：纵向 -outer..-inner，横向以轴为中心
                bar(-neg, -outer, pos, -inner),
                // 下：与上逐格镜像
                bar(-neg, inner, pos, outer),
                // 左：横向 -outer..-inner，纵向以轴为中心
                bar(-outer, -neg, -inner, pos),
                // 右：与左逐格镜像
                bar(inner, -neg, outer, pos),
        };
    }

    /**
     * 建一条线段，并把两个端点<b>归一化</b>成"小在前、大在后"。
     *
     * <p>为什么要归一化：{@code arm} 被夹到 1、{@code gap} 又很小时，
     * {@code inner} 和 {@code outer} 会相等，让区间反向 ——
     * 实测出现过 {@code y[-4..-5]} 这种"起点比终点大"的矩形
     * （靠一段 Java 探针打印真实输出才抓到，光看公式看不出来）。
     * 归一化之后任何参数组合都产出合法矩形。
     */
    private static Bar bar(int x0, int y0, int x1, int y1) {
        return new Bar(Math.min(x0, x1), Math.min(y0, y1),
                Math.max(x0, x1), Math.max(y0, y1));
    }

    /**
     * 中心点。
     *
     * @param size 边长（像素）
     */
    public static Bar dot(int size) {
        int s = Math.max(1, size);
        int half = s / 2;
        return new Bar(-half, -half, -half + s - 1, -half + s - 1);
    }

    /**
     * 线段总共占的横跨范围（从 -outer 到 +outer，含线宽）。
     * 用于离线校验"准星不会大到离谱"。
     */
    public static int span(int gap, int arm) {
        // 闭区间：从 -outer 到 +outer（两端都在），共 2*outer+1 像素
        int outer = Math.max(0, gap) + Math.max(1, arm);
        return 2 * outer + 1;
    }
}
