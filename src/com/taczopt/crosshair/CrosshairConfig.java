package com.taczopt.crosshair;

import com.taczopt.config.ConfigFile;

/**
 * 腰射准星（三角洲样式）的配置读取。
 *
 * <p>前缀 {@code crosshair.*}，写进 {@code config/taczopt.properties}，
 * 保存后 1 秒内热重载（沿用 {@link ConfigFile} 的机制）。
 */
public final class CrosshairConfig {

    /** 总开关：关掉就回到 TaCZ 原本的准星（默认开启；关掉后 TaCZ 的准星和命中标记都会回来） */
    public static final String KEY_ENABLED = "crosshair.enabled";
    /** 颜色，十六进制 RGB（如 FF3377）。默认白色（细白线最不挡视野） */
    public static final String KEY_COLOR = "crosshair.color";
    /** 线宽（像素） */
    public static final String KEY_THICKNESS = "crosshair.thickness";
    /** 线段长度（像素） */
    public static final String KEY_ARM = "crosshair.arm";
    /** 静态模式下的固定间距（dynamic=false 时用）；动态模式下不用它 */
    public static final String KEY_GAP = "crosshair.gap";
    /** 是否让间距随扩散变化（关掉=静态准星） */
    public static final String KEY_DYNAMIC = "crosshair.dynamic";
    /** 间距倍率：间距 = 扩散值(=10-腰射精度) * 这个值。默认 1.0 就是"所见即所得" */
    public static final String KEY_GAP_MULTIPLIER = "crosshair.gap_multiplier";
    /** 间距上限（像素），防止极端数值把准星甩出屏幕（默认 60） */
    public static final String KEY_GAP_MAX = "crosshair.gap_max";
    /** 中心点大小（像素），0 = 不画中心点 */
    public static final String KEY_DOT = "crosshair.dot";
    /** 开镜时是否隐藏（三角洲就是这样） */
    public static final String KEY_HIDE_ON_AIM = "crosshair.hide_on_aim";
    /** 持枪以外的场合是否也替换准星（默认 false：空手/挖矿时用原版） */
    public static final String KEY_ONLY_GUNS = "crosshair.only_guns";
    /** 准星整体缩放（在"10-精度"算出的间距上再乘）。1.0 = 所见即所得 */
    public static final String KEY_SIZE_SCALE = "crosshair.spread_scale";
    /** 调试日志 */
    public static final String KEY_DEBUG_LOG = "crosshair.debug_log";

    private CrosshairConfig() {
    }

    public static boolean enabled() {
        ConfigFile.ensureLoaded();
        return ConfigFile.getBool(KEY_ENABLED, true);
    }

    /** 解析 {@code RRGGBB} 或 {@code #RRGGBB}，解析失败退回白色 */
    public static int color() {
        ConfigFile.ensureLoaded();
        String v = ConfigFile.get(KEY_COLOR, "FFFFFF").trim();
        if (v.startsWith("#")) {
            v = v.substring(1);
        }
        if (v.startsWith("0x") || v.startsWith("0X")) {
            v = v.substring(2);
        }
        try {
            return Integer.parseInt(v, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }

    public static int thickness() {
        ConfigFile.ensureLoaded();
        return clamp(ConfigFile.getInt(KEY_THICKNESS, 1), 1, 8);
    }

    public static int arm() {
        ConfigFile.ensureLoaded();
        return clamp(ConfigFile.getInt(KEY_ARM, 5), 1, 40);
    }

    public static int gap() {
        ConfigFile.ensureLoaded();
        return clamp(ConfigFile.getInt(KEY_GAP, 8), 1, 80);
    }

    public static boolean dynamic() {
        ConfigFile.ensureLoaded();
        return ConfigFile.getBool(KEY_DYNAMIC, true);
    }

    public static double gapMultiplier() {
        ConfigFile.ensureLoaded();
        double v = ConfigFile.getDouble(KEY_GAP_MULTIPLIER, 1.0);
        return Math.max(0.0, Math.min(v, 20.0));
    }

    public static int gapMax() {
        ConfigFile.ensureLoaded();
        return clamp(ConfigFile.getInt(KEY_GAP_MAX, 60), 4, 200);
    }

    public static int dot() {
        ConfigFile.ensureLoaded();
        return clamp(ConfigFile.getInt(KEY_DOT, 1), 0, 8);
    }

    public static boolean hideOnAim() {
        ConfigFile.ensureLoaded();
        return ConfigFile.getBool(KEY_HIDE_ON_AIM, true);
    }

    public static boolean onlyGuns() {
        ConfigFile.ensureLoaded();
        return ConfigFile.getBool(KEY_ONLY_GUNS, true);
    }

    /**
     * 腰射扩散的缩放倍率：<b>间距 = 该枪的腰射扩散 × 这个值</b>。
     *
     * <p>只作用于"扩散"这一个量。线长 / 线宽 / 中心点是准星自己的形状参数，
     * 各有各的键，<b>不跟着一起乘</b> —— 混在一起乘就没法单独调粗细了。
     *
     * <p>默认 2.5 是实测调出来的：把 inaccuracy 直接当像素用时，
     * 准星明显小于实际散布（实测弹孔离中心约为准星的 2.3~2.5 倍），
     * 看着就像"打不准"。
     */
    public static float spreadScale() {
        ConfigFile.ensureLoaded();
        double v = ConfigFile.getDouble(KEY_SIZE_SCALE, 2.5);
        return (float) Math.max(0.2, Math.min(v, 5.0));
    }

    public static boolean debugLog() {
        ConfigFile.ensureLoaded();
        return ConfigFile.getBool(KEY_DEBUG_LOG, false);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(v, hi));
    }
}
