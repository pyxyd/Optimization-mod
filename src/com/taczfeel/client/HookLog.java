package com.taczfeel.client;

import com.taczfeel.FeelLog;

/** 混入是否生效：只在第一次命中时各打一条日志，方便排查"为什么没效果"。 */
public final class HookLog {

    private static boolean sound;
    private static boolean muzzle;
    private static boolean tracer;

    private HookLog() {
    }

    public static void soundOnce(String detail) {
        if (!sound) {
            sound = true;
            FeelLog.info("sound mixin active: " + detail);
        }
    }

    public static void muzzleOnce(String detail) {
        if (!muzzle) {
            muzzle = true;
            FeelLog.info("muzzle mixin active: " + detail);
        }
    }

    public static void tracerOnce(String detail) {
        if (!tracer) {
            tracer = true;
            FeelLog.info("tracer mixin active: " + detail);
        }
    }
}
