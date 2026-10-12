package com.taczopt.hybridaim;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** 日志保持纯 ASCII，方便搜 "taczhybridaim"。 */
public final class HybridAimLog {

    private static final Logger LOGGER = LogManager.getLogger("taczopt");
    private static boolean suppressedLogged = false;
    private static boolean fallbackLogged = false;

    private HybridAimLog() {
    }

    public static void modLoaded() {
        LOGGER.info("[taczopt] mod loaded: enabled={} short_press_ms={} guns={}",
                HybridAimConfig.enabled, HybridAimConfig.shortPressMs, HybridAimConfig.gunsRaw());
    }

    /** 拦截生效的正面证据 */
    public static void suppressed() {
        if (suppressedLogged) {
            return;
        }
        suppressedLogged = true;
        LOGGER.info("[taczopt] TaCZ aim key handler suppressed, hybrid mode owns the aim key");
    }

    /** 拦截没生效 -> 自动降级成 TaCZ 原本的模式，避免两边打架 */
    public static void fallback() {
        if (fallbackLogged) {
            return;
        }
        fallbackLogged = true;
        LOGGER.warn("[taczopt] TaCZ aim key handler is still active (mixin not applied?), "
                + "hybrid mode disabled this session. Send latest.log if you see this.");
    }
}