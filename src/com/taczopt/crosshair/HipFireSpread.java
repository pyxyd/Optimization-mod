package com.taczopt.crosshair;

public final class HipFireSpread {

    private HipFireSpread() {
    }

    public enum Stance {
        STAND, MOVE, SNEAK, LIE, AIM
    }

    public static Stance stanceFor(float aimingProgress, boolean swimming,
                                   boolean crouching, boolean moving) {
        if (aimingProgress == 1.0f) {
            return Stance.AIM;
        }
        if (swimming) {
            return Stance.LIE;
        }
        if (crouching) {
            return Stance.SNEAK;
        }
        if (moving) {
            return Stance.MOVE;
        }
        return Stance.STAND;
    }

    public static boolean isMove(double limbSwingDiff, double playerSpeed) {
        double d = Double.isNaN(playerSpeed) ? Math.abs(limbSwingDiff) : playerSpeed;
        return d > 0.05;
    }

    public static float lerp(float delta, float start, float end) {
        return start + delta * (end - start);
    }

    public static float heatMultiplier(boolean hasHeatData, float heatAmount, float maxHeat,
                                       float minInaccuracy, float maxInaccuracy) {
        if (!hasHeatData) {
            return 1.0f;
        }
        float ratio = maxHeat == 0f ? 0f : heatAmount / maxHeat;
        return lerp(ratio, minInaccuracy, maxInaccuracy);
    }

    public static float spread(float baseInaccuracy, float heatMultiplier) {
        return Math.max(0f, baseInaccuracy * heatMultiplier);
    }

    public static float defaultFor(Stance stance) {
        switch (stance) {
            case MOVE:
                return 5.75f;
            case SNEAK:
                return 3.5f;
            case LIE:
                return 2.5f;
            case AIM:
                return 0.1f;
            case STAND:
            default:
                return 5.0f;
        }
    }
}
