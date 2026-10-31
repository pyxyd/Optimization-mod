import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Verifies that every @Mixin class in src is registered by the mixin config that owns
 * its package.
 *
 * The mod now ships two configs (taczopt.mixins.json and taczfeel.mixins.json) under two
 * packages, so the check takes all of them and resolves each class against the config
 * whose "package" covers it. A class is only missing when no covering config lists it,
 * and a class whose package is covered by no config at all is reported separately --
 * that is the case that silently ships an unapplied mixin.
 */
public class MixinsCheck {

    private static final Pattern MIXIN_ANNOTATION = Pattern.compile("@Mixin\\s*\\(");
    private static final Pattern PACKAGE_DECL = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;");
    private static final Pattern CONFIG_PACKAGE = Pattern.compile("\"package\"\\s*:\\s*\"([\\w.$]+)\"");
    private static final Pattern STRING_LITERAL = Pattern.compile("\"([A-Za-z0-9_$]+)\"");

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("usage: MixinsCheck <srcRoot> <mixinConfig> [moreConfigs...]");
            System.exit(2);
            return;
        }
        Path srcRoot = Paths.get(args[0]);

        Map<String, Set<String>> declaredByPackage = new LinkedHashMap<>();
        List<String> configNames = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            Path jsonPath = Paths.get(args[i]);
            String json = new String(Files.readAllBytes(jsonPath), StandardCharsets.UTF_8);
            Matcher pm = CONFIG_PACKAGE.matcher(json);
            if (!pm.find()) {
                System.out.println("FAIL: " + jsonPath.getFileName() + " 缺少 package 字段");
                System.exit(2);
                return;
            }
            configNames.add(jsonPath.getFileName().toString());
            Set<String> declared = declaredByPackage.computeIfAbsent(pm.group(1), k -> new HashSet<>());
            Matcher m = STRING_LITERAL.matcher(json);
            while (m.find()) {
                declared.add(m.group(1));
            }
        }

        Map<String, Path> mixinClasses = new TreeMap<>();
        Files.walk(srcRoot).filter(p -> p.toString().endsWith(".java")).forEach(p -> {
            try {
                String text = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                if (!MIXIN_ANNOTATION.matcher(text).find()) {
                    return;
                }
                Matcher pm = PACKAGE_DECL.matcher(text);
                if (!pm.find()) {
                    return;
                }
                String pkg = pm.group(1);
                String simple = p.getFileName().toString().replace(".java", "");
                mixinClasses.put(pkg + "." + simple, p);
            } catch (IOException ignored) {

            }
        });

        if (mixinClasses.isEmpty()) {
            System.out.println("FAIL: 源码里一个 @Mixin 类都没找到，检查路径: " + srcRoot);
            System.exit(2);
            return;
        }

        List<String> missing = new ArrayList<>();
        List<String> uncovered = new ArrayList<>();
        for (Map.Entry<String, Path> e : mixinClasses.entrySet()) {
            String fqcn = e.getKey();
            String pkg = fqcn.substring(0, fqcn.lastIndexOf('.'));
            String simple = fqcn.substring(fqcn.lastIndexOf('.') + 1);

            Set<String> declared = declaredByPackage.get(pkg);
            if (declared == null) {
                uncovered.add(fqcn);
            } else if (!declared.contains(simple)) {
                missing.add(simple);
            }
        }

        if (!uncovered.isEmpty()) {
            System.out.println("FAIL: 这些 @Mixin 类所在的包没有任何 mixin 配置覆盖 -> " + uncovered);
            System.out.println("      漏掉的后果：运行到相关代码时抛 IllegalClassLoadError 直接崩游戏。");
            System.exit(1);
            return;
        }
        if (!missing.isEmpty()) {
            System.out.println("FAIL: 这些 @Mixin 类没有写进对应配置 " + configNames + " -> " + missing);
            System.out.println("      漏掉的后果：运行到相关代码时抛 IllegalClassLoadError 直接崩游戏。");
            System.exit(1);
            return;
        }
        System.out.println("mixins.json ok: " + mixinClasses.size() + " 个 @Mixin 类全部已注册（配置: " + configNames + "）");
        System.exit(0);
    }
}