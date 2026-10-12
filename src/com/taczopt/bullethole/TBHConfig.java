package com.taczopt.bullethole;

import com.taczopt.config.ConfigFile;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 自定义弹孔的配置。键前缀 {@code hole.}，存在合并版配置 config/taczopt.properties 里。
 * 不依赖任何 Forge API；热重载由 {@link ConfigFile} 统一负责。
 *
 * <p>两个互相独立的匹配列表（写法相同：* 全部 / auto 自动识别 / 精确 id / 通配符 * 与 ?，逗号分隔）：
 * <pre>
 *   hole.guns            -> 哪些枪显示悬浮 logo
 *   hole.hole_scale_guns -> 哪些枪改方块面上原版弹孔的大小（默认 *，即全部枪）
 * </pre>
 */
public final class TBHConfig {

    private static final String P = "hole.";
    /**
     * 贴图命名空间故意保留 taczbullethole：资源包 assets/taczbullethole/... 不用改。
     *
     * <p>⚠ 这里必须用 {@code new ResourceLocation(ns, path)}，而这个构造器在 1.20.1
     * 标了 {@code @Deprecated(forRemoval = true)}。试过换成 {@code ResourceLocation.tryParse}
     * —— <b>那个方法在这个版本里不存在</b>（javac: 找不到符号）。Forge 的 SRG
     * client jar 里也没有。所以只能留着旧构造器并抑制警告：
     * 不抑制的话 javac 会往 stderr 写提示，而 build.ps1 把"javac 的 stderr 非空"
     * 当成构建失败信号（详见其中 Invoke-Native 的注释）。
     */
    @SuppressWarnings({"deprecation", "removal"})
    private static final ResourceLocation FALLBACK =
            new ResourceLocation("taczbullethole", "block/bullet_hole");

    /** 是否启用悬浮 logo（false = 完全恢复原版，包括不改弹孔大小） */
    public static boolean enabled = true;
    /** 悬浮 logo 的大小倍数，1.0 = 0.1 格 */
    public static float scale = 1.0F;
    /** 方块面上原版弹孔的大小倍数，1.0 = TaCZ 原始大小（0.1 格） */
    public static float holeScale = 0.6F;
    /** 悬浮 logo 离方块面的高度（格） */
    public static float logoLift = 0.03F;
    /** 悬浮 logo 的不透明度 0.0 ~ 1.0 */
    public static float alpha = 1.0F;
    /** logo 显示多少 tick 后开始淡出（20 tick = 1 秒） */
    public static int logoHold = 20;
    /** 淡出持续多少 tick */
    public static int logoFade = 20;
    /** 左右镜像 */
    public static boolean flipU = false;
    /** 上下颠倒 */
    public static boolean flipV = true;

    private static volatile boolean loaded = false;
    private static volatile ResourceLocation textureId = FALLBACK;

    private static final ListMatcher LOGO = new ListMatcher();
    private static final ListMatcher HOLE = new ListMatcher();

    private static String lastLogoRaw = "auto";
    private static String lastHoleRaw = "*";

    private TBHConfig() {
    }

    public static void ensureLoaded() {
        ConfigFile.ensureLoaded();
        if (loaded) {
            return;
        }
        synchronized (TBHConfig.class) {
            if (loaded) {
                return;
            }
            load();
            loaded = true;
        }
    }

    /** logo 显示 + 淡出所需的总 tick 数 */
    public static int logoTicks() {
        ensureLoaded();
        return Math.max(logoHold, 0) + Math.max(logoFade, 0);
    }

    /**
     * 热重载。合并版里配置文件由 {@link ConfigFile} 统一监视，
     * 这里只在匹配列表真的变了的时候重建规则。
     */
    public static void reloadIfChanged() {
        ensureLoaded();
        ConfigFile.reloadIfChanged();
        String guns = ConfigFile.get(P + "guns", "auto");
        String holeGuns = ConfigFile.get(P + "hole_scale_guns", "*");
        if (!guns.equals(lastLogoRaw) || !holeGuns.equals(lastHoleRaw)) {
            load();
        }
    }

