package com.taczopt.hitfeedback;

import com.taczopt.config.ConfigFile;

/**
 * 命中标记的配置读取。
 *
 * <p>前缀 {@code hit.*}，键都写进 {@code config/taczopt.properties}，
 * 保存后 1 秒内热重载（沿用 {@link ConfigFile} 的机制）。
 */
public final class HitFeedbackConfig {

    /** 总开关 */
    public static final String KEY_ENABLED = "hit.enabled";
    /** 标记停留时长（毫秒） */
    public static final String KEY_DURATION_MS = "hit.duration_ms";
    /** 标记大小倍率（0.5~2.5，1.0 = 默认，X 的臂长 5 像素） */
    public static final String KEY_SCALE = "hit.scale";
    /** 命中确认音 */
    public static final String KEY_SOUND = "hit.sound";
    /** 命中瞬间的镜头微抖 */
    public static final String KEY_SHAKE = "hit.shake";
    /**
     * 标记相对准星（屏幕中心）的垂直偏移。默认 3 = 往下 3 像素，负值向上。
     *
     * <p>屏幕坐标是 <b>y 向下为正</b>（Minecraft 的 GUI 坐标系和屏幕一致），
     * 所以"往下"是加、"往上"是减。
     *
     * <p><b>锚点在哪：</b>TaCZ 自己的命中标记用的是
     * {@code window.getGuiScaledWidth() / 2 - 8} 和 {@code getGuiScaledHeight() / 2 - 8}
     * （见 {@code com.tacz.guns.client.event.RenderCrosshairEvent.renderHitMarker} 的字节码）——
     * 减 8 是因为它的贴图 32x32、图案占中间 16x16，减掉一半才是把<b>图案中心</b>
     * 落在 GUI 中心。所以 TaCZ 的准星和它自己的标记都锚在 GUI 中心，
     * 本模组的标记也锚在同一个点（{@code m_85445_()/2}、{@code m_85446_()/2}）。
     *
     * <p>3 是逐次小步试出来的：0（正中心）时标记正压在准星上，往下挪一点点更顺眼，
     *
     * <p>⚠ 别按"差多少像素"硬算 —— 实测这个值到屏幕像素的换算不是 1:1
     * （按像素差算过一次 -18，抬过头了）。要调就小步试，一次 5~10。
     */
    public static final String KEY_OFFSET_Y = "hit.offset_y";

    /**
     * 标记相对准星（屏幕中心）的水平偏移，正值向右。默认 0 = 正对中心。
     *
     * <p>和 {@link #KEY_OFFSET_Y} 一起构成标记的屏幕坐标：
     * <pre>
     *   x = window.getGuiScaledWidth()  / 2 + hit.offset_x
     *   y = window.getGuiScaledHeight() / 2 + hit.offset_y
     * </pre>
     * 两个都支持热重载（改完保存，1 秒内生效，不用重启游戏，也不用重新构建）。
     */
    public static final String KEY_OFFSET_X = "hit.offset_x";
    /** 调试日志：每次命中打一行 */
    public static final String KEY_DEBUG_LOG = "hit.debug_log";

    private HitFeedbackConfig() {
    }

    public static boolean enabled() {
        ConfigFile.ensureLoaded();
        return ConfigFile.getBool(KEY_ENABLED, true);
    }

    public static int durationMs() {
        ConfigFile.ensureLoaded();
        int v = ConfigFile.getInt(KEY_DURATION_MS, 450);
        // 低于 80ms 人眼基本抓不到；上限也别太夸张，否则屏幕上一直挂着标记
        return Math.max(80, Math.min(v, 3000));
    }

    public static float scale() {
        ConfigFile.ensureLoaded();
        double v = ConfigFile.getDouble(KEY_SCALE, 1.0);
        return (float) Math.max(0.5, Math.min(v, 2.5));
    }

    /**
     * 爆头标记曾经用独立的尺寸（{@code hit.headshot_size}）和双 X 间距
     * （{@code hit.headshot_spacing}）。
     *
     * <p>这两个键<b>已经删掉</b>：爆头现在只是把同一个 X 换成橙色，不再换尺寸、
     * 也不再画两个。键名进了 {@code ConfigFile.DEPRECATED_KEYS}，老配置文件里
     * 残留的会被自动清掉，不会留着一个看起来还生效的开关。
     */

    public static boolean sound() {
        ConfigFile.ensureLoaded();
        return ConfigFile.getBool(KEY_SOUND, true);
    }

    public static boolean shake() {
        ConfigFile.ensureLoaded();
        return ConfigFile.getBool(KEY_SHAKE, true);
    }

    public static int offsetY() {
        ConfigFile.ensureLoaded();
        int v = ConfigFile.getInt(KEY_OFFSET_Y, 3);
        // 范围放宽一些：不同 GUI scale 下要挪的像素数不一样，别把人卡死
        return Math.max(-400, Math.min(v, 400));
    }

    /** 水平偏移，正值向右 */
    public static int offsetX() {
        ConfigFile.ensureLoaded();
        int v = ConfigFile.getInt(KEY_OFFSET_X, 0);
        return Math.max(-400, Math.min(v, 400));
    }

    public static boolean debugLog() {
        ConfigFile.ensureLoaded();
        return ConfigFile.getBool(KEY_DEBUG_LOG, false);
    }
}
