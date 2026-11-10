package com.taczopt.lean;

/**
 * Smoothed lean amount.
 *
 * current runs -1 (peek left) .. +1 (peek right); the target comes from the arrow keys and
 * the value eases toward it every frame so the camera does not snap.
 */
public final class LeanState {

    private static float current = 0F;
    private static int target = 0;

    private LeanState() {
    }

    public static void setTarget(int value) {
        target = value < -1 ? -1 : (value > 1 ? 1 : value);
    }

    public static int target() {
        return target;
    }

    public static float current() {
        return current;
    }

    /** Eases toward the target; called once per client tick with the frame delta in ticks. */
    public static void update(float deltaTicks) {
        float goal = LeanConfig.enabled ? target : 0F;
        if (LeanConfig.invert) {
            goal = -goal;
        }
        float step = (float) (LeanConfig.speed * Math.max(0.05F, deltaTicks));
        if (current < goal) {
            current = Math.min(goal, current + step);
        } else if (current > goal) {
            current = Math.max(goal, current - step);
        }
    }

    public static void reset() {
        current = 0F;
        target = 0;
    }

    public static String describe() {
        return "lean=" + Math.round(current * 100F) / 100F + " target=" + target;
    }
}
