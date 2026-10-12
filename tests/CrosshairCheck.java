import com.taczopt.crosshair.CrosshairLogic;
import com.taczopt.crosshair.HipFireSpread;
import com.taczopt.crosshair.CrosshairLogic.Bar;
import com.taczopt.crosshair.HipFireSpread.Stance;

/**
 * 离线校验腰射准星的纯逻辑（不启动游戏）。
 *
 * <p>{@link CrosshairLogic} 特意不引用任何游戏类，就是为了能在这里直接跑。
 * 校验三件事：
 * <ol>
 *   <li><b>空心十字的形状</b>：四条线、中间留空、关于中心对称、长度和线宽正确。
 *       最难发现的是"中间没留空"（画出来变成实心十字）和"四条线不对称"。</li>
 *   <li><b>扩散换算</b>：间距随扩散增大但被上限夹住，且永远大于基础值。</li>
 *   <li><b>不会跑到屏幕外</b>：横跨范围有个合理上限。</li>
 * </ol>
 *
 * <p>跑法（源码目录下）：
 * <pre>
 *   javac -encoding UTF-8 -cp classes -d check tools\CrosshairCheck.java
 *   java -cp "check;classes" CrosshairCheck
 * </pre>
 */
public final class CrosshairCheck {

    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=== 空心十字的形状 ===");
        Bar[] c = CrosshairLogic.cross(8, 5, 2);
        eqInt("一共四条线", 4, c.length);

        // 上 / 下 / 左 / 右 的顺序
        Bar up = c[0];
        Bar down = c[1];
        Bar left = c[2];
        Bar right = c[3];

        check("上：在中心上方（y 全为负）", up.y0() < 0 && up.y1() < 0);
        check("下：在中心下方（y 全为正）", down.y0() > 0 && down.y1() > 0);
        check("左：在中心左侧（x 全为负）", left.x0() < 0 && left.x1() < 0);
        check("右：在中心右侧（x 全为正）", right.x0() > 0 && right.x1() > 0);

        // 关键：中间必须留空
        // 对称性：上下/左右必须逐格镜像。
        // 原版 MC 准星在这里是不对称的（横向循环 -1..4、纵向 -1..5），
        // 第一版照抄了那个结构，结果"右边和下边各少 1 像素"被用户看出来。
        eqInt("上的内侧 = -(gap+1)", -9, up.y1());
        eqInt("下的内侧 = +(gap+1)", 9, down.y0());
        eqInt("左的内侧 = -(gap+1)", -9, left.x1());
        eqInt("右的内侧 = +(gap+1)", 9, right.x0());
        check("上下到中心的空隙一样（有符号距离和为 0）",
                up.y1() + down.y0() == 0);
        check("左右到中心的空隙一样（有符号距离和为 0）",
                left.x1() + right.x0() == 0);
        check("上下外端对称", up.y0() + down.y1() == 0);
        check("左右外端对称", left.x0() + right.x1() == 0);
        check("中间是空的（不是实心十字）", up.y1() < 0 && down.y0() > 0);

        // 长度：gap + arm
        eqInt("上的外侧 = -(gap+arm)", -13, up.y0());
        eqInt("下的外侧 = +(gap+arm)", 13, down.y1());
        eqInt("上的长度 = arm(5) 格", 5, up.y1() - up.y0() + 1);

        // 线宽
        eqInt("上的线宽 = 2", 2, up.x1() - up.x0() + 1);
        eqInt("左的线宽 = 2", 2, left.y1() - left.y0() + 1);

        // 对称
        check("上下对称", up.x0() == down.x0() && up.x1() == down.x1());
        check("左右对称", left.y0() == right.y0() && left.y1() == right.y1());
        // 下面这些是"留空一样多、长度一样"这类不变量。
        // 逐格镜像的断言在上面（这是用户实际看出来的那个问题）。
        eqInt("上下的留空一样多", -up.y1() - 1, down.y0() - 1);
        eqInt("上下的长度一样", up.y1() - up.y0(), down.y1() - down.y0());
        eqInt("左右的留空一样多", -left.x1() - 1, right.x0() - 1);
        eqInt("左右的长度一样（比水平跨度）", left.x1() - left.x0(), right.x1() - right.x0());
        check("上段全在中心上方", up.y1() < 0);
        check("下段全在中心下方", down.y0() > 0);
        check("左段全在中心左侧", left.x1() < 0);
        check("右段全在中心右侧", right.x0() > 0);

