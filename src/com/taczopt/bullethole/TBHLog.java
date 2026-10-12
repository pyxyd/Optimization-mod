package com.taczopt.bullethole;

import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 日志（内容保持纯 ASCII，方便在 latest.log 里搜索 "taczbullethole"）。
 * 只打关键节点，避免每颗弹孔刷屏。
 */
public final class TBHLog {

    private static final Logger LOGGER = LogManager.getLogger("taczopt");
    private static boolean applyLogged = false;
    private static boolean missingLogged = false;
    private static boolean holeLogged = false;
    private static int autoLogged = 0;

    private TBHLog() {
    }

    public static void modLoaded() {
        LOGGER.info("[taczopt] mod loaded: enabled={} logo_guns={} hole_guns={} texture={} logo_scale={} hole_scale={} lift={} hold={} fade={} alpha={}",
                TBHConfig.enabled, TBHConfig.gunsRaw(), TBHConfig.holeGunsRaw(), TBHConfig.texture(), TBHConfig.scale,
                TBHConfig.holeScale, TBHConfig.logoLift, TBHConfig.logoHold, TBHConfig.logoFade, TBHConfig.alpha);
    }

    /** 自动识别的判定过程；只打前若干条，方便确认规则对不对 */
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
        LOGGER.warn("[taczopt] texture {} not found in block atlas, falling back to vanilla TaCZ bullet hole. "
                + "Check assets/taczbullethole/textures/block/bullet_hole.png in the mod jar or in your resource pack.", id);
    }
}
