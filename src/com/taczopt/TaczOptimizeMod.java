package com.taczopt;

import com.mojang.logging.LogUtils;
import com.taczopt.adsfire.AdsFireConfig;
import com.taczopt.bullethole.TBHConfig;
import com.taczopt.bullethole.TBHLog;
import com.taczopt.config.ConfigFile;
import com.taczopt.hybridaim.HybridAimConfig;
import com.taczopt.hitfeedback.HitFeedbackConfig;
import com.taczopt.hitfeedback.HitFeedbackNetwork;
import com.taczopt.hitfeedback.HitFeedbackServer;
import com.taczopt.oneshot.OneShotConfig;
import com.taczopt.oneshot.OneShotLog;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Path;

@Mod(TaczOptimizeMod.MOD_ID)
public class TaczOptimizeMod {

    public static final String MOD_ID = "taczopt";

    private static final Logger LOGGER = LogUtils.getLogger();

    public TaczOptimizeMod() {

        ConfigFile.ensureLoaded();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            TBHConfig.ensureLoaded();
            TBHLog.modLoaded();
            OneShotConfig.ensureLoaded();
            OneShotLog.modLoaded();
            HybridAimConfig.ensureLoaded();
            AdsFireConfig.ensureLoaded();
        }

        HitFeedbackConfig.enabled();
        HitFeedbackNetwork.register();
        MinecraftForge.EVENT_BUS.register(HitFeedbackServer.class);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            registerClientHitFeedback();
            registerClientCrosshair();
        }
        LOGGER.info("[taczopt] Optimization for Tacz 已加载：弹孔 / 秒杀 / 混合开镜 / 开火自动开镜 / 命中标记 / 腰射准星；配置 config/taczopt.properties");
    }

    private static void registerClientHitFeedback() {
        MinecraftForge.EVENT_BUS.register(
                com.taczopt.hitfeedback.HitFeedbackClient.class);
    }

    private static void registerClientCrosshair() {
        com.taczopt.crosshair.CrosshairConfig.enabled();
        MinecraftForge.EVENT_BUS.register(
                com.taczopt.crosshair.CrosshairStyleClient.class);
    }

    public static Logger logger() {
        return LOGGER;
    }

    public static Path gameDir() {
        return FMLPaths.GAMEDIR.get();
    }
}
