package com.taczopt.crosshair;

public final class CrosshairLogic {

    private CrosshairLogic() {
    }

    public record Bar(int x0, int y0, int x1, int y1) {
    }

    public static int gapFor(float inaccuracy, double multiplier, int floor, int max) {
        double v = Math.max(0f, inaccuracy) * (multiplier <= 0 ? 1.0 : multiplier);
        int gap = (int) Math.round(v);
        int lo = Math.max(1, floor);
        int hi = Math.max(lo, max);
        return Math.max(lo, Math.min(gap, hi));
    }

    public static Bar[] cross(int gap, int arm, int thickness) {
        int g = Math.max(0, gap);
        int a = Math.max(1, arm);

        int inner = g + 1;
        int outer = g + a;
        int t = Math.max(1, thickness);

        int neg = t / 2;
        int pos = t - t / 2 - 1;
        return new Bar[] {

                bar(-neg, -outer, pos, -inner),

                bar(-neg, inner, pos, outer),

                bar(-outer, -neg, -inner, pos),

                bar(inner, -neg, outer, pos),
        };
    }

    private static Bar bar(int x0, int y0, int x1, int y1) {
        return new Bar(Math.min(x0, x1), Math.min(y0, y1),
                Math.max(x0, x1), Math.max(y0, y1));
    }

    public static Bar dot(int size) {
        int s = Math.max(1, size);
        int half = s / 2;
        return new Bar(-half, -half, -half + s - 1, -half + s - 1);
    }

    public static int span(int gap, int arm) {

        int outer = Math.max(0, gap) + Math.max(1, arm);
        return 2 * outer + 1;
    }
}
