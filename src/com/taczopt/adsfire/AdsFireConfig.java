package com.taczopt.adsfire;

import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.taczopt.config.ConfigFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * 开火自动开镜的配置 + 每把枪的开关。键前缀 {@code adsfire.}，
 * 存在合并版配置 config/taczopt.properties 里。
 *
 * <pre>
 * adsfire.enabled=true
 * adsfire.default=false                # 没单独设置过的枪，默认是否开启
 * adsfire.aim_wait_ticks=4
 * adsfire.hold_ticks=10
 * adsfire.release_when_stop=true
 * adsfire.debug_log=false
 * adsfire.gun.tacz:ai_awp=true         # 单独某把枪
 * </pre>
 *
 * <p>游戏内按 O 打开枪械面板就能逐把切换，写入的就是这些键。
 */
public final class AdsFireConfig {

    private static final String P = "adsfire.";
    /** 单枪设置在这个前缀之后（gun. 是为了兼容旧文件里的键名） */
    private static final String GUN_PREFIX = P + "gun.";

    private static volatile boolean enabled = true;
    /** 开镜请求发出后至少等多少 tick 再开火 */
    private static volatile int aimWaitTicks = 4;
    /** 松开左键后保持开镜多少 tick（20 tick = 1 秒） */
    private static volatile int holdTicks = 10;
    /** 松手后是否自动收镜 */
    private static volatile boolean releaseWhenStop = true;
    /** 没有单独配置过的枪，默认是否启用 */
    private static volatile boolean defaultOn = false;
    /** 排错用：开火拦截时打日志 */
    private static volatile boolean debugLog = false;

    private static final Map<String, Boolean> PER_GUN = new HashMap<String, Boolean>();

    private AdsFireConfig() {
    }

    // ------------------------------------------------------------------ 读取

    public static void ensureLoaded() {
        ConfigFile.ensureLoaded();
        synchronized (AdsFireConfig.class) {
            load();
        }
    }

    public static void reloadIfChanged() {
        ConfigFile.reloadIfChanged();
        synchronized (AdsFireConfig.class) {
            load();
        }
    }

    private static void load() {
        enabled = ConfigFile.getBool(P + "enabled", true);
        releaseWhenStop = ConfigFile.getBool(P + "release_when_stop", true);
        defaultOn = ConfigFile.getBool(P + "default", false);
        debugLog = ConfigFile.getBool(P + "debug_log", false);
        aimWaitTicks = clamp(ConfigFile.getInt(P + "aim_wait_ticks", 4), 0, 60);
        holdTicks = clamp(ConfigFile.getInt(P + "hold_ticks", 10), 0, 200);
        synchronized (PER_GUN) {
            PER_GUN.clear();
            for (String id : ConfigFile.keysWithPrefix(GUN_PREFIX)) {
                String key = id.trim().toLowerCase();
                if (!key.isEmpty()) {
                    PER_GUN.put(key, ConfigFile.getBool(GUN_PREFIX + id, true));
                }
            }
        }
    }

    // ------------------------------------------------------------------ 查询

    public static boolean enabled() {
        ensureLoaded();
        return enabled;
    }

    /** 这把枪是否启用了「开火先开镜」 */
    public static boolean isOn(String gunId) {
        ensureLoaded();
        if (!enabled || gunId == null) {
            return false;
        }
        Boolean v;
        synchronized (PER_GUN) {
            v = PER_GUN.get(gunId.toLowerCase());
        }
        return v == null ? defaultOn : v.booleanValue();
    }

    public static void setOn(String gunId, boolean on) {
        ensureLoaded();
        if (gunId == null) {
            return;
        }
        String key = gunId.toLowerCase();
        synchronized (PER_GUN) {
            PER_GUN.put(key, Boolean.valueOf(on));
        }
        Properties p = new Properties();
        p.setProperty(GUN_PREFIX + key, Boolean.toString(on));
        ConfigFile.putAll(p);
    }

    public static void toggle(String gunId) {
        setOn(gunId, !isOn(gunId));
    }

    public static boolean isExplicitlySet(String gunId) {
        ensureLoaded();
        synchronized (PER_GUN) {
            return PER_GUN.containsKey(gunId.toLowerCase());
        }
    }

    public static boolean defaultOn() {
        ensureLoaded();
        return defaultOn;
    }

    public static void setDefaultOn(boolean on) {
        ensureLoaded();
        defaultOn = on;
        ConfigFile.put(P + "default", Boolean.toString(on));
    }

    public static void clearAll() {
        ensureLoaded();
        synchronized (PER_GUN) {
            PER_GUN.clear();
        }
        ConfigFile.removeKeysWithPrefix(GUN_PREFIX);
    }

    public static int aimWaitTicks() {
        ensureLoaded();
        return aimWaitTicks;
    }

    public static int holdTicks() {
        ensureLoaded();
        return holdTicks;
    }

    public static boolean releaseWhenStop() {
        ensureLoaded();
        return releaseWhenStop;
    }

    public static boolean debugLog() {
        ensureLoaded();
        return debugLog;
    }

    // ------------------------------------------------------------------ 枪械列表