        System.out.println();
        System.out.println("=== 中心点 ===");
        Bar d1 = CrosshairLogic.dot(1);
        eqInt("1 像素中心点占 1x1", 1, d1.x1() - d1.x0() + 1);
        check("1 像素中心点盖住原点", d1.x0() <= 0 && d1.x1() >= 0 && d1.y0() <= 0 && d1.y1() >= 0);
        Bar d2 = CrosshairLogic.dot(2);
        eqInt("2 像素中心点占 2x2", 2, d2.x1() - d2.x0() + 1);
        check("2 像素中心点也盖住原点（不能偏一边）", d2.x0() <= 0 && d2.x1() >= 0);

        System.out.println();
        System.out.println("=== 线宽 / 长度 的参数防御 ===");
        check("线宽 0 会被夹到 1", barWidth(CrosshairLogic.cross(8, 5, 0)[0]) == 1);
        check("长度 0 会被夹到 1 格（上下端点重合，差 0）", CrosshairLogic.cross(4, 0, 2)[0].y1() - CrosshairLogic.cross(4, 0, 2)[0].y0() == 0);
        check("gap 传负数按 0 处理", CrosshairLogic.cross(-5, 5, 2)[0].y1() == -1);

        System.out.println();
        System.out.println("=== 扩散换算：间距 = 扩散值(= 10 - 腰射精度) * 倍率 ===");
        // gapFor(inaccuracy, multiplier, floor, max)
        eqInt("精准手枪 stand=1.25 -> gap 1", 1, CrosshairLogic.gapFor(1.25f, 1.0, 1, 60));
        eqInt("m1911 stand=1.5 -> gap 2（四舍五入）", 2, CrosshairLogic.gapFor(1.5f, 1.0, 1, 60));
        eqInt("glock stand=2 -> gap 2", 2, CrosshairLogic.gapFor(2f, 1.0, 1, 60));
        eqInt("akm stand=4.75 -> gap 5", 5, CrosshairLogic.gapFor(4.75f, 1.0, 1, 60));
        eqInt("m7_prismatic stand=8 -> gap 8（最散）", 8, CrosshairLogic.gapFor(8f, 1.0, 1, 60));
        eqInt("倍数 2 -> 8*2 = 16", 16, CrosshairLogic.gapFor(8f, 2.0, 1, 60));
        eqInt("被上限夹住", 60, CrosshairLogic.gapFor(1000f, 1.0, 1, 60));
        eqInt("扩散 0 也不会贴成 0（否则四条线连成一坨）", 1, CrosshairLogic.gapFor(0f, 1.0, 1, 60));
        eqInt("负扩散按下限", 1, CrosshairLogic.gapFor(-5f, 1.0, 1, 60));
        eqInt("倍率传 0 按 1.0 处理（防手滑）", 4, CrosshairLogic.gapFor(4f, 0.0, 1, 60));
        eqInt("倍率传负数也按 1.0", 4, CrosshairLogic.gapFor(4f, -3.0, 1, 60));
        check("下限大于上限时不会反过来变小",
                CrosshairLogic.gapFor(1000f, 1.0, 80, 20) == 80);
        check("扩散越大间距越大（单调）",
                CrosshairLogic.gapFor(1.5f, 1.0, 1, 60) <= CrosshairLogic.gapFor(8f, 1.0, 1, 60));
        // 关键回归点：公式必须是"10 - 精度"，也就是直接用扩散值。
        // 早先版本写成 base + inaccuracy*0.8，精准枪和散射枪的准星几乎一样大，
        // 玩家反馈"腰射不准"就是这个。
        check("精准枪(1.5) 与 散射枪(8.0) 的准星差距明显（>4 像素）",
                CrosshairLogic.gapFor(8f, 1.0, 1, 60) - CrosshairLogic.gapFor(1.5f, 1.0, 1, 60) > 4);

        System.out.println();
        System.out.println("=== 准星尺寸在合理范围 ===");
        int span = CrosshairLogic.span(8, 5);
        eqInt("基础横跨 = 2*(8+5)+1 = 27", 27, span);
        check("基础横跨不超过 40（不会太大）", span <= 40);
        check("满扩散也在屏幕内（<200）", CrosshairLogic.span(60, 5) < 200);

