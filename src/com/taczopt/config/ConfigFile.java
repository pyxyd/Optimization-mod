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

public final class ConfigFile {

    private static final Path FILE = resolveFile();

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

        }
    }

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

            Map<String, String> raw = parseRaw(old);
            if (raw.isEmpty()) {
                continue;
            }
            for (Map.Entry<String, String> e : raw.entrySet()) {
                merged.setProperty(pair[1] + e.getKey(), e.getValue());
            }
            any = true;

            try {
                Files.move(old, legacyFile(pair[0] + ".migrated"),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {

            }
        }

        Properties existing = new Properties();
        if (Files.isRegularFile(FILE)) {
            try (InputStream in = Files.newInputStream(FILE)) {
                existing.load(in);
            } catch (IOException ignored) {

            }
        }

        for (String name : existing.stringPropertyNames()) {
            merged.setProperty(name, existing.getProperty(name));
        }

        Properties out = (any || !existing.isEmpty()) ? merged : defaults();

        Properties def = defaults();
        for (String name : def.stringPropertyNames()) {
            if (!out.containsKey(name)) {
                out.setProperty(name, def.getProperty(name));
            }
        }

        for (String dead : DEPRECATED_KEYS) {
            out.remove(dead);
        }

        for (String[] pair : RENAMED_DEFAULT) {
            String cur = out.getProperty(pair[0]);
            if (pair[1].equals(cur)) {
                out.setProperty(pair[0], pair[2]);
            }
        }
        write(out);
    }

    private static final String[][] RENAMED_DEFAULT = {
            { "hit.offset_y", "14", "3" },
            { "hit.offset_y", "-6", "3" },
            { "hit.offset_y", "-18", "3" },
            { "hit.offset_y", "0", "3" },
            { "hit.offset_y", "10", "3" },
            { "hit.duration_ms", "700", "450" },

            { "crosshair.enabled", "false", "true" },

            { "crosshair.color", "33FF66", "FFFFFF" },
            { "crosshair.thickness", "2", "1" },

            { "crosshair.gap_multiplier", "0.8", "1.0" },

            { "crosshair.spread_scale", "1.0", "2.5" },
            { "crosshair.spread_scale", "2.3", "2.5" },
    };

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

    private static Properties defaults() {
        Properties p = new Properties();

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

        p.setProperty("oneshot.enabled", "true");
        p.setProperty("oneshot.guns", "tacz:ai_awp");
        p.setProperty("oneshot.affect_players", "false");
        p.setProperty("oneshot.damage", "100000.0");
        p.setProperty("oneshot.force_kill", "true");

        p.setProperty("hybridaim.enabled", "true");
        p.setProperty("hybridaim.short_press_ms", "250");
        p.setProperty("hybridaim.guns", "*");

        p.setProperty("adsfire.enabled", "true");
        p.setProperty("adsfire.default", "false");
        p.setProperty("adsfire.aim_wait_ticks", "4");
        p.setProperty("adsfire.hold_ticks", "10");
        p.setProperty("adsfire.release_when_stop", "true");
        p.setProperty("adsfire.debug_log", "false");

        p.setProperty("hit.enabled", "true");
        p.setProperty("hit.duration_ms", "450");
        p.setProperty("hit.scale", "1.0");
        p.setProperty("hit.sound", "true");
        p.setProperty("hit.shake", "true");
        p.setProperty("hit.offset_x", "0");
        p.setProperty("hit.offset_y", "3");
        p.setProperty("hit.debug_log", "false");

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

    private static final String[] DEPRECATED_KEYS = {
            "hit.show_armor",

            "crosshair.size_scale",

            "hit.headshot_size",
            "hit.headshot_spacing",
    };
}
