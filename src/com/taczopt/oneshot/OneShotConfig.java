package com.taczopt.oneshot;

import com.taczopt.config.ConfigFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 指定枪械秒杀的配置。键前缀 {@code oneshot.}，存在合并版配置 config/taczopt.properties 里。
 * guns 支持：* 全部枪 / 精确 id / 通配符（* 与 ?），逗号分隔。热重载由 {@link ConfigFile} 统一负责。
 */
public final class OneShotConfig {

    private static final String P = "oneshot.";

    public static boolean enabled = true;
    public static boolean affectPlayers = false;
    public static boolean forceKill = true;
    public static float damage = 100000.0F;

    private static final Set<String> EXACT = new HashSet<String>();
    private static final List<Pattern> PATTERNS = new ArrayList<Pattern>();
    private static boolean allGuns = false;
    private static String gunsRaw = "tacz:ai_awp";
    private static String lastGunsRaw = null;

    private static volatile boolean loaded = false;

    private OneShotConfig() {
    }

    public static void ensureLoaded() {
        ConfigFile.ensureLoaded();
        if (loaded) {
            return;
        }
        synchronized (OneShotConfig.class) {
            if (loaded) {
                return;
            }
            load();
            loaded = true;
        }
    }

    public static void reloadIfChanged() {
        ensureLoaded();
        ConfigFile.reloadIfChanged();
        String guns = ConfigFile.get(P + "guns", "tacz:ai_awp");
        if (!guns.equals(lastGunsRaw)) {
            load();
        }
    }

    private static void load() {
        enabled = ConfigFile.getBool(P + "enabled", true);
        affectPlayers = ConfigFile.getBool(P + "affect_players", false);
        forceKill = ConfigFile.getBool(P + "force_kill", true);
        damage = (float) ConfigFile.getDouble(P + "damage", 100000.0D);

        gunsRaw = ConfigFile.get(P + "guns", "tacz:ai_awp");
        lastGunsRaw = gunsRaw;
        EXACT.clear();
        PATTERNS.clear();
        allGuns = gunsRaw.isEmpty() || "*".equals(gunsRaw);
        if (!allGuns) {
            for (String part : gunsRaw.split(",")) {
                String id = part.trim();
                if (id.isEmpty()) {
                    continue;
                }
                if (id.indexOf('*') >= 0 || id.indexOf('?') >= 0) {
                    PATTERNS.add(toPattern(id));
                } else {
                    EXACT.add(id.toLowerCase());
                }
            }
        }
    }

    public static String gunsRaw() {
        ensureLoaded();
        return gunsRaw;
    }

    public static boolean applies(String gunId) {
        ensureLoaded();
        if (!enabled || gunId == null) {
            return false;
        }
        if (allGuns) {
            return true;
        }
        String lower = gunId.toLowerCase();
        if (EXACT.contains(lower)) {
            return true;
        }
        for (int i = 0; i < PATTERNS.size(); i++) {
            if (PATTERNS.get(i).matcher(lower).matches()) {
                return true;
            }
        }
        return false;
    }

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
}
