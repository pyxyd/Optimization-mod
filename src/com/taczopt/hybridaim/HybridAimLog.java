package com.taczopt.hybridaim;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    public static void suppressed() {
        if (suppressedLogged) {
            return;
        }
        suppressedLogged = true;
        LOGGER.info("[taczopt] TaCZ aim key handler suppressed, hybrid mode owns the aim key");
    }

    public static void fallback() {
        if (fallbackLogged) {
            return;
        }
        fallbackLogged = true;
        LOGGER.warn("[taczopt] TaCZ aim key handler is still active (mixin not applied?), "
                + "hybrid mode disabled this session. Send latest.log if you see this.");
    }
}
