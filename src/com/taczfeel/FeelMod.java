package com.taczfeel;

import net.minecraftforge.fml.common.Mod;

/**
 * TaCZ 质感增强（Feel for TaCZ）
 * <p>
 * 纯客户端，不改任何游戏逻辑，只加"看得见 / 听得见"的东西：
 * <ul>
 *   <li>手感：开火时第一人称枪身视觉后坐（后推 + 抬头 + 随机侧偏），并按枪械真实后坐力数据缩放</li>
 *   <li>手感：镜头短促上跳 + 随机偏航/翻滚，可只对开镜或腰射生效</li>
 *   <li>视觉：枪口火光放大</li>
 *   <li>视觉：开镜时屏幕边缘暗角（随开镜进度淡入）</li>
 *   <li>视觉：命中时在弹着点爆出火花</li>
 *   <li>声音：枪声随机音高 + 可选"尾音"叠层（同一枪声变频变轻再放一次）</li>
 * </ul>
 * 参数全部在 config/taczfeel.properties，存盘 1 秒内热重载。
 */
public final class FeelMod {

    public static final String MOD_ID = "taczopt";

    public static void init() {
        FeelConfig.ensureLoaded();
        FeelLog.info("loaded: kick=" + FeelConfig.kickEnabled
                + " camera=" + FeelConfig.cameraEnabled
                + " muzzle=" + FeelConfig.muzzleEnabled + "(" + FeelConfig.muzzleScale + ")"
                + " vignette=" + FeelConfig.vignetteEnabled
                + " spark=" + FeelConfig.hitSpark
                + " tracer=" + FeelConfig.tracerEnabled + "(size=" + FeelConfig.tracerSize
                + ",onlySelf=" + FeelConfig.tracerOnlySelf + ")"
                + " dof=" + FeelConfig.dofEnabled + "(strength=" + FeelConfig.dofStrength
                + ",core=" + FeelConfig.dofCore + ")"
                + " sound=" + FeelConfig.soundEnabled
                + " pitchRandom=" + FeelConfig.soundPitchRandom
                + " tail=" + FeelConfig.soundTail);
    }
}
