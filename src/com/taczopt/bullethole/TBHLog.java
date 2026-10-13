package com.taczopt.bullethole;

import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class TBHLog {

    private static final Logger LOGGER = LogManager.getLogger("taczopt");
    private static boolean applyLogged = false;
    private static boolean missingLogged = false;
    private static boolean holeLogged = false;
    private static int autoLogged = 0;

    private TBHLog() {
    }

    public static void modLoaded() {
        LOGGER.info("[taczopt] mod loaded: enabled={} pictures={} logo_guns={} hole_guns={} texture={} logo_scale={} hole_scale={} lift={} hold={} fade={} alpha={}",
                TBHConfig.enabled, TBHConfig.pictures(), TBHConfig.gunsRaw(), TBHConfig.holeGunsRaw(), TBHConfig.texture(),
                TBHConfig.scale, TBHConfig.holeScale, TBHConfig.logoLift, TBHConfig.logoHold, TBHConfig.logoFade, TBHConfig.alpha);
    }

    public static void autoCheck(String gunId, String fromIndex, String fromConvention, String fromItem, boolean special) {
        if (autoLogged >= 10) {
            return;
        }
        autoLogged++;
        LOGGER.info("[taczopt] auto-check {}: index={} convention={} item={} -> special={}",
                gunId, fromIndex, fromConvention, fromItem, special);
    }

    public static void applied(String gunId) {
        if (applyLogged) {
            return;
        }
        applyLogged = true;
        LOGGER.info("[taczopt] mixin active: gun {} name={} -> floating logo enabled", gunId, TBHSpecial.displayName(gunId));
    }

    public static void holeScaled(String gunId) {
        if (holeLogged) {
            return;
        }
        holeLogged = true;
        LOGGER.info("[taczopt] hole scale {} applied for gun {} (guns={})", TBHConfig.holeScale, gunId, TBHConfig.holeGunsRaw());
    }

    public static void textureMissing(ResourceLocation id) {
        if (missingLogged) {
            return;
        }
        missingLogged = true;
        LOGGER.warn("[taczopt] texture {} not found in block atlas, keeping the TaCZ bullet hole. "
                + "Put a .png into {} (folder is created on demand).", id, PicturePack.root());
    }
}
