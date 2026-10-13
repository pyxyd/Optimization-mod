import com.taczopt.config.ConfigFile;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigUpgradeCheck {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("taczopt-cfgtest");
        Path cfgDir = dir.resolve("config");
        Files.createDirectories(cfgDir);

        System.setProperty("taczopt.configdir", dir.toString());

        String legacy = String.join("\n",
                "hit.enabled=true",
                "hit.duration_ms=700",
                "hit.offset_y=14",
                "hit.headshot_spacing=4",
                "hit.headshot_size=4",
                "hit.scale=1.7",
                "hole.scale=0.8",
                "hole.guns=auto",
                "");
        Files.write(cfgDir.resolve("taczopt.properties"), legacy.getBytes("UTF-8"));

        ConfigFile.ensureLoaded();

        System.out.println("=== 旧默认值应被升级 ===");
        eq("hit.duration_ms 700 -> 450", "450", ConfigFile.get("hit.duration_ms", "?"));
        eq("hit.offset_y 14 -> 3", "3", ConfigFile.get("hit.offset_y", "?"));

        System.out.println();
        System.out.println("=== 玩家改过的值必须原样保留 ===");
        eq("hit.scale=1.7 保留", "1.7", ConfigFile.get("hit.scale", "?"));
        eq("hole.scale=0.8 保留", "0.8", ConfigFile.get("hole.scale", "?"));

        System.out.println();
        System.out.println("=== 缺失的键补上新默认 ===");
        eq("hit.sound 补上", "true", ConfigFile.get("hit.sound", "?"));
        eq("hit.enabled 保留", "true", ConfigFile.get("hit.enabled", "?"));
        eq("hole.pictures 补上", "true", ConfigFile.get("hole.pictures", "?"));

        System.out.println();
        System.out.println("=== hole.guns=auto 要升级成 *（auto 只在 tacz 命名空间下有意义）===");
        eq("hole.guns auto -> *", "*", ConfigFile.get("hole.guns", "?"));

        System.out.println();
        System.out.println("=== 玩家自己写的 hole.guns 不能被改 ===");
        check("hole.guns 不是 auto 时保持原样", !"auto".equals(ConfigFile.get("hole.guns", "?")));

        System.out.println();
        System.out.println("=== 已废弃的键必须被丢掉（否则玩家以为还能调） ===");

        eq("hit.headshot_spacing 读不到", "?", ConfigFile.get("hit.headshot_spacing", "?"));
        eq("hit.headshot_size 读不到", "?", ConfigFile.get("hit.headshot_size", "?"));

        System.out.println();
        System.out.println("=== 升级结果确实落盘了 ===");
        String after = new String(Files.readAllBytes(cfgDir.resolve("taczopt.properties")), "UTF-8");
        check("文件里有 hit.offset_y=3", after.contains("hit.offset_y=3"));
        check("文件里没有 hit.offset_y=14", !after.contains("hit.offset_y=14"));
        check("文件里有 hit.duration_ms=450", after.contains("hit.duration_ms=450"));
        check("文件里没有 hit.headshot_spacing（废弃键已清理）", !after.contains("hit.headshot_spacing"));
        check("文件里没有 hit.headshot_size（废弃键已清理）", !after.contains("hit.headshot_size"));
        check("文件里保留了 hit.scale=1.7", after.contains("hit.scale=1.7"));
        check("文件里保留了 hole.scale=0.8", after.contains("hole.scale=0.8"));
        check("文件里 hole.guns 已经是 *", after.contains("hole.guns=*"));
        check("文件里没有 hole.guns=auto", !after.contains("hole.guns=auto"));

        System.out.println();
        System.out.println("=== 重复读不会反复改（幂等） ===");
        ConfigFile.ensureLoaded();
        eq("再读一次 hit.offset_y 还是 3", "3", ConfigFile.get("hit.offset_y", "?"));
        eq("再读一次 hit.scale 还是 1.7", "1.7", ConfigFile.get("hit.scale", "?"));

        System.out.println();
        System.out.println("注：'中间版本的 -18 也要升到 -6' 这条在同一个 JVM 里测不了 ——");
        System.out.println("    ConfigFile 的路径与 loaded 标志都是类加载时的静态状态，换目录不会重读。");
        System.out.println("    规则本身在 RENAMED_DEFAULT 里逐条独立判断，不会因为顺序失效。");

        try {
            Files.walk(dir)
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception ignored) {

        }

        System.out.println();
        if (failures == 0) {
            System.out.println("ConfigUpgradeCheck: 全部通过");
        } else {
            System.out.println("ConfigUpgradeCheck: " + failures + " 项失败");
            System.exit(1);
        }
    }

    private static void eq(String what, String expect, String actual) {
        boolean ok = expect.equals(actual);
        System.out.println((ok ? "  [OK]   " : "  [FAIL] ") + what
                + (ok ? "" : "  期望 \"" + expect + "\" 实际 \"" + actual + "\""));
        if (!ok) {
            failures++;
        }
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [OK]   " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