    /** 面板里的一行：一把枪 */
    public static final class GunEntry {
        public final String id;
        public final String name;
        public final String type;
        /**
         * 显示名重复时用来区分的后缀，例如 {@code "foot_stool"}。
         * 枪包里有同名的不同枪（三角洲包 m14_short 和 m14_short_foot_stool 都叫 "Mk14"），
         * 不区分的话面板里看着像重复项。没重名时是空串。
         */
        public final String suffix;

        GunEntry(String id, String name, String type, String suffix) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.suffix = suffix == null ? "" : suffix;
        }

        /** 列表里显示的名字（重名的带上 id 里能区分的部分） */
        public String displayName() {
            return suffix.isEmpty() ? name : name + " [" + suffix + "]";
        }
    }

    private static List<GunEntry> cachedList = null;

    /** 列出当前装的所有枪（客户端索引）。结果会被缓存。 */
    public static List<GunEntry> listGuns() {
        if (cachedList != null) {
            return cachedList;
        }
        List<GunEntry> out = new ArrayList<GunEntry>();
        try {
            Set<Map.Entry<net.minecraft.resources.ResourceLocation, ClientGunIndex>> all =
                    com.tacz.guns.api.TimelessAPI.getAllClientGunIndex();
            for (Map.Entry<net.minecraft.resources.ResourceLocation, ClientGunIndex> e : all) {
                ClientGunIndex idx = e.getValue();
                String id = e.getKey().toString();
                String raw = idx == null ? null : idx.getName();
                String name = id;
                if (raw != null && !raw.isEmpty()) {
                    try {
                        String translated = I18n.m_118938_(raw, new Object[0]);
                        name = (translated != null && !translated.isEmpty()) ? translated : raw;
                    } catch (RuntimeException ex) {
                        name = raw;
                    }
                }
                String type = idx == null ? "" : idx.getType();
                out.add(new GunEntry(id, name == null ? id : name, type == null ? "" : type, null));
            }
        } catch (RuntimeException ignored) {
            // TaCZ 还没加载完 -> 返回空表
        }
        markDuplicateNames(out);
        Collections.sort(out, new Comparator<GunEntry>() {
            @Override
            public int compare(GunEntry a, GunEntry b) {
                int c = a.name.compareToIgnoreCase(b.name);
                return c != 0 ? c : a.id.compareToIgnoreCase(b.id);
            }
        });
        cachedList = out;
        return out;
    }

    /**
     * 给显示名重复的枪补一个区分后缀。
     *
     * <p>实例：三角洲枪包里 {@code wemql_r:m14_short} 和 {@code wemql_r:m14_short_foot_stool}
     * 的显示名都是 "Mk14"；{@code m14_long} 和 {@code m14_long_foot_stool} 都叫 "M14 EBR"。
     * 它们是不同的枪（一个有脚架一个没有），但不区分的话面板里就是两条一模一样的条目。
     *
     * <p>后缀取法：把同组 id 的公共前缀去掉，剩下的部分。例如 m14_short / m14_short_foot_stool
     * 的公共前缀是 "m14_short"，于是两个后缀分别是 "" 和 "foot_stool"；
     * 空的那个说明它本来就是"基础款"，用完整 id 兜底以免显示成空括号。
     */
    private static void markDuplicateNames(List<GunEntry> list) {
        Map<String, Integer> count = new HashMap<String, Integer>();
        for (GunEntry e : list) {
            Integer c = count.get(e.name);
            count.put(e.name, Integer.valueOf(c == null ? 1 : c.intValue() + 1));
        }
        Map<String, List<GunEntry>> byName = new HashMap<String, List<GunEntry>>();
        for (GunEntry e : list) {
            if (count.get(e.name).intValue() < 2) {
                continue;
            }
            List<GunEntry> group = byName.get(e.name);
            if (group == null) {
                group = new ArrayList<GunEntry>();
                byName.put(e.name, group);
            }
            group.add(e);
        }
        for (Map.Entry<String, List<GunEntry>> en : byName.entrySet()) {
            List<GunEntry> group = en.getValue();
            String prefix = null;
            for (GunEntry e : group) {
                String path = pathOf(e.id);
                if (prefix == null) {
                    prefix = path;
                } else {
                    int i = 0;
                    while (i < prefix.length() && i < path.length() && prefix.charAt(i) == path.charAt(i)) {
                        i++;
                    }
                    prefix = prefix.substring(0, i);
                }
            }
            for (int k = 0; k < list.size(); k++) {
                GunEntry e = list.get(k);
                if (!en.getKey().equals(e.name)) {
                    continue;
                }
                String path = pathOf(e.id);
                String rest = (prefix != null && path.startsWith(prefix)) ? path.substring(prefix.length()) : path;
                rest = rest.replaceAll("^[_-]+", "").replaceAll("[_-]+$", "");
                String suffix = rest.isEmpty() ? "base" : rest;
                list.set(k, new GunEntry(e.id, e.name, e.type, suffix));
            }
        }
    }

    private static String pathOf(String gunId) {
        int i = gunId.indexOf(':');
        return i >= 0 ? gunId.substring(i + 1) : gunId;
    }

    public static void invalidateGunList() {
        cachedList = null;
    }

    public static boolean inGame() {
        Minecraft mc = Minecraft.m_91087_();
        return mc != null && mc.f_91074_ != null;
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
