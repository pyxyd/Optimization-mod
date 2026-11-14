package com.taczopt.assist;

import com.taczopt.config.ConfigFile;

/**
 * On/off switch for the aim assist, flipped by the Y key.
 *
 * The switch is persisted as assist.on in taczopt.properties and read back once per launch, so
 * toggling it on survives a restart instead of silently coming back OFF every time.
 */
public final class AssistState {

    public static final String KEY = "assist.on";

    private static volatile boolean loaded = false;
    private static volatile boolean on = false;
    private static volatile String lastTarget = "";

    private AssistState() {
    }

    public static void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (AssistState.class) {
            if (loaded) {
                return;
            }
            on = ConfigFile.getBool(KEY, false);
            loaded = true;
        }
    }

    public static boolean isOn() {
        ensureLoaded();
        return on;
    }

    public static void toggle() {
        ensureLoaded();
        on = !on;
        lastTarget = "";
        ConfigFile.put(KEY, Boolean.toString(on));
    }

    public static String target() {
        return lastTarget;
    }

    public static void setLastTarget(String target) {
        lastTarget = target == null ? "" : target;
    }

    public static String describe() {
        return "on=" + on + " target=" + (lastTarget.isEmpty() ? "none" : lastTarget);
    }
}
