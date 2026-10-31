package com.taczfeel;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * config/taczfeel.properties
 * <p>
 * 不依赖 Forge/Cloth 的配置读取：文件不存在就按默认值写一份（带中文注释），
 * 之后每秒最多检查一次 mtime，改了存盘就热重载，不用重启游戏。
 * 解析失败的单条参数会被忽略并保留原值 —— 写错一个数字不会让游戏崩。
 */
public final class FeelConfig {

    private static final Path FILE = Paths.get("config", "taczfeel.properties");

    // ---------------- 手感：枪身视觉后坐 ----------------
    /** 开火时第一人称枪身是否有后坐动作 */
    public static boolean kickEnabled = true;
    /** 每发把枪身往玩家方向推多少格 */
    public static float kickBack = 0.024F;
    /** 每发把枪身抬高多少格 */
    public static float kickUp = 0.008F;
    /** 每发随机左右偏多少格（0 = 不随机） */
    public static float kickSide = 0.006F;
    /** 每发枪口上抬角度（度） */
    public static float kickPitch = 0.55F;
    /** 每发随机翻滚角度（度） */
    public static float kickRoll = 0.32F;
    /** 回弹速度（每秒衰减倍率的指数底，越大回得越快） */
    public static float kickRecover = 10.0F;
    /** 位移类后坐的最大累积值（格），防止连发时枪飞出屏幕 */
    public static float kickMax = 0.05F;
    /** 腰射时后坐强度倍数 */
    public static float kickHipfireScale = 0.75F;
    /** 开镜时后坐强度倍数 */
    public static float kickAdsScale = 1.0F;
    /** 是否按枪械自带后坐力数据缩放（重枪更沉、轻枪更飘） */
    public static boolean kickRecoilScale = true;

    // ---------------- 手感：镜头冲击 ----------------
    /** 开火时镜头是否短促震动 */
    public static boolean cameraEnabled = true;
    /** 每发镜头抬多少度（正数 = 往上跳） */
    public static float cameraPitch = 0.22F;
    /** 每发镜头随机左右偏多少度 */
    public static float cameraYaw = 0.06F;
    /** 每发镜头随机翻滚多少度 */
    public static float cameraRoll = 0.05F;
    /** 镜头冲击回正速度 */
    public static float cameraRecover = 14.0F;
    /** 腰射时镜头冲击倍数 */
    public static float cameraHipfireScale = 1.0F;
    /** 开镜时镜头冲击倍数 */
    public static float cameraAdsScale = 0.7F;
    /** 是否按枪械后坐力数据缩放镜头冲击 */
    public static boolean cameraRecoilScale = true;

    // ---------------- 视觉：枪口火光 ----------------
    /** 是否放大枪口火光 */
    public static boolean muzzleEnabled = true;
    /** 枪口火光大小倍数（1.0 = 枪包原样） */
    public static float muzzleScale = 1.3F;

    // ---------------- 视觉：开镜暗角 ----------------
    /** 开镜时屏幕边缘是否压暗 */
    public static boolean vignetteEnabled = true;
    /** 暗角最大不透明度 0.0 ~ 1.0 */
    public static float vignetteAlpha = 0.5F;
    /** 暗角颜色，RRGGBB（000000 = 黑） */
    public static String vignetteColor = "000000";
    /** 只在手持 TaCZ 枪械时画暗角 */
    public static boolean vignetteOnlyGun = true;

    // ---------------- 视觉：命中火花 ----------------
    /** 子弹命中实体时是否爆火花 */
    public static boolean hitSpark = true;
    /** 每次命中火花粒子数量 */
    public static int hitSparkCount = 5;
    /** 火花扩散速度 */
    public static float hitSparkSpeed = 0.35F;

    // ---------------- 视觉：曳光弹 ----------------
    /** 是否让每一发子弹都带曳光 */
    public static boolean tracerEnabled = true;
    /** 只给自己打出去的子弹加曳光（false = 别人的也加） */
    public static boolean tracerOnlySelf = true;
    /** 曳光粗细倍数（1.0 = TaCZ 原样） */
    public static float tracerSize = 1.0F;
    /** 是否强制使用下面这个颜色（false = 用枪包/弹药自己的曳光色） */
    public static boolean tracerForceColor = false;
    /** 曳光颜色 RRGGBB，留空表示跟随枪包 */
    public static String tracerColor = "";

    // ---------------- 视觉：开镜景深 ----------------
    /** 开镜时是否让画面外围虚化 */
    public static boolean dofEnabled = true;
    /** 最大模糊半径（像素） */
    public static float dofStrength = 6.0F;
    /** 中心保持锐利的范围，0 = 只有正中清晰，0.6 = 大部分清晰只看边缘糊 */
    public static float dofCore = 0.32F;
    /** 只在手持 TaCZ 枪械时生效 */
    public static boolean dofOnlyGun = true;

