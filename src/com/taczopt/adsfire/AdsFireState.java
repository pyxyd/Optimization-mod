package com.taczopt.adsfire;

/**
 * 「开火先开镜」的一次会话状态。
 *
 * <p>流程：
 * <pre>
 * 玩家按左键（还没开镜）
 *   -> begin()      记录本次会话，本 tick 不让 TaCZ 开枪
 *   -> operator.aim(true)   请求开镜
 *   -> 之后每个 tick 检查开镜是否到位，到位（或超时）后打出这一发 -> markFired()
 *   -> 松开左键后再保持 hold_ticks，然后收镜 -> end()
 * </pre>
 *
 * <p>所有字段都只在客户端主线程访问，不需要同步。
 */
public final class AdsFireState {

    private AdsFireState() {
    }

    /** 本次会话对应的枪（切枪就作废） */
    private static String sessionGunId = null;
    /** 会话进行中 */
    private static boolean active = false;
    /** 已经请求开镜 */
    private static boolean aimRequested = false;
    /** 这一发已经打出去了 */
    private static boolean fired = false;
    /** 会话开始的 tick */
    private static int startTick = 0;
    /** 这一轮开镜是我们自己开的（用于在收镜时判断要不要抢玩家的开镜键） */
    private static boolean autoAimed = false;
    /** 最后一次真正打出子弹的 tick */
    private static int lastFireTick = -10000;
    /** 最后一次看到"玩家按着左键"的 tick（松手后计时用） */
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

    /** 开始一次「先开镜再开火」会话 */
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

    /** 收镜 / 结束会话时调用 */
    public static void end() {
        active = false;
        aimRequested = false;
        fired = false;
        autoAimed = false;
        sessionGunId = null;
    }

    /**
     * 收镜前必须先清掉 autoAimed，否则 LocalPlayerAimMixin 会把我们自己发的收镜请求也拦掉。
     */
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
