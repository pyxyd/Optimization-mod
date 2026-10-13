package com.taczopt.hybridaim;

import com.taczopt.config.ConfigFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class HybridAimConfig {

    private static final String P = "hybridaim.";

    public static boolean enabled = true;
    public static int shortPressMs = 250;

    private static final Set<String> EXACT = new HashSet<String>();
    private static final List<Pattern> PATTERNS = new ArrayList<Pattern>();
    private static boolean allGuns = true;
    private static String gunsRaw = "*";
    private static String lastGunsRaw = null;

    private static volatile boolean loaded = false;

    private HybridAimConfig() {
    }

    public static void ensureLoaded() {
        ConfigFile.ensureLoaded();
        if (loaded) {
            return;
        }
        synchronized (HybridAimConfig.class) {
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
        String guns = ConfigFile.get(P + "guns", "*");
        if (!guns.equals(lastGunsRaw)) {
            load();
        }
    }

    private static void load() {
        enabled = ConfigFile.getBool(P + "enabled", true);
        shortPressMs = ConfigFile.getInt(P + "short_press_ms", 250);

        gunsRaw = ConfigFile.get(P + "guns", "*");
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
