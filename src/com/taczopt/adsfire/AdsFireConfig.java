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

public final class AdsFireConfig {

    private static final String P = "adsfire.";

    private static final String GUN_PREFIX = P + "gun.";

    private static volatile boolean enabled = true;

    private static volatile int aimWaitTicks = 4;

    private static volatile int holdTicks = 10;

    private static volatile boolean releaseWhenStop = true;

    private static volatile boolean defaultOn = false;

    private static volatile boolean debugLog = false;

    private static final Map<String, Boolean> PER_GUN = new HashMap<String, Boolean>();

    private AdsFireConfig() {
    }

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

    public static boolean enabled() {
        ensureLoaded();
        return enabled;
    }

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

    public static final class GunEntry {
        public final String id;
        public final String name;
        public final String type;

        public final String suffix;

        GunEntry(String id, String name, String type, String suffix) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.suffix = suffix == null ? "" : suffix;
        }

        public String displayName() {
            return suffix.isEmpty() ? name : name + " [" + suffix + "]";
        }
    }

    private static List<GunEntry> cachedList = null;

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
