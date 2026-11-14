package com.taczopt.assist;

import com.taczopt.config.ConfigFile;

/** Settings for the aim assist. */
public final class AssistConfig {

    private static final String P = "assist.";

    public static boolean enabled = true;
    public static double range = 48.0D;
    public static double halfAngle = 40.0D;
    public static double speedDegrees = 1.2D;
    public static boolean requireGun = true;
    public static boolean includePlayers = false;
    public static boolean requireLineOfSight = true;

    private static volatile boolean loaded = false;

    private AssistConfig() {
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
        range = ConfigFile.getDouble(P + "range", 48.0D);
        halfAngle = ConfigFile.getDouble(P + "half_angle", 40.0D);
        speedDegrees = ConfigFile.getDouble(P + "speed_degrees", 1.2D);
        requireGun = ConfigFile.getBool(P + "require_gun", true);
        includePlayers = ConfigFile.getBool(P + "include_players", false);
        requireLineOfSight = ConfigFile.getBool(P + "require_line_of_sight", true);

        if (range < 4.0D) {
            range = 4.0D;
        }
        if (range > 128.0D) {
            range = 128.0D;
        }
        if (halfAngle < 1.0D) {
            halfAngle = 1.0D;
        }
        if (halfAngle > 90.0D) {
            halfAngle = 90.0D;
        }
        if (speedDegrees < 0.05D) {
            speedDegrees = 0.05D;
        }
        if (speedDegrees > 15.0D) {
            speedDegrees = 15.0D;
        }
    }
}
