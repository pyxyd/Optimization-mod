package com.taczopt.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 合并版的单一配置文件：{@code config/taczopt.properties}
 *
 * <p>四个功能各自的前缀：
 * <pre>
 *   hole.*      自定义弹孔
 *   oneshot.*   指定枪械秒杀
 *   hybridaim.* 混合开镜
 *   adsfire.*   开火自动开镜（含每把枪的开关）
 * </pre>
 *
 * <p>为了不丢玩家已有设置，第一次加载时会把旧的四个文件
 * （taczbullethole / taczoneshot / taczhybridaim / taczadsfire.properties）
 * 自动迁移过来，然后把旧文件改名为 {@code *.migrated}。
 *
 * <p>文件改动后热重载（每秒最多检查一次）。
 */
public final class ConfigFile {

    private static final Path FILE = resolveFile();

    /**
     * 配置文件路径。默认 {@code config/taczopt.properties}（相对游戏目录）。
     * 系统属性 {@code taczopt.configdir} 可以覆盖根目录，只有离线测试会用到。
     */
    private static Path resolveFile() {
        String override = System.getProperty("taczopt.configdir");
        if (override != null && !override.isEmpty()) {
            return Paths.get(override, "config", "taczopt.properties");
        }
        return Paths.get("config", "taczopt.properties");
    }

    private static Path legacyFile(String name) {
        String override = System.getProperty("taczopt.configdir");
        if (override != null && !override.isEmpty()) {
            return Paths.get(override, "config", name);
        }
        return Paths.get("config", name);
    }

    private static volatile Properties props = new Properties();
    private static volatile boolean loaded = false;
    private static long lastCheckMillis = 0L;
    private static long lastModified = -1L;

    private ConfigFile() {
    }

    // ------------------------------------------------------------------ 加载 / 热重载

