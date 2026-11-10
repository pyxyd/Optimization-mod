package com.taczopt.lean;

import com.taczopt.config.ConfigFile;

/** Settings for the left/right peek. */
public final class LeanConfig {

    private static final String P = "lean.";

    public static boolean enabled = true;
    public static double yawDegrees = 30.0D;
    public static double distance = 0.0D;
    public static double rollDegrees = 0.0D;
    public static double speed = 0.28D;
    public static boolean invert = false;

    private static volatile boolean loaded = false;

    private LeanConfig() {
    }

    public static synchronized void ensureLoaded() {
        ConfigFile.ensureLoaded();
        if (loaded) {
            return;
        }
        load();
        loaded = true;
    }

    public static synchronized void reloadIfChanged() {
        ensureLoaded();
        ConfigFile.reloadIfChanged();
        load();
    }

    private static void load() {
        enabled = ConfigFile.getBool(P + "enabled", true);
        yawDegrees = ConfigFile.getDouble(P + "yaw_degrees", 30.0D);
        distance = ConfigFile.getDouble(P + "distance", 0.0D);
        rollDegrees = ConfigFile.getDouble(P + "roll_degrees", 0.0D);
        speed = ConfigFile.getDouble(P + "speed", 0.28D);
        invert = ConfigFile.getBool(P + "invert", false);

        if (yawDegrees < 0.0D) {
            yawDegrees = 0.0D;
        }
        if (yawDegrees > 85.0D) {
            yawDegrees = 85.0D;
        }
        if (distance < 0.0D) {
            distance = 0.0D;
        }
        if (distance > 1.5D) {
            distance = 1.5D;
        }
        if (rollDegrees < 0.0D) {
            rollDegrees = 0.0D;
        }
        if (rollDegrees > 30.0D) {
            rollDegrees = 30.0D;
        }
        if (speed < 0.02D) {
            speed = 0.02D;
        }
        if (speed > 1.0D) {
            speed = 1.0D;
        }
    }
}
