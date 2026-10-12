package com.taczopt.hitfeedback;

/**
 * 命中标记的纯逻辑：样式判定 + X 的线段几何。
 *
 * <p>不引用任何游戏类，方便离线单测（见 {@code tools\HitFeedbackCheck.java}）。
 *
 * <h2>标记规则</h2>
 * <ul>
 *   <li>打到<b>其他部位</b> -> 白色单 X</li>
 *   <li>打到<b>头部</b>     -> <b>橙色</b>单 X</li>
 *   <li>这一枪<b>致死</b>   -> 红色单 X（优先级最高）</li>
 * </ul>
 * 形状永远是同一个单 X，只用颜色区分部位和击杀 —— 尺寸、位置都不变，
 * 所以连续命中时标记不会跳动。
 *
 * <p>不再显示伤害数字 —— 那是 GD656击杀图标 模组在做的事，两边叠在一起看不清。
 *
 * <h2>⚠ 曾经的"爆头双 X"为什么删了</h2>
 * 早先按需求做成"两个 X 上下叠"，还专门把爆头的 X 缩小到 {@code outer=4}。
 * 但实测（截图像素反解）两个 X 的中心距只有 14 像素、各自宽 10 像素，
 * 中间<strong>只隔 5~6 像素</strong>：正常坐姿看屏幕会连成一团
 * "菱形花纹"，读不出是两个 X —— 等于白做。改成橙色单 X 后，
 * 一眼就能看出是爆头，还不占额外空间。
 */
public final class HitFeedbackLogic {

    private HitFeedbackLogic() {
    }

    /** 白色：打到非头部 */
    public static final int COLOR_NORMAL = 0xFFFFFF;
    /**
     * 橙色：打到头部。
     *
     * <p>{@code 0xFFA500} 是标准橙。选它是因为：
     * <ul>
     *   <li>和白色拉开足够色差，扫一眼就能分辨；</li>
     *   <li>不是红色，所以不会和"致死红"混；</li>
     *   <li>在游戏常见的砖红/土黄背景上仍然清晰。</li>
     * </ul>
     */
    public static final int COLOR_HEADSHOT = 0xFFA500;
    /** 红色：这一枪打死了目标 */
    public static final int COLOR_KILL = 0xFF5555;

    /**
     * 一条线段（像素，相对标记中心）。
     *
     * <p>用 record 而不是直接填像素：{@link com.taczopt.hitfeedback.HitFeedbackClient}
     * 只负责把它交给 {@code GuiGraphics} 画出来，几何计算留在这里才能离线验证。
     */
    public record Stroke(int x0, int y0, int x1, int y1) {
    }

    /**
     * 生成一个 X 的四条线段。
     *
     * <p>形状就是经典命中标记：四条从中心往外斜出去的短线，中间留空，
     * 所以看起来是四条臂而不是两条对角线。
     *
     * <p>⚠ 参数是"横向半径"不是"轴向长度"。线段的 dy 正好等于 dx（45 度），
     * 所以如果按轴向长度算，横跨会变成它的 1/√2 倍 —— 之前就是那么写，
     * 结果 X 比预期小一圈、细得几乎看不见。这里直接给横跨半径，几何就直白了。
     *
     * @param outer 外侧横向半径（决定 X 有多大）
     * @param inner 内侧横向半径（中间留空的一半宽度，0 = 不留空）
     */
    public static Stroke[] markerStrokes(int outer, int inner) {
        // 半径是"长度"，负数只能当正数用（取绝对值），不能直接 max(1, x)。
        // 踩过：max(1, -5) 得到 1，而 inner 还是 0，于是线段变成 (-1,-1) -> (0,0)，
        // 终点比起点更靠近中心 —— 对负半径来说"中心"在右边，四条臂全部反向
        // （画出来是个反着的 X）。取绝对值后负号只是被忽略，形状不变。
        int o = Math.max(1, Math.abs(outer));
        int i = Math.max(0, Math.min(Math.abs(inner), o - 1));
        return new Stroke[] {
                new Stroke(-o, -o, -i, -i),
                new Stroke(o, -o, i, -i),
                new Stroke(-o, o, -i, i),
                new Stroke(o, o, i, i),
        };
    }

    /**
     * 标记颜色。
     *
     * <p>优先级：<b>致死 > 爆头 > 普通</b>。致死必须压过爆头 ——
     * 一枪爆头打死人时，玩家最需要知道的是"打死了"，红色比橙色信息量大。
     *
     * @param headshot 是否命中头部
     * @param killed   这一枪是否致死
     */
    public static int colorFor(boolean headshot, boolean killed) {
        if (killed) {
            return COLOR_KILL;
        }
        return headshot ? COLOR_HEADSHOT : COLOR_NORMAL;
    }

    /**
     * 小于这个 alpha 的线段直接不画。
     *
     * <p>淡化到几乎看不见时还在画，只是白白浪费顶点；给个阈值顺手也避免
     * alpha=0 时留一条黑线。
     */
    public static boolean visible(float alpha) {
        return alpha > 0.05f;
    }
}