    public static void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (ConfigFile.class) {
            if (loaded) {
                return;
            }
            migrateLegacy();
            reload();
            loaded = true;
        }
    }

    public static void reloadIfChanged() {
        long now = System.currentTimeMillis();
        if (now - lastCheckMillis < 1000L) {
            return;
        }
        lastCheckMillis = now;
        long modified;
        try {
            modified = Files.isRegularFile(FILE) ? Files.getLastModifiedTime(FILE).toMillis() : -1L;
        } catch (IOException e) {
            return;
        }
        if (modified != lastModified) {
            synchronized (ConfigFile.class) {
                reload();
            }
        }
    }

    private static void reload() {
        Properties p = new Properties();
        if (Files.isRegularFile(FILE)) {
            try (InputStream in = Files.newInputStream(FILE)) {
                p.load(in);
            } catch (IOException ignored) {
                // 读不了就用默认值
            }
        } else {
            p = defaults();
            write(p);
        }
        props = p;
        try {
            lastModified = Files.isRegularFile(FILE) ? Files.getLastModifiedTime(FILE).toMillis() : -1L;
        } catch (IOException e) {
            lastModified = -1L;
        }
    }

    // ------------------------------------------------------------------ 读

    public static String get(String key, String def) {
        ensureLoaded();
        String v = props.getProperty(key);
        return v == null ? def : v.trim();
    }

    public static boolean getBool(String key, boolean def) {
        String v = props.getProperty(key);
        return v == null ? def : Boolean.parseBoolean(v.trim());
    }

    public static int getInt(String key, int def) {
        String v = props.getProperty(key);
        if (v == null) {
            return def;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static double getDouble(String key, double def) {
        String v = props.getProperty(key);
        if (v == null) {
            return def;
        }
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** 取某个前缀下的所有键（去掉前缀后的名字）。用于 gun.<id> 这种动态键 */
    public static List<String> keysWithPrefix(String prefix) {
        ensureLoaded();
        List<String> out = new ArrayList<String>();
        for (String name : props.stringPropertyNames()) {
            if (name.startsWith(prefix)) {
                out.add(name.substring(prefix.length()));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ 写

    /**
     * 写入若干键（null 值 = 删除该键）。用于面板里切换每把枪的开关。
     * 会保留文件里其它所有键。
     */
    public static synchronized void putAll(Properties changes) {
        ensureLoaded();
        Properties p = new Properties();
        p.putAll(props);
        if (changes != null) {
            for (String name : changes.stringPropertyNames()) {
                String v = changes.getProperty(name);
                if (v == null) {
                    p.remove(name);
                } else {
                    p.setProperty(name, v);
                }
            }
        }
        write(p);
        props = p;
    }

    public static void put(String key, String value) {
        Properties p = new Properties();
        p.setProperty(key, value);
        putAll(p);
    }

    public static synchronized void removeKeysWithPrefix(String prefix) {
        ensureLoaded();
        Properties p = new Properties();
        p.putAll(props);
        for (String name : new ArrayList<String>(p.stringPropertyNames())) {
            if (name.startsWith(prefix)) {
                p.remove(name);
            }
        }
        write(p);
        props = p;
    }

    private static void write(Properties p) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Optimization for Tacz —— 合并版配置\n");
        sb.append("# 弹孔 / 秒杀 / 混合开镜 / 开火自动开镜 / 命中标记 / 腰射准星 都在这个文件里。\n");
        sb.append("# 保存后 1 秒内热重载，不用重启游戏。\n");
        sb.append("# 开火自动开镜的每把枪开关也可以游戏内按 O 打开面板调。\n");
        sb.append("\n");
        List<String> keys = new ArrayList<String>(p.stringPropertyNames());
        Collections.sort(keys);
        String lastSection = null;
        for (String k : keys) {
            int dot = k.indexOf('.');
            String section = dot > 0 ? k.substring(0, dot) : "";
            if (!section.equals(lastSection)) {
                sb.append("\n# ---------- ").append(sectionTitle(section)).append(" ----------\n");
                lastSection = section;
            }
            sb.append(escapeKey(k)).append('=').append(escapeValue(p.getProperty(k))).append('\n');
        }
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) {
                out.write(sb.toString().getBytes("UTF-8"));
            }
            lastModified = Files.getLastModifiedTime(FILE).toMillis();
        } catch (IOException ignored) {
            // 写不进去也不影响本次生效
        }
    }

    /**
     * 转义键名。{@code Properties} 把 {@code = :} 和空白当分隔符，
     * 而枪械 id 里带冒号（如 {@code tacz:ai_awp}），不转义会被写坏。
     */
    private static String escapeKey(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '=' || c == ':' || c == '#' || c == '!' || c == ' ' || c == '\t' || c == '\\') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /** 转义值：只处理开头的空白和反斜杠/换行 */
    private static String escapeValue(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 4);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (i == 0 && (c == ' ' || c == '\t')) {
                sb.append('\\').append(c);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String sectionTitle(String section) {
        if ("hole".equals(section)) {
            return "自定义弹孔";
        }
        if ("oneshot".equals(section)) {
            return "指定枪械秒杀";
        }
        if ("hybridaim".equals(section)) {
            return "混合开镜（短按切换 / 长按按住）";
        }
        if ("adsfire".equals(section)) {
            return "开火自动开镜（先开镜后开火）";
        }
        if ("hit".equals(section)) {
            return "命中标记（X / 爆头双 X）";
        }
        if ("crosshair".equals(section)) {
            return "腰射准星（三角洲样式空心十字）";
        }
        return section.isEmpty() ? "其它" : section;
    }

    // ------------------------------------------------------------------ 旧配置迁移

    /** 旧文件 -> 新前缀 */
    private static final String[][] LEGACY = {
            { "taczbullethole.properties", "hole." },
            { "taczoneshot.properties", "oneshot." },
            { "taczhybridaim.properties", "hybridaim." },
            { "taczadsfire.properties", "adsfire." },
    };

    private static void migrateLegacy() {
        Properties merged = new Properties();
        boolean any = false;
        for (String[] pair : LEGACY) {
            Path old = legacyFile(pair[0]);
            if (!Files.isRegularFile(old)) {
                continue;
            }
            // 按原文解析，而不是 Properties.load：
            // gun.<id>=true 里的冒号会被 Properties 当分隔符，导致同命名空间的枪互相覆盖。
            Map<String, String> raw = parseRaw(old);
            if (raw.isEmpty()) {
                continue;
            }
            for (Map.Entry<String, String> e : raw.entrySet()) {
                merged.setProperty(pair[1] + e.getKey(), e.getValue());
            }
            any = true;
            // 迁移完改名，保留一份备份，避免以后重复迁移
            try {
                Files.move(old, legacyFile(pair[0] + ".migrated"),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // 改名失败也无所谓，下次启动会再迁一次（值相同，幂等）
            }
        }

        Properties existing = new Properties();
        if (Files.isRegularFile(FILE)) {
            try (InputStream in = Files.newInputStream(FILE)) {
                existing.load(in);
            } catch (IOException ignored) {
                // 忽略
            }
        }
        // 已有配置优先（不覆盖玩家后来在新文件里改的值）
        for (String name : existing.stringPropertyNames()) {
            merged.setProperty(name, existing.getProperty(name));
        }

        // 新文件不存在 且 有旧文件 -> 用迁移结果；否则用默认模板
        Properties out = (any || !existing.isEmpty()) ? merged : defaults();
        // 补齐默认里缺失的键
        Properties def = defaults();
        for (String name : def.stringPropertyNames()) {
            if (!out.containsKey(name)) {
                out.setProperty(name, def.getProperty(name));
            }
        }
        // 丢掉废弃的键，免得玩家看着还以为有那个功能
        for (String dead : DEPRECATED_KEYS) {
            out.remove(dead);
        }
        // 把这个功能的旧默认值升级到新默认值
        for (String[] pair : RENAMED_DEFAULT) {
            String cur = out.getProperty(pair[0]);
            if (pair[1].equals(cur)) {
                out.setProperty(pair[0], pair[2]);
            }
        }
        write(out);
    }

    /**
     * 默认值变过、需要一次性推给老配置文件的键。
     *
     * <p>格式：{@code {键, 旧默认值, 新默认值}} —— 只有当前值**正好等于某个旧默认值**
     * 时才改。这样玩家的自定义值不会被覆盖，而"从没动过"的键会自动跟上新默认。
     *
     * <p>同一个键可以有多条规则（值经过几轮默认值变迁时），
     * 所以每条是独立判断的，不 break。
     *
     * <p>为什么需要：{@code migrateLegacy()} 的规则是"已有配置优先"，
     * 所以配置文件里已经存在的键**永远不会**被新的默认值更新。
     * 命中标记就踩了这个 —— 默认改了之后老配置里还是旧值，看着像没修：
     * <pre>
     *   hit.offset_y    14 / -18 / -6 / 0 / 10 / 5 -> 3   历次默认都偏，逐次小步试到往下 3
     *   hit.duration_ms        700 -> 450    700ms 太长，屏幕上一直挂着标记
     * </pre>
     */
    private static final String[][] RENAMED_DEFAULT = {
            { "hit.offset_y", "14", "3" },
            { "hit.offset_y", "-6", "3" },
            { "hit.offset_y", "-18", "3" },
            { "hit.offset_y", "0", "3" },
            { "hit.offset_y", "10", "3" },
            { "hit.duration_ms", "700", "450" },
            // 腰射准星一开始默认关闭（怕擅自改观感），但用户要的就是它，改成默认开启
            { "crosshair.enabled", "false", "true" },
            // 青绿 + 线宽 2 太粗太显眼，改成白色细线
            { "crosshair.color", "33FF66", "FFFFFF" },
            { "crosshair.thickness", "2", "1" },
            // 间距公式改成"10 - 腰射精度"（= 直接用扩散值），倍率从 0.8 回到 1.0
            { "crosshair.gap_multiplier", "0.8", "1.0" },
            // 实测：弹孔离准星中心的距离约为准星的 2.3 倍
            { "crosshair.spread_scale", "1.0", "2.5" },
            { "crosshair.spread_scale", "2.3", "2.5" },
    };

    /**
     * 按原文逐行解析一个 properties 文件，绕开 {@link Properties} 的分隔符规则。
     *
     * <p>为什么要自己解析：{@code java.util.Properties} 把 {@code :} 和 {@code =} 都当分隔符，
     * 而枪械 id 里带冒号（{@code tacz:ai_awp}），于是
     * {@code gun.tacz:aa12=true} 会被解析成 key={@code gun.tacz}、value={@code aa12=true}，
     * 同一命名空间的枪全部互相覆盖 —— 表现就是"每把枪的开关只剩最后一条"。
     *
     * <p>这里把第一个 {@code =} 当作键值分隔符，并且：
     * <ul>
     *   <li>键以 {@code gun.} 开头时，整段（含冒号）都算键名；</li>
     *   <li>其它键若已经带反斜杠转义，先还原成字面量（后续统一由 write() 转义）。</li>
     * </ul>
     */
    private static Map<String, String> parseRaw(Path file) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        List<String> lines;
        try {
            lines = Files.readAllLines(file, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            return out;
        }
        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#") || t.startsWith("!")) {
                continue;
            }
            int eq = t.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = t.substring(0, eq);
            String value = t.substring(eq + 1).trim();
            if (!key.startsWith("gun.")) {
                key = key.replace("\\:", ":").replace("\\=", "=");
            }
            out.put(key, value);
        }
        return out;
    }

    // ------------------------------------------------------------------ 默认模板

    private static Properties defaults() {
        Properties p = new Properties();
        // ---- 弹孔 ----
        p.setProperty("hole.enabled", "true");
        p.setProperty("hole.guns", "auto");
        p.setProperty("hole.logo_hold", "20");
        p.setProperty("hole.logo_fade", "20");
        p.setProperty("hole.logo_lift", "0.03");
        p.setProperty("hole.scale", "1.0");
        p.setProperty("hole.alpha", "1.0");
        p.setProperty("hole.flip_u", "false");
        p.setProperty("hole.flip_v", "true");
        p.setProperty("hole.hole_scale", "0.6");
        p.setProperty("hole.hole_scale_guns", "*");
        p.setProperty("hole.texture", "taczbullethole:block/bullet_hole");
        // ---- 秒杀 ----
        p.setProperty("oneshot.enabled", "true");
        p.setProperty("oneshot.guns", "tacz:ai_awp");
        p.setProperty("oneshot.affect_players", "false");
        p.setProperty("oneshot.damage", "100000.0");
        p.setProperty("oneshot.force_kill", "true");
        // ---- 混合开镜 ----
        p.setProperty("hybridaim.enabled", "true");
        p.setProperty("hybridaim.short_press_ms", "250");
        p.setProperty("hybridaim.guns", "*");
        // ---- 开火自动开镜 ----
        p.setProperty("adsfire.enabled", "true");
        p.setProperty("adsfire.default", "false");
        p.setProperty("adsfire.aim_wait_ticks", "4");
        p.setProperty("adsfire.hold_ticks", "10");
        p.setProperty("adsfire.release_when_stop", "true");
        p.setProperty("adsfire.debug_log", "false");
        // ---- 命中标记 ----
        p.setProperty("hit.enabled", "true");
        p.setProperty("hit.duration_ms", "450");
        p.setProperty("hit.scale", "1.0");
        p.setProperty("hit.sound", "true");
        p.setProperty("hit.shake", "true");
        p.setProperty("hit.offset_x", "0");
        p.setProperty("hit.offset_y", "3");
        p.setProperty("hit.debug_log", "false");
        // ---- 腰射准星（三角洲样式）----
        p.setProperty("crosshair.enabled", "true");
        p.setProperty("crosshair.color", "FFFFFF");
        p.setProperty("crosshair.thickness", "1");
        p.setProperty("crosshair.arm", "5");
        p.setProperty("crosshair.gap", "8");
        p.setProperty("crosshair.dynamic", "true");
        p.setProperty("crosshair.gap_multiplier", "1.0");
        p.setProperty("crosshair.gap_max", "60");
        p.setProperty("crosshair.dot", "1");
        p.setProperty("crosshair.hide_on_aim", "true");
        p.setProperty("crosshair.only_guns", "true");
        p.setProperty("crosshair.spread_scale", "2.5");
        p.setProperty("crosshair.debug_log", "false");
        return p;
    }

    /**
     * 已经废弃的键 —— 读配置时直接丢掉，别再写回文件。
     *
     * <p>第一版命中反馈是"显示伤害数字 + 护甲削减差额"，需要
     * {@code hit.show_armor}，而且伤害数字和 GD656击杀图标 模组叠在一起看不清，
     * 所以改成了命中标记（X / 双 X），这些键就没用了。
     *
     * <p>{@code hit.headshot_size} / {@code hit.headshot_spacing} 是爆头"双 X"
     * 时代的产物（爆头画两个上下叠的 X，还得把 X 缩小才读得出来）。
     * 实测两个 X 在屏幕尺寸下糊成一团，已改成<b>橙色单 X</b>，
     * 尺寸和间距都不再需要 —— 这两个键留着只会让人以为还能调。
     *
     * <p>不清理的话它们会一直留在玩家的配置文件里，看着像是还有这个功能。
     */
    private static final String[] DEPRECATED_KEYS = {
            "hit.show_armor",
            // 改名了：size_scale -> spread_scale（语义也从"整体缩放"改成"只缩放扩散值"）
            "crosshair.size_scale",
            // 爆头改成橙色单 X，不再有独立的爆头尺寸 / 双 X 间距
            "hit.headshot_size",
            "hit.headshot_spacing",
    };
}
