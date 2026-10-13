package com.taczopt.bullethole;

import com.taczopt.config.ConfigFile;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class TBHConfig {

    private static final String P = "hole.";

    @SuppressWarnings({"deprecation", "removal"})
    private static final ResourceLocation FALLBACK =
            new ResourceLocation("taczbullethole", "block/bullet_hole");

    public static boolean enabled = true;

    public static float scale = 1.0F;

    public static float holeScale = 0.6F;

    public static float logoLift = 0.03F;

    public static float alpha = 1.0F;

    public static int logoHold = 20;

    public static int logoFade = 20;

    public static boolean flipU = false;

    public static boolean flipV = true;

    private static volatile boolean loaded = false;
    private static volatile ResourceLocation textureId = FALLBACK;
    private static volatile boolean pictures = true;

    private static final ListMatcher LOGO = new ListMatcher();
    private static final ListMatcher HOLE = new ListMatcher();

    private static String lastLogoRaw = "*";
    private static String lastHoleRaw = "*";
    private static boolean lastPictures = true;
    private static String lastSignature = null;

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

    public static int logoTicks() {
        ensureLoaded();
        return Math.max(logoHold, 0) + Math.max(logoFade, 0);
    }

    public static void reloadIfChanged() {
        ensureLoaded();
        ConfigFile.reloadIfChanged();
        String sig = signature();
        if (lastSignature == null || !sig.equals(lastSignature)) {
            load();
        }
    }

    private static String signature() {
        StringBuilder sb = new StringBuilder(96);
        sb.append(ConfigFile.getBool(P + "enabled", true)).append('|')
          .append(ConfigFile.get(P + "guns", "*")).append('|')
          .append(ConfigFile.get(P + "texture", "taczbullethole:block/bullet_hole")).append('|')
          .append(ConfigFile.getDouble(P + "scale", 1.0D)).append('|')
          .append(ConfigFile.getDouble(P + "hole_scale", 0.6D)).append('|')
          .append(ConfigFile.get(P + "hole_scale_guns", "*")).append('|')
          .append(ConfigFile.getDouble(P + "logo_lift", 0.03D)).append('|')
          .append(ConfigFile.getDouble(P + "alpha", 1.0D)).append('|')
          .append(ConfigFile.getInt(P + "logo_hold", 20)).append('|')
          .append(ConfigFile.getInt(P + "logo_fade", 20)).append('|')
          .append(ConfigFile.getBool(P + "flip_u", false)).append('|')
          .append(ConfigFile.getBool(P + "flip_v", true)).append('|')
          .append(ConfigFile.getBool(P + "pictures", true));
        return sb.toString();
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
        lastLogoRaw = ConfigFile.get(P + "guns", "*");
        lastHoleRaw = ConfigFile.get(P + "hole_scale_guns", "*");
        lastPictures = ConfigFile.getBool(P + "pictures", true);
        pictures = lastPictures;
        lastSignature = signature();
        LOGO.parse(lastLogoRaw);
        HOLE.parse(lastHoleRaw);
    }

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

    public static boolean applies(String gunId) {
        ensureLoaded();
        if (!enabled || !pictures || isTacz(gunId)) {
            return false;
        }
        return LOGO.matches(gunId);
    }

    public static boolean holeApplies(String gunId) {
        ensureLoaded();
        if (!enabled || !pictures || isTacz(gunId)) {
            return false;
        }
        return HOLE.matches(gunId);
    }

    private static boolean isTacz(String gunId) {
        if (gunId == null || gunId.isEmpty()) {
            return true;
        }
        int colon = gunId.indexOf(':');
        String namespace = colon > 0 ? gunId.substring(0, colon) : "tacz";
        return "tacz".equalsIgnoreCase(namespace);
    }

    public static String gunsRaw() {
        ensureLoaded();
        return LOGO.raw;
    }

    public static String holeGunsRaw() {
        ensureLoaded();
        return HOLE.raw;
    }

    public static ResourceLocation texture() {
        ensureLoaded();
        return pictures ? textureId : FALLBACK;
    }

    public static boolean pictures() {
        ensureLoaded();
        return pictures;
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

    @SuppressWarnings({"deprecation", "removal"})
    private static ResourceLocation build(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }
}
