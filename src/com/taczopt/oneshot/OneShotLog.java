package com.taczopt.oneshot;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class OneShotLog {

    private static final Logger LOGGER = LogManager.getLogger("taczopt");
    private static boolean hitLogged = false;

    private OneShotLog() {
    }

    public static void modLoaded() {
        LOGGER.info("[taczopt] mod loaded: enabled={} guns={} affect_players={} damage={} force_kill={}",
                OneShotConfig.enabled, OneShotConfig.gunsRaw(), OneShotConfig.affectPlayers,
                OneShotConfig.damage, OneShotConfig.forceKill);
    }

    public static void lethalHit(String gunId) {
        if (hitLogged) {
            return;
        }
        hitLogged = true;
        LOGGER.info("[taczopt] lethal hit applied, gun={}", gunId);
    }
}
