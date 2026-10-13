package com.taczopt.crosshair;

import com.taczopt.config.ConfigFile;

public final class CrosshairConfig {

    public static final String KEY_ENABLED = "crosshair.enabled";

    public static final String KEY_COLOR = "crosshair.color";

    public static final String KEY_THICKNESS = "crosshair.thickness";

    public static final String KEY_ARM = "crosshair.arm";

    public static final String KEY_GAP = "crosshair.gap";

    public static final String KEY_DYNAMIC = "crosshair.dynamic";

    public static final String KEY_GAP_MULTIPLIER = "crosshair.gap_multiplier";

    public static final String KEY_GAP_MAX = "crosshair.gap_max";

    public static final String KEY_DOT = "crosshair.dot";

    public static final String KEY_HIDE_ON_AIM = "crosshair.hide_on_aim";

    public static final String KEY_ONLY_GUNS = "crosshair.only_guns";

    public static final String KEY_SIZE_SCALE = "crosshair.spread_scale";

    public static final String KEY_DEBUG_LOG = "crosshair.debug_log";

    private CrosshairConfig() {
    }

    public static boolean enabled() {
        ConfigFile.ensureLoaded();
        return ConfigFile.getBool(KEY_ENABLED, true);
    }

    public static int color() {
        ConfigFile.ensureLoaded();
        String v = ConfigFile.get(KEY_COLOR, "FFFFFF").trim();
        if (v.startsWith("#")) {
            v = v.substring(1);
        }
        if (v.startsWith("0x") || v.startsWith("0X")) {
            v = v.substring(2);
        }
        try {
            return Integer.parseInt(v, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }

    public static int thickness() {
        ConfigFile.ensureLoaded();
        return clamp(ConfigFile.getInt(KEY_THICKNESS, 1), 1, 8);
    }

    public static int arm() {
        ConfigFile.ensureLoaded();
        return clamp(ConfigFile.getInt(KEY_ARM, 5), 1, 40);
    }

    public static int gap() {
        ConfigFile.ensureLoaded();
        return clamp(ConfigFile.getInt(KEY_GAP, 8), 1, 80);
    }

    public static boolean dynamic() {
        ConfigFile.ensureLoaded();
        return ConfigFile.getBool(KEY_DYNAMIC, true);
    }

    public static double gapMultiplier() {
        ConfigFile.ensureLoaded();
        double v = ConfigFile.getDouble(KEY_GAP_MULTIPLIER, 1.0);
        return Math.max(0.0, Math.min(v, 20.0));
    }

    public static int gapMax() {
        ConfigFile.ensureLoaded();
        return clamp(ConfigFile.getInt(KEY_GAP_MAX, 60), 4, 200);
    }

    public static int dot() {
        ConfigFile.ensureLoaded();
        return clamp(ConfigFile.getInt(KEY_DOT, 1), 0, 8);
    }

    public static boolean hideOnAim() {
        ConfigFile.ensureLoaded();
        return ConfigFile.getBool(KEY_HIDE_ON_AIM, true);
    }

    public static boolean onlyGuns() {
        ConfigFile.ensureLoaded();
        return ConfigFile.getBool(KEY_ONLY_GUNS, true);
    }

    public static float spreadScale() {
        ConfigFile.ensureLoaded();
        double v = ConfigFile.getDouble(KEY_SIZE_SCALE, 2.5);
        return (float) Math.max(0.2, Math.min(v, 5.0));
    }

    public static boolean debugLog() {
        ConfigFile.ensureLoaded();
        return ConfigFile.getBool(KEY_DEBUG_LOG, false);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(v, hi));
    }
}
