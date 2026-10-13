package com.taczopt.hitfeedback;

public final class HitFeedbackLogic {

    private HitFeedbackLogic() {
    }

    public static final int COLOR_NORMAL = 0xFFFFFF;

    public static final int COLOR_HEADSHOT = 0xFFA500;

    public static final int COLOR_KILL = 0xFF5555;

    public record Stroke(int x0, int y0, int x1, int y1) {
    }

    public static Stroke[] markerStrokes(int outer, int inner) {

        int o = Math.max(1, Math.abs(outer));
        int i = Math.max(0, Math.min(Math.abs(inner), o - 1));
        return new Stroke[] {
                new Stroke(-o, -o, -i, -i),
                new Stroke(o, -o, i, -i),
                new Stroke(-o, o, -i, i),
                new Stroke(o, o, i, i),
        };
    }

    public static int colorFor(boolean headshot, boolean killed) {
        if (killed) {
            return COLOR_KILL;
        }
        return headshot ? COLOR_HEADSHOT : COLOR_NORMAL;
    }

    public static boolean visible(float alpha) {
        return alpha > 0.05f;
    }
}