    // ---------------- 声音 ----------------
    /** 音效模块总开关 */
    public static boolean soundEnabled = true;
    /** 枪声音高随机幅度（0.02 = ±2%；0 = 关闭；上限 0.3） */
    public static float soundPitchRandom = 0.02F;
    /** 除"开火时间窗"外，声音名字里带这些词也当枪声（逗号分隔） */
    public static String soundNameExtra = "shoot,silence";
    /** 是否叠加"尾音"：把同一枪声变频变轻再放一次，做出枪声的尾巴 */
    public static boolean soundTail = true;
    /** 尾音层数（1 ~ 3） */
    public static int soundTailCount = 1;
    /** 尾音音量倍数（相对原枪声） */
    public static float soundTailVolume = 0.28F;
    /** 尾音音高倍数（小于 1 = 更低沉） */
    public static float soundTailPitch = 0.80F;
    /** 尾音延迟毫秒 */
    public static int soundTailDelayMs = 90;

    // ---------------- 其它 ----------------
    /** 调试日志 */
    public static boolean debugLog = false;

    private static volatile boolean loaded = false;
    private static long lastCheckMillis = 0L;
    private static long lastModified = -1L;

    private FeelConfig() {
    }

    public static void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (FeelConfig.class) {
            if (loaded) {
                return;
            }
            load();
            loaded = true;
        }
    }

    /** 每秒最多检查一次配置文件是否被改动 */
    public static void reloadIfChanged() {
        ensureLoaded();
        long now = System.currentTimeMillis();
        if (now - lastCheckMillis < 1000L) {
            return;
        }
        lastCheckMillis = now;
        long modified;
        try {
            if (!Files.exists(FILE)) {
                return;
            }
            modified = Files.getLastModifiedTime(FILE).toMillis();
        } catch (Exception e) {
            return;
        }
        if (modified == lastModified) {
            return;
        }
        lastModified = modified;
        load();
        FeelLog.info("config reloaded: kick=" + kickEnabled + " camera=" + cameraEnabled
                + " muzzle=" + muzzleEnabled + "(" + muzzleScale + ")"
                + " vignette=" + vignetteEnabled + " sound=" + soundEnabled
                + " pitchRandom=" + soundPitchRandom + " tail=" + soundTail);
    }

    public static void load() {
        try {
            if (!Files.exists(FILE)) {
                writeDefault();
            }
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(FILE);
                 Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                p.load(r);
            }
            lastModified = Files.getLastModifiedTime(FILE).toMillis();

            // 手感
            kickEnabled = bool(p, "kick.enabled", kickEnabled);
            kickBack = f(p, "kick.back", kickBack);
            kickUp = f(p, "kick.up", kickUp);
            kickSide = f(p, "kick.side", kickSide);
            kickPitch = f(p, "kick.pitch_deg", kickPitch);
            kickRoll = f(p, "kick.roll_deg", kickRoll);
            kickRecover = f(p, "kick.recover", kickRecover);
            kickMax = f(p, "kick.max", kickMax);
            kickHipfireScale = f(p, "kick.hipfire_scale", kickHipfireScale);
            kickAdsScale = f(p, "kick.ads_scale", kickAdsScale);
            kickRecoilScale = bool(p, "kick.recoil_scale", kickRecoilScale);

            cameraEnabled = bool(p, "camera.enabled", cameraEnabled);
            cameraPitch = f(p, "camera.pitch_deg", cameraPitch);
            cameraYaw = f(p, "camera.yaw_deg", cameraYaw);
            cameraRoll = f(p, "camera.roll_deg", cameraRoll);
            cameraRecover = f(p, "camera.recover", cameraRecover);
            cameraHipfireScale = f(p, "camera.hipfire_scale", cameraHipfireScale);
            cameraAdsScale = f(p, "camera.ads_scale", cameraAdsScale);
            cameraRecoilScale = bool(p, "camera.recoil_scale", cameraRecoilScale);

            // 视觉
            muzzleEnabled = bool(p, "muzzle.enabled", muzzleEnabled);
            muzzleScale = f(p, "muzzle.scale", muzzleScale);

            vignetteEnabled = bool(p, "vignette.enabled", vignetteEnabled);
            vignetteAlpha = f(p, "vignette.alpha", vignetteAlpha);
            vignetteColor = s(p, "vignette.color", vignetteColor);
            vignetteOnlyGun = bool(p, "vignette.only_gun", vignetteOnlyGun);

            hitSpark = bool(p, "hit.spark", hitSpark);
            hitSparkCount = i(p, "hit.spark_count", hitSparkCount);
            hitSparkSpeed = f(p, "hit.spark_speed", hitSparkSpeed);

            tracerEnabled = bool(p, "tracer.enabled", tracerEnabled);
            tracerOnlySelf = bool(p, "tracer.only_self", tracerOnlySelf);
            tracerSize = f(p, "tracer.size", tracerSize);
            tracerForceColor = bool(p, "tracer.force_color", tracerForceColor);
            tracerColor = s(p, "tracer.color", tracerColor);

            dofEnabled = bool(p, "dof.enabled", dofEnabled);
            dofStrength = f(p, "dof.strength", dofStrength);
            dofCore = f(p, "dof.core", dofCore);
            dofOnlyGun = bool(p, "dof.only_gun", dofOnlyGun);

            // 声音
            soundEnabled = bool(p, "sound.enabled", soundEnabled);
            soundPitchRandom = f(p, "sound.pitch_random", soundPitchRandom);
            soundNameExtra = s(p, "sound.name_extra", soundNameExtra);
            soundTail = bool(p, "sound.tail", soundTail);
            soundTailCount = i(p, "sound.tail_count", soundTailCount);
            soundTailVolume = f(p, "sound.tail_volume", soundTailVolume);
            soundTailPitch = f(p, "sound.tail_pitch", soundTailPitch);
            soundTailDelayMs = i(p, "sound.tail_delay_ms", soundTailDelayMs);

            debugLog = bool(p, "debug.log", debugLog);

            clampAll();
        } catch (Exception e) {
            FeelLog.warn("config read failed, keeping previous values: " + e);
        }
    }

    private static void clampAll() {
        kickBack = clamp(kickBack, -0.5F, 0.5F);
        kickUp = clamp(kickUp, -0.5F, 0.5F);
        kickSide = clamp(kickSide, 0F, 0.3F);
        kickPitch = clamp(kickPitch, 0F, 15F);
        kickRoll = clamp(kickRoll, 0F, 15F);
        kickRecover = clamp(kickRecover, 0.5F, 60F);
        kickMax = clamp(kickMax, 0F, 0.6F);
        kickHipfireScale = clamp(kickHipfireScale, 0F, 5F);
        kickAdsScale = clamp(kickAdsScale, 0F, 5F);

        cameraPitch = clamp(cameraPitch, 0F, 12F);
        cameraYaw = clamp(cameraYaw, 0F, 8F);
        cameraRoll = clamp(cameraRoll, 0F, 8F);
        cameraRecover = clamp(cameraRecover, 0.5F, 60F);
        cameraHipfireScale = clamp(cameraHipfireScale, 0F, 5F);
        cameraAdsScale = clamp(cameraAdsScale, 0F, 5F);

        muzzleScale = clamp(muzzleScale, 0.1F, 5F);
        vignetteAlpha = clamp(vignetteAlpha, 0F, 1F);
        hitSparkCount = Math.max(0, Math.min(24, hitSparkCount));
        hitSparkSpeed = clamp(hitSparkSpeed, 0F, 2F);

        tracerSize = clamp(tracerSize, 0.1F, 5F);
        dofStrength = clamp(dofStrength, 0F, 24F);
        dofCore = clamp(dofCore, 0F, 0.95F);

        soundPitchRandom = clamp(soundPitchRandom, 0F, 0.3F);
        soundTailCount = Math.max(0, Math.min(3, soundTailCount));
        soundTailVolume = clamp(soundTailVolume, 0F, 2F);
        soundTailPitch = clamp(soundTailPitch, 0.4F, 2F);
        soundTailDelayMs = Math.max(0, Math.min(1000, soundTailDelayMs));
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }

    private static boolean bool(Properties p, String key, boolean def) {
        String v = p.getProperty(key);
        if (v == null) {
            return def;
        }
        v = v.trim();
        if ("true".equalsIgnoreCase(v) || "on".equalsIgnoreCase(v) || "1".equals(v)) {
            return true;
        }
        if ("false".equalsIgnoreCase(v) || "off".equalsIgnoreCase(v) || "0".equals(v)) {
            return false;
        }
        return def;
    }

    private static float f(Properties p, String key, float def) {
        String v = p.getProperty(key);
        if (v == null) {
            return def;
        }
        try {
            return Float.parseFloat(v.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static int i(Properties p, String key, int def) {
        String v = p.getProperty(key);
        if (v == null) {
            return def;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static String s(Properties p, String key, String def) {
        String v = p.getProperty(key);
        return v == null ? def : v.trim();
    }

    private static void writeDefault() {
        String text = "# TaCZ 质感增强（taczfeel）—— 全部参数都在这里\n"
                + "# 保存后 1 秒内热重载，不用重启游戏。把某一项改成 0 / false 就是关掉它。\n"
                + "\n"
                + "# ---------------- 手感：枪身视觉后坐 ----------------\n"
                + "# 开火瞬间第一人称枪模往后推、往上抬、随机侧偏，然后按 recover 回弹。\n"
                + "# 方向和幅度不合口味就直接调：觉得反了就把 back 改成负数。\n"
                + "kick.enabled=true\n"
                + "kick.back=0.045\n"
                + "kick.up=0.015\n"
                + "kick.side=0.010\n"
                + "kick.pitch_deg=1.1\n"
                + "kick.roll_deg=0.6\n"
                + "kick.recover=10.0\n"
                + "kick.max=0.09\n"
                + "kick.hipfire_scale=0.75\n"
                + "kick.ads_scale=1.0\n"
                + "# 按枪械自带后坐力数据缩放：重枪更沉、冲锋枪更飘\n"
                + "kick.recoil_scale=true\n"
                + "\n"
                + "# ---------------- 手感：镜头冲击 ----------------\n"
                + "# 在 TaCZ 自己的镜头后坐上再叠一层很短的抖动，开镜时默认只给 70%。\n"
                + "camera.enabled=true\n"
                + "camera.pitch_deg=0.45\n"
                + "camera.yaw_deg=0.12\n"
                + "camera.roll_deg=0.10\n"
                + "camera.recover=14.0\n"
                + "camera.hipfire_scale=1.0\n"
                + "camera.ads_scale=0.7\n"
                + "camera.recoil_scale=true\n"
                + "\n"
                + "# ---------------- 视觉：枪口火光 ----------------\n"
                + "# 1.0 = 枪包原样，1.3 = 放大三成；改完立刻生效\n"
                + "muzzle.enabled=true\n"
                + "muzzle.scale=1.3\n"
                + "\n"
                + "# ---------------- 视觉：开镜暗角 ----------------\n"
                + "# 随开镜进度淡入的四周压暗，alpha 0.5 已经很明显，0.3 更收敛\n"
                + "vignette.enabled=true\n"
                + "vignette.alpha=0.5\n"
                + "vignette.color=000000\n"
                + "vignette.only_gun=true\n"
                + "\n"
                + "# ---------------- 视觉：命中火花 ----------------\n"
                + "hit.spark=true\n"
                + "hit.spark_count=5\n"
                + "hit.spark_speed=0.35\n"
                + "\n"
                + "# ---------------- 视觉：曳光弹 ----------------\n"
                + "# 默认每发子弹都带曳光（走 TaCZ 自己的曳光渲染路径，兼容光影包）\n"
                + "# only_self=false 可以让别人打过来的子弹也带曳光\n"
                + "# force_color=true 时用下面的 color 强制染色，否则跟随枪包/弹药的曳光色\n"
                + "tracer.enabled=true\n"
                + "tracer.only_self=true\n"
                + "tracer.size=1.0\n"
                + "tracer.force_color=false\n"
                + "tracer.color=\n"
                + "\n"
                + "# ---------------- 视觉：开镜景深 ----------------\n"
                + "# 是屏幕外围虚化（世界糊、枪模和 HUD 永远清晰），随开镜进度淡入。\n"
                + "# strength = 最大模糊半径（像素），core = 中心锐利范围（0~0.95）\n"
                + "dof.enabled=true\n"
                + "dof.strength=6.0\n"
                + "dof.core=0.32\n"
                + "dof.only_gun=true\n"
                + "\n"
                + "# ---------------- 声音 ----------------\n"
                + "# pitch_random：每发枪声音高随机幅度，0.02 = ±2%，连发时不再像复读机\n"
                + "# tail：把同一枪声变频变轻再放一次，做枪声的尾巴 / 距离感\n"
                + "sound.enabled=true\n"
                + "sound.pitch_random=0.02\n"
                + "sound.name_extra=shoot,silence\n"
                + "sound.tail=true\n"
                + "sound.tail_count=1\n"
                + "sound.tail_volume=0.28\n"
                + "sound.tail_pitch=0.80\n"
                + "sound.tail_delay_ms=90\n"
                + "\n"
                + "# ---------------- 其它 ----------------\n"
                + "debug.log=false\n";
        try {
            Files.createDirectories(FILE.getParent());
            Path tmp = FILE.resolveSibling("taczfeel.properties.tmp");
            try (OutputStream out = Files.newOutputStream(tmp);
                 Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                w.write(text);
            }
            Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
            FeelLog.info("wrote default config -> " + FILE);
        } catch (Exception e) {
            FeelLog.warn("cannot write default config: " + e);
        }
    }
}
