import com.taczopt.hitfeedback.HitFeedbackLogic;
import com.taczopt.hitfeedback.HitFeedbackLogic.Stroke;

/**
 * 离线校验命中标记的纯逻辑（不启动游戏）。
 *
 * <p>{@link HitFeedbackLogic} 特意不引用任何游戏类，就是为了能在这里直接跑。
 * 校验三件事：
 * <ol>
 *   <li><b>X 的形状</b>：四条臂、45 度、中间留空、臂朝四个象限；
 *       而且"横跨半径"必须真是 {@code outer}（早先按轴向长度算过，X 小了一圈）。</li>
 *   <li><b>标记形态唯一</b>：爆头和非爆头画出来的线段<b>完全一样</b>
 *       —— 部位只靠颜色区分，所以标记不会因命中部位而变形或跳动。</li>
 *   <li><b>颜色</b>：普通白、爆头橙、致死红，且致死压过爆头。</li>
 * </ol>
 *
 * <p>跑法（源码目录下）：
 * <pre>
 *   javac -encoding UTF-8 -cp classes -d check tools\HitFeedbackCheck.java
 *   java -cp "check;classes" HitFeedbackCheck
 * </pre>
 */
public final class HitFeedbackCheck {

    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=== 单 X 的形状 ===");
        Stroke[] one = HitFeedbackLogic.markerStrokes(7, 2);
        check("一共四条臂", one.length == 4);

        boolean all45 = true;
        for (Stroke s : one) {
            if (Math.abs(s.x1() - s.x0()) != Math.abs(s.y1() - s.y0())) {
                all45 = false;
            }
        }
        check("每条臂都是 45 度（|dx| == |dy|）", all45);

        // 四条臂分别落在四个象限
        boolean[] quad = new boolean[4];
        for (Stroke s : one) {
            int qx = s.x0() < 0 ? 0 : 1;
            int qy = s.y0() < 0 ? 0 : 1;
            quad[qy * 2 + qx] = true;
        }
        boolean allQuad = quad[0] && quad[1] && quad[2] && quad[3];
        check("四条臂分别指向四个象限", allQuad);

        // 关键回归点：横跨半径必须真是 outer，不能被 1/sqrt(2) 缩掉
        int maxReach = 0;
        for (Stroke s : one) {
            maxReach = Math.max(maxReach, Math.abs(s.x0()));
            maxReach = Math.max(maxReach, Math.abs(s.y0()));
        }
        eqInt("外侧横跨半径 = 7", 7, maxReach);

        int minReach = Integer.MAX_VALUE;
        for (Stroke s : one) {
            minReach = Math.min(minReach, Math.abs(s.x1()));
        }
        eqInt("内侧留空半径 = 2（不是实心叉）", 2, minReach);

        check("中间是空的（不是两条对角线）", minReach > 0);

        System.out.println();
        System.out.println("=== 标记形态唯一（爆头不再画双 X）===");
        // 爆头曾经会画 8 条线段（两个 X 上下叠），实测糊成一团已删除。
        // 现在唯一入口是 markerStrokes，这里守住"它永远只给 4 条"。
        eqInt("单 X 永远是 4 条线段（不会变成 8 条双 X）", 4,
                HitFeedbackLogic.markerStrokes(7, 2).length);
        eqInt("横跨半径改成 4 也只有 4 条（历史上爆头用过 4）", 4,
                HitFeedbackLogic.markerStrokes(4, 1).length);

        System.out.println();
        System.out.println("=== 参数防御 ===");
        eqInt("outer 传 0 也至少给 4 条臂", 4, HitFeedbackLogic.markerStrokes(0, 5).length);
        Stroke[] weird = HitFeedbackLogic.markerStrokes(0, 5);
        check("inner 比 outer 大时不会把臂压成反向",
                Math.abs(weird[0].x1()) <= Math.abs(weird[0].x0()));
        check("outer 传负数也不会画出反向的臂（负半径按绝对值算）",
                Math.abs(HitFeedbackLogic.markerStrokes(-5, 0)[0].x1())
                        <= Math.abs(HitFeedbackLogic.markerStrokes(-5, 0)[0].x0()));
        check("outer 传负数画出来和正数一模一样",
                sameStrokes(HitFeedbackLogic.markerStrokes(-5, 2),
                        HitFeedbackLogic.markerStrokes(5, 2)));

        System.out.println();
        System.out.println("=== 颜色（白 / 橙 / 红 三级）===");
        eqInt("非爆头且没死 -> 白色", 0xFFFFFF, HitFeedbackLogic.colorFor(false, false));
        eqInt("爆头但没死   -> 橙色", 0xFFA500, HitFeedbackLogic.colorFor(true, false));
        eqInt("致死         -> 红色", 0xFF5555, HitFeedbackLogic.colorFor(false, true));
        eqInt("爆头且致死   -> 红色（致死优先）", 0xFF5555, HitFeedbackLogic.colorFor(true, true));
        check("橙色和白色不同（否则爆头看不出来）",
                HitFeedbackLogic.COLOR_HEADSHOT != HitFeedbackLogic.COLOR_NORMAL);
        check("橙色和红色不同（否则爆头致死分不清）",
                HitFeedbackLogic.COLOR_HEADSHOT != HitFeedbackLogic.COLOR_KILL);
        check("三个颜色两两不同",
                HitFeedbackLogic.COLOR_NORMAL != HitFeedbackLogic.COLOR_HEADSHOT
                        && HitFeedbackLogic.COLOR_HEADSHOT != HitFeedbackLogic.COLOR_KILL
                        && HitFeedbackLogic.COLOR_NORMAL != HitFeedbackLogic.COLOR_KILL);

        System.out.println();
        System.out.println("=== 可见性阈值 ===");
        check("alpha 0.5 可见", HitFeedbackLogic.visible(0.5f));
        check("alpha 0.2 可见", HitFeedbackLogic.visible(0.2f));
        check("alpha 0.01 不可见（不浪费顶点，也不留黑线）", !HitFeedbackLogic.visible(0.01f));
        check("alpha 0 不可见", !HitFeedbackLogic.visible(0f));

        System.out.println();
        if (failures == 0) {
            System.out.println("HitFeedbackCheck: 全部通过");
        } else {
            System.out.println("HitFeedbackCheck: " + failures + " 项失败");
            System.exit(1);
        }
    }

    private static boolean sameStrokes(Stroke[] a, Stroke[] b) {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i].x0() != b[i].x0() || a[i].y0() != b[i].y0()
                    || a[i].x1() != b[i].x1() || a[i].y1() != b[i].y1()) {
                return false;
            }
        }
        return true;
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
