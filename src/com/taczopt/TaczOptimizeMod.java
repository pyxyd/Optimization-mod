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

/**
 * Optimization for Tacz —— 把之前分开做的四个模组合并成一个，外加命中反馈。
 *
 * <p>显示名是 {@code Optimization for Tacz}（老版本叫 [TaCZ]优化）；
 * jar 文件名是 {@code [TaCZ：优化]Optimization-1.20.1-2.0.0-forge.jar}
 * （前缀里的冒号是**全角**「：」U+FF1A，是文件名的一部分，不要改成半角）。
 * mod id 始终是 {@code taczopt}，从没变过。
 *
 * <ol>
 *   <li><b>自定义弹孔</b>：方块上的弹孔用你的图片（悬浮一层），并可缩放原版弹孔。</li>
 *   <li><b>指定枪械秒杀</b>：指定枪命中生物直接致命（默认 AWM）。</li>
 *   <li><b>混合开镜</b>：开镜键短按=切换开关、长按=按住开镜松手收镜；鼠标键盘都支持。</li>
 *   <li><b>开火自动开镜</b>：按住左键先自动开镜，镜走完再开火；游戏内按 O 逐把枪开关。</li>
 *   <li><b>命中标记</b>：命中时在准星旁闪一个 X（白色=普通，橙色=爆头，红色=致死）。</li>
 * </ol>
 *
 * <p>定位是<b>优化</b>：只改观感与手感，不新增槽位、不新增配件、不往枪包里塞东西。
 * （曾经加过「枪管槽位 + 急速握把一体枪管」，连同 loader jar 一起全部撤掉了 ——
 *  那属于"创造"，不属于优化。相关源码、贴图、枪包、枪械数据改动都已清除。）
 *
 * <p>配置：{@code config/taczopt.properties}
 * （第一次启动会自动把旧的四个 properties 迁移进来，旧文件改名为 {@code *.migrated}）
 *
 * <p>排错：日志里搜 {@code [taczopt]}。
 */
@Mod(TaczOptimizeMod.MOD_ID)
public class TaczOptimizeMod {

    public static final String MOD_ID = "taczopt";

    private static final Logger LOGGER = LogUtils.getLogger();

    public TaczOptimizeMod() {
        // 先把合并配置载入（内含旧配置迁移），各功能再各自读取自己的前缀
        ConfigFile.ensureLoaded();
        // 四个功能各自的初始化（都在客户端，服务端只是空跑）
        if (FMLEnvironment.dist == Dist.CLIENT) {
            TBHConfig.ensureLoaded();
            TBHLog.modLoaded();
            OneShotConfig.ensureLoaded();
            OneShotLog.modLoaded();
            HybridAimConfig.ensureLoaded();
            AdsFireConfig.ensureLoaded();
        }
        // 命中标记：服务端转发命中事件 + 自定义包发给射手 + 客户端画 X。
        // 事件要手动注册到 FORGE 总线（这个类本身注册在 MOD 总线上）。
        HitFeedbackConfig.enabled();
        HitFeedbackNetwork.register();
        MinecraftForge.EVENT_BUS.register(HitFeedbackServer.class);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            registerClientHitFeedback();
            registerClientCrosshair();
        }
        LOGGER.info("[taczopt] Optimization for Tacz 已加载：弹孔 / 秒杀 / 混合开镜 / 开火自动开镜 / 命中标记 / 腰射准星；配置 config/taczopt.properties");
    }

    /**
     * 客户端专属的注册单独放一个方法。
     *
     * <p>不这样写的话，{@code HitFeedbackClient} 会被服务端也加载，
     * 而它引用了 {@code GuiGraphics} 这类纯客户端类 —— 专用服务器上直接 NoClassDefFoundError。
     */
    private static void registerClientHitFeedback() {
        MinecraftForge.EVENT_BUS.register(
                com.taczopt.hitfeedback.HitFeedbackClient.class);
    }

    /**
     * 腰射准星（三角洲样式）的客户端注册。
     *
     * <p>同样只在客户端加载 —— 它引用 {@code GuiGraphics}。
     * TaCZ 自己那套准星由 {@code CrosshairEventMixin} 拦，不需要在这里注册。
     */
    private static void registerClientCrosshair() {
        com.taczopt.crosshair.CrosshairConfig.enabled();
        MinecraftForge.EVENT_BUS.register(
                com.taczopt.crosshair.CrosshairStyleClient.class);
    }

    public static Logger logger() {
        return LOGGER;
    }

    /** 游戏根目录（.minecraft / 版本隔离目录），配置与脚本都放这儿 */
    public static Path gameDir() {
        return FMLPaths.GAMEDIR.get();
    }
}