        System.out.println();
        System.out.println("=== 照搬 TaCZ 的档位判定（顺序错就全错）===");
        // InaccuracyType.getInaccuracyType 的顺序：AIM > LIE > SNEAK > MOVE > STAND
        eqStance("开镜优先于一切（开镜+蹲+移动 仍是 AIM）",
                Stance.AIM, HipFireSpread.stanceFor(1.0f, true, true, true));
        check("瞄准进度 0.99 不算开镜",
                HipFireSpread.stanceFor(0.99f, false, false, false) == Stance.STAND);
        eqStance("游泳 -> LIE（即使蹲着）",
                Stance.LIE, HipFireSpread.stanceFor(0f, true, true, true));
        eqStance("蹲 -> SNEAK（即使移动）",
                Stance.SNEAK, HipFireSpread.stanceFor(0f, false, true, true));
        eqStance("移动 -> MOVE", Stance.MOVE, HipFireSpread.stanceFor(0f, false, false, true));
        eqStance("都不满足 -> STAND", Stance.STAND, HipFireSpread.stanceFor(0f, false, false, false));

        System.out.println();
        System.out.println("=== isMove 的阈值 0.05 ===");
        check("0.06 > 0.05 -> 算移动", HipFireSpread.isMove(0.06, Double.NaN));
        check("0.05 不 > 0.05 -> 不算移动", !HipFireSpread.isMove(0.05, Double.NaN));
        check("0.04 不算移动", !HipFireSpread.isMove(0.04, Double.NaN));
        check("玩家速度优先于腿部摆动（速度够大就算移动）",
                HipFireSpread.isMove(0.0, 0.2));
        check("玩家速度优先（速度很小就不算，哪怕摆动大）",
                !HipFireSpread.isMove(0.9, 0.01));
        check("负摆动取绝对值后照样判定（0.9 的绝对值 > 0.05）",
                HipFireSpread.isMove(-0.9, Double.NaN));

        System.out.println();
        System.out.println("=== 过热系数与最终扩散 ===");
        eqF("没过热数据 -> 系数 1.0", 1.0f, HipFireSpread.heatMultiplier(false, 0f, 100f, 0.5f, 3f));
        eqF("热量为 0 -> 取下限", 0.5f, HipFireSpread.heatMultiplier(true, 0f, 100f, 0.5f, 3f));
        eqF("热量满 -> 取上限", 3.0f, HipFireSpread.heatMultiplier(true, 100f, 100f, 0.5f, 3f));
        eqF("热量一半 -> 中间值", 1.75f, HipFireSpread.heatMultiplier(true, 50f, 100f, 0.5f, 3f));
        eqF("maxHeat 为 0 不炸（按 0 处理）", 0.5f, HipFireSpread.heatMultiplier(true, 0f, 0f, 0.5f, 3f));
        eqF("扩散 = 基础 * 系数", 10.0f, HipFireSpread.spread(5f, 2f));
        eqF("扩散不小于 0", 0.0f, HipFireSpread.spread(-3f, 1f));
        eqF("倍率 1.0 时就是基础值", 4.5f, HipFireSpread.spread(4.5f, 1f));

        System.out.println();
        System.out.println("=== 兜底常量要和 TaCZ 的 getDefaultInaccuracy 一致 ===");
        eqF("STAND 默认 5.0", 5.0f, HipFireSpread.defaultFor(Stance.STAND));
        eqF("MOVE 默认 5.75", 5.75f, HipFireSpread.defaultFor(Stance.MOVE));
        eqF("SNEAK 默认 3.5", 3.5f, HipFireSpread.defaultFor(Stance.SNEAK));
        eqF("LIE 默认 2.5", 2.5f, HipFireSpread.defaultFor(Stance.LIE));

        System.out.println();
        if (failures == 0) {
            System.out.println("CrosshairCheck: 全部通过");
        } else {
            System.out.println("CrosshairCheck: " + failures + " 项失败");
            System.exit(1);
        }
    }

    private static int barWidth(Bar b) {
        return b.x1() - b.x0() + 1;
    }

    private static void eqStance(String what, Stance expect, Stance actual) {
        boolean ok = expect == actual;
        System.out.println((ok ? "  [OK]   " : "  [FAIL] ") + what
                + (ok ? "" : "  期望 " + expect + " 实际 " + actual));
        if (!ok) {
            failures++;
        }
    }

    private static void eqF(String what, float expect, float actual) {
        boolean ok = Math.abs(expect - actual) < 1e-4f;
        System.out.println((ok ? "  [OK]   " : "  [FAIL] ") + what
                + (ok ? "" : "  期望 " + expect + " 实际 " + actual));
        if (!ok) {
            failures++;
        }
    }

    private static void eqInt(String what, int expect, int actual) {
        boolean ok = expect == actual;
        System.out.println((ok ? "  [OK]   " : "  [FAIL] ") + what
                + (ok ? "" : "  期望 " + expect + " 实际 " + actual));
        if (!ok) {
            failures++;
        }
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [OK]   " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
