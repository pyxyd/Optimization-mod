import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * 构建期校验：taczopt.mixins.json 必须列出所有带 @Mixin 的类。
 *
 * <p>为什么需要：漏一个 @Mixin 类不会导致编译失败，也不会在启动时报错，
 * 而是等到运行到那条代码路径时才抛
 * {@code IllegalClassLoadError: Illegal classload request for accessor mixin ...}
 * —— 直接崩游戏。合并模组时就漏抄过两个 @Accessor 接口，导致"打枪出现弹孔就崩"。
 *
 * <p>用法：MixinsCheck <srcRoot> <mixinJsonPath>
 */
public class MixinsCheck {

    private static final Pattern MIXIN_ANNOTATION = Pattern.compile("@Mixin\\s*\\(");
    private static final Pattern PACKAGE_DECL = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;");

    public static void main(String[] args) throws Exception {
        Path srcRoot = Paths.get(args[0]);
        Path jsonPath = Paths.get(args[1]);

        String json = new String(Files.readAllBytes(jsonPath), StandardCharsets.UTF_8);
        Set<String> declared = new HashSet<>();
        Matcher m = Pattern.compile("\"([A-Za-z0-9_$]+)\"").matcher(json);
        while (m.find()) {
            declared.add(m.group(1));
        }

        // 扫描源码里所有 @Mixin 类，按包名 + 简单名收集
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
                // 读不了就跳过
            }
        });

        if (mixinClasses.isEmpty()) {
            System.out.println("FAIL: 源码里一个 @Mixin 类都没找到，检查路径: " + srcRoot);
            System.exit(2);
            return;
        }

        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, Path> e : mixinClasses.entrySet()) {
            String fqcn = e.getKey();
            String pkg = fqcn.substring(0, fqcn.lastIndexOf('.'));
            String simple = fqcn.substring(fqcn.lastIndexOf('.') + 1);
            // 只检查与本配置同包的那些（accessor 等）
            if (pkg.equals("com.taczopt.mixin") && !declared.contains(simple)) {
                missing.add(simple);
            }
        }

        if (!missing.isEmpty()) {
            System.out.println("FAIL: 这些 @Mixin 类没有写进 " + jsonPath.getFileName() + " -> " + missing);
            System.out.println("      漏掉的后果：运行到相关代码时抛 IllegalClassLoadError 直接崩游戏。");
            System.exit(1);
            return;
        }
        System.out.println("mixins.json ok: " + mixinClasses.size() + " 个 @Mixin 类全部已注册");
        System.exit(0);
    }
}
