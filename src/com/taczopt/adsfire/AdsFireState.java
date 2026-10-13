package com.taczopt.adsfire;

public final class AdsFireState {

    private AdsFireState() {
    }

    private static String sessionGunId = null;

    private static boolean active = false;

    private static boolean aimRequested = false;

    private static boolean fired = false;

    private static int startTick = 0;

    private static boolean autoAimed = false;

    private static int lastFireTick = -10000;

    private static int lastShootDownTick = -10000;

    public static boolean isActive() {
        return active;
    }

    public static int lastShootDownTick() {
        return lastShootDownTick;
    }

    public static void markShootDown(int tick) {
        lastShootDownTick = tick;
    }

    public static boolean isAutoAimed() {
        return autoAimed;
    }

    public static boolean hasFired() {
        return fired;
    }

    public static String sessionGunId() {
        return sessionGunId;
    }

    public static int lastFireTick() {
        return lastFireTick;
    }

    public static void begin(String gunId, int tick) {
        sessionGunId = gunId;
        active = true;
        aimRequested = false;
        fired = false;
        autoAimed = false;
        startTick = tick;
    }

    public static void markAimRequested() {
        aimRequested = true;
        autoAimed = true;
    }

    public static boolean isAimRequested() {
        return aimRequested;
    }

    public static void markFired(int tick) {
        fired = true;
        lastFireTick = tick;
    }

    public static void end() {
        active = false;
        aimRequested = false;
        fired = false;
        autoAimed = false;
        sessionGunId = null;
    }

    public static void clearAutoAimed() {
        autoAimed = false;
    }

    public static int startTick() {
        return startTick;
    }

    public static void reset() {
        end();
        lastFireTick = -10000;
        lastShootDownTick = -10000;
    }
}