    private static void load() {
        enabled = ConfigFile.getBool(P + "enabled", true);
        flipU = ConfigFile.getBool(P + "flip_u", false);
        flipV = ConfigFile.getBool(P + "flip_v", true);
        scale = (float) ConfigFile.getDouble(P + "scale", 1.0D);
        holeScale = (float) ConfigFile.getDouble(P + "hole_scale", 0.6D);
        logoLift = (float) ConfigFile.getDouble(P + "logo_lift", 0.03D);
        alpha = (float) ConfigFile.getDouble(P + "alpha", 1.0D);
        logoHold = ConfigFile.getInt(P + "logo_hold", 20);
        logoFade = ConfigFile.getInt(P + "logo_fade", 20);
        textureId = parseTexture(ConfigFile.get(P + "texture", "taczbullethole:block/bullet_hole"));

        TBHSpecial.clearCache();
        lastLogoRaw = ConfigFile.get(P + "guns", "auto");
        lastHoleRaw = ConfigFile.get(P + "hole_scale_guns", "*");
        LOGO.parse(lastLogoRaw);
        HOLE.parse(lastHoleRaw);
    }

    /** 一套匹配规则：all(*) / auto(按 § 标签自动识别) / 精确 id / 通配符 */
    private static final class ListMatcher {
        private final Set<String> exact = new HashSet<String>();
        private final List<Pattern> patterns = new ArrayList<Pattern>();
        private boolean all = false;
        private boolean auto = false;
        private String raw = "*";

        void parse(String value) {
            exact.clear();
            patterns.clear();
            all = false;
            auto = false;
            raw = value == null ? "*" : value.trim();
            if (raw.isEmpty() || "*".equals(raw)) {
                all = true;
                return;
            }
            for (String part : raw.split(",")) {
                String id = part.trim();
                if (id.isEmpty()) {
                    continue;
                }
                if ("auto".equalsIgnoreCase(id) || "special".equalsIgnoreCase(id)) {
                    auto = true;
                    continue;
                }
                if (id.indexOf('*') >= 0 || id.indexOf('?') >= 0) {
                    patterns.add(toPattern(id));
                } else {
                    exact.add(id.toLowerCase());
                }
            }
        }

        boolean matches(String gunId) {
            if (all) {
                return true;
            }
            if (gunId == null) {
                return false;
            }
            if (auto && TBHSpecial.isSpecial(gunId)) {
                return true;
            }
            String lower = gunId.toLowerCase();
            if (exact.contains(lower)) {
                return true;
            }
            for (int i = 0; i < patterns.size(); i++) {
                if (patterns.get(i).matcher(lower).matches()) {
                    return true;
                }
            }
            return false;
        }
    }

    /** 这把枪是否显示悬浮 logo */
    public static boolean applies(String gunId) {
        ensureLoaded();
        if (!enabled) {
            return false;
        }
        return LOGO.matches(gunId);
    }

    /** 这把枪在方块面上的原版弹孔是否要改大小（默认全部枪） */
    public static boolean holeApplies(String gunId) {
        ensureLoaded();
        if (!enabled) {
            return false;
        }
        return HOLE.matches(gunId);
    }

    public static String gunsRaw() {
        ensureLoaded();
        return LOGO.raw;
    }

    public static String holeGunsRaw() {
        ensureLoaded();
        return HOLE.raw;
    }

    /** logo 贴图 id（已解析好并缓存） */
    public static ResourceLocation texture() {
        ensureLoaded();
        return textureId;
    }

    /** 把 * / ? 通配符转成正则 */
    private static Pattern toPattern(String glob) {
        StringBuilder sb = new StringBuilder(glob.length() + 8);
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            if (c == '*') {
                sb.append(".*");
            } else if (c == '?') {
                sb.append('.');
            } else {
                if ("\\^$.|+()[]{}".indexOf(c) >= 0) {
                    sb.append('\\');
                }
                sb.append(c);
            }
        }
        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
    }

    private static ResourceLocation parseTexture(String value) {
        String s = value == null ? "" : value.trim();
        int i = s.indexOf(':');
        String namespace = i > 0 ? s.substring(0, i) : "taczbullethole";
        String path = i > 0 ? s.substring(i + 1) : s;
        if (path.isEmpty()) {
            namespace = "taczbullethole";
            path = "block/bullet_hole";
        }
        try {
            return build(namespace, path);
        } catch (RuntimeException e) {
            return FALLBACK;
        }
    }

    /**
     * 单独一层只为挂 {@code @SuppressWarnings}（原因见 {@link #FALLBACK}）。
     *
     * <p>必须同时写 {@code "removal"} —— 1.20.1 把这两个构造器标成
     * {@code @Deprecated(forRemoval = true)}，javac 因此报的是 {@code [removal]}
     * 类别，而 {@code "deprecation"} <b>抑制不了它</b>（实测：只写 deprecation 时
     * 仍会输出"某些输入文件使用或覆盖了已过时的 API"）。
     */
    @SuppressWarnings({"deprecation", "removal"})
    private static ResourceLocation build(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }
}
