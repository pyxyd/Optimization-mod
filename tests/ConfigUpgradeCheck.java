import com.taczopt.config.ConfigFile;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 离线校验：老配置文件里的"旧默认值"会被一次性升级到新默认值，废弃的键会被清理。
 *
 * <h2>为什么要测这个</h2>
 * {@code ConfigFile.migrateLegacy()} 的规则是"已有配置优先" ——
 * 只要键已经存在于玩家的配置文件里，就<b>永远不会</b>被新的默认值更新。
 * 所以每次改默认值都必须往 {@code RENAMED_DEFAULT} 里加一条，
 * 否则玩家那边还是旧值，看着就像"改了没用"。
 *
 * <p>命中标记就踩过：{@code hit.offset_y} 的默认改了之后，
 * 老配置里还是旧值，标记继续偏。
 *
 * <p>同理，删掉一个键时必须往 {@code DEPRECATED_KEYS} 里加，
 * 否则它会一直留在玩家的配置文件里，看着像是还有这个功能。
 * 爆头从"双 X"改成"橙色单 X"后就删了 {@code hit.headshot_size} 和
 * {@code hit.headshot_spacing}，这个校验器就是守住这条的。
 *
 * <h2>⚠ 为什么只用一个临时目录</h2>
 * {@code ConfigFile.FILE} 是 {@code static final}，在<b>类加载时</b>就按
 * {@code taczopt.configdir} 定死了，而且 {@code ensureLoaded()} 只跑一次迁移。
 * 所以同一个 JVM 里换目录再 {@code ensureLoaded()} 是没用的 ——
 * 第一版就这么写，结果第三步读到的是第一步那份配置（测试假失败）。
 *
 * <p>因此这里改成"<b>一份配置覆盖全部情况</b>"：
 * 旧默认值 + 玩家改过的值 + 完全缺失的键，全部塞进同一次加载里验证。
 * 想验"中间版本的默认值链"（-18 也要升到 -6）得单独起一个 JVM，
 * 规则本身在 RENAMED_DEFAULT 里是逐条独立判断的。
 *
 * <p>跑法（源码目录下）：
 * <pre>
 *   javac -encoding UTF-8 -cp classes -d check tools\ConfigUpgradeCheck.java
 *   java -cp "check;classes" ConfigUpgradeCheck
 * </pre>
 */
public final class ConfigUpgradeCheck {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("taczopt-cfgtest");
        Path cfgDir = dir.resolve("config");
        Files.createDirectories(cfgDir);

        // ⚠ 必须在这之前设好系统属性：ConfigFile 的 FILE 是类加载时算的
        System.setProperty("taczopt.configdir", dir.toString());

        // 一份"老版本写出来的"配置：
        //   两个旧默认值 + 两个已废弃的键 + 一个玩家改过的值 + 一个别的功能的键
        //   （hit.sound 故意不写，用来验"完全缺失的键会补上新默认"）
        String legacy = String.join("\n",
                "hit.enabled=true",
                "hit.duration_ms=700",          // 旧默认 -> 应升级为 450
                "hit.offset_y=14",              // 旧默认 -> 应升级为 3
                "hit.headshot_spacing=4",       // 已废弃 -> 应被删掉（爆头改成橙色单 X 了）
                "hit.headshot_size=4",          // 已废弃 -> 应被删掉
                "hit.scale=1.7",                // 玩家改过 -> 必须保留
                "hole.scale=0.8",               // 别的功能的键 -> 不动
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

        System.out.println();
        System.out.println("=== 已废弃的键必须被丢掉（否则玩家以为还能调） ===");
        // 爆头从"双 X"改成"橙色单 X"之后，爆头尺寸和双 X 间距这两个键就没有意义了
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
            // 清理失败无所谓，系统临时目录
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
