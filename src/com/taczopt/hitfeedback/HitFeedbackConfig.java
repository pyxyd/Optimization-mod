package com.taczopt.hitfeedback;

import com.taczopt.config.ConfigFile;

public final class HitFeedbackConfig {

    public static final String KEY_ENABLED = "hit.enabled";

    public static final String KEY_DURATION_MS = "hit.duration_ms";

    public static final String KEY_SCALE = "hit.scale";

    public static final String KEY_SOUND = "hit.sound";

    public static final String KEY_SHAKE = "hit.shake";

    public static final String KEY_OFFSET_Y = "hit.offset_y";

    public static final String KEY_OFFSET_X = "hit.offset_x";

    public static final String KEY_DEBUG_LOG = "hit.debug_log";

    private HitFeedbackConfig() {
    }

    public static boolean enabled() {
        ConfigFile.ensureLoaded();
        return ConfigFile.getBool(KEY_ENABLED, true);
    }

    public static int durationMs() {
        ConfigFile.ensureLoaded();
        int v = ConfigFile.getInt(KEY_DURATION_MS, 450);

        return Math.max(80, Math.min(v, 3000));
    }

    public static float scale() {
        ConfigFile.ensureLoaded();
        double v = ConfigFile.getDouble(KEY_SCALE, 1.0);
        return (float) Math.max(0.5, Math.min(v, 2.5));
    }

    public static boolean sound() {
        ConfigFile.ensureLoaded();
        return ConfigFile.getBool(KEY_SOUND, true);
    }

    public static boolean shake() {
        ConfigFile.ensureLoaded();
        return ConfigFile.getBool(KEY_SHAKE, true);
    }

    public static int offsetY() {
        ConfigFile.ensureLoaded();
        int v = ConfigFile.getInt(KEY_OFFSET_Y, 3);

        return Math.max(-400, Math.min(v, 400));
    }

    public static int offsetX() {
        ConfigFile.ensureLoaded();
        int v = ConfigFile.getInt(KEY_OFFSET_X, 0);
        return Math.max(-400, Math.min(v, 400));
    }

    public static boolean debugLog() {
        ConfigFile.ensureLoaded();
        return ConfigFile.getBool(KEY_DEBUG_LOG, false);
    }
}
