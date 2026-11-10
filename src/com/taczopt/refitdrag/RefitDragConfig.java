package com.taczopt.refitdrag;

import com.taczopt.config.ConfigFile;

/** Settings for dragging the gun around in the refit screen. */
public final class RefitDragConfig {

    private static final String P = "refitdrag.";

    public static boolean enabled = true;
    public static double sensitivity = 0.55D;
    public static int pitchLimit = 85;
    public static boolean invertX = false;
    public static boolean invertY = false;
    public static boolean resetOnOpen = true;

    private static volatile boolean loaded = false;

    private RefitDragConfig() {
    }

    public static synchronized void ensureLoaded() {
        ConfigFile.ensureLoaded();
        if (loaded) {
            return;
        }
        load();
        loaded = true;
    }

    /**
     * Only called when the refit screen opens or a drag starts -- never per frame, because
     * this re-reads the properties file.
     */
    public static synchronized void reloadIfChanged() {
        ensureLoaded();
        ConfigFile.reloadIfChanged();
        load();
    }

    private static void load() {
        enabled = ConfigFile.getBool(P + "enabled", true);
        sensitivity = ConfigFile.getDouble(P + "sensitivity", 0.55D);
        pitchLimit = ConfigFile.getInt(P + "pitch_limit", 85);
        invertX = ConfigFile.getBool(P + "invert_x", false);
        invertY = ConfigFile.getBool(P + "invert_y", false);
        resetOnOpen = ConfigFile.getBool(P + "reset_on_open", true);

        if (sensitivity < 0.05D) {
            sensitivity = 0.05D;
        }
        if (sensitivity > 5.0D) {
            sensitivity = 5.0D;
        }
        if (pitchLimit < 10) {
            pitchLimit = 10;
        }
        if (pitchLimit > 90) {
            pitchLimit = 90;
        }
    }
}
