import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Build-time check: parse mods.toml with the same TOML library Forge uses (night-config).
 *
 * <p>为什么需要它：TOML 多行字符串是三个单引号，写成四个（''''）不是 Java 编译错误，
 * 用"数引号个数"这种土办法也查不出来，但 Forge 会在启动最早期直接抛
 * "contains a corrupt or misconfigured toml"，整个游戏起不来。所以这里用真解析器验一遍。
 */
public class ModsTomlCheck {
    public static void main(String[] args) {
        try {
            CommentedConfig root = new TomlParser().parse(
                    Files.newBufferedReader(Paths.get(args[0]), StandardCharsets.UTF_8));

            Object modsObj = root.get("mods");
            if (!(modsObj instanceof List) || ((List<?>) modsObj).isEmpty()) {
                System.out.println("FAIL: mods.toml has no [[mods]] section");
                System.exit(2);
                return;
            }
            CommentedConfig mod = (CommentedConfig) ((List<?>) modsObj).get(0);
            String modId = (String) mod.get("modId");
            String version = (String) mod.get("version");
            String displayName = (String) mod.get("displayName");
            if (modId == null || modId.isEmpty()) {
                System.out.println("FAIL: [[mods]] has no modId");
                System.exit(2);
                return;
            }
            if (version == null || version.isEmpty()) {
                System.out.println("FAIL: [[mods]] has no version");
                System.exit(2);
                return;
            }
            Object loader = root.get("modLoader");
            Object deps = root.get("dependencies");
            System.out.println("TOML ok: modLoader=" + loader + " modId=" + modId
                    + " version=" + version + " displayName=" + displayName
                    + " hasDependencies=" + (deps != null));
            System.exit(0);
        } catch (Throwable t) {
            System.out.println("FAIL: mods.toml is not valid TOML -> " + t);
            System.exit(1);
        }
    }
}
