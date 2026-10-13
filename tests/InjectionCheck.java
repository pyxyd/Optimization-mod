import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class InjectionCheck implements Opcodes {

    private static final String TARGET = "com/tacz/guns/api/item/gun/AbstractGunItem";
    private static final String METHOD = "doBulletSpread";

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("用法: InjectionCheck <tacz.jar>");
            System.exit(2);
        }
        byte[] cls = readEntry(args[0], TARGET + ".class");
        ClassNode cn = new ClassNode();
        new ClassReader(cls).accept(cn, ClassReader.SKIP_FRAMES);

        MethodNode target = null;
        for (MethodNode m : cn.methods) {
            if (METHOD.equals(m.name)) {
                target = m;
                break;
            }
        }
        check("目标类里有 " + METHOD, target != null);
        if (target == null) {
            System.exit(1);
        }
        System.out.println("  描述符: " + target.desc);

        Type[] params = Type.getArgumentTypes(target.desc);
        System.out.println("  参数表: " + params.length + " 个");

        String[] slotName = new String[128];
        for (int i = 0; i < params.length; i++) {
            slotName[i + 1] = "参数" + i + "(" + shortName(params[i]) + ")";
        }

        MethodInsnNode shoot = null;
        for (AbstractInsnNode insn = target.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call
                    && "net/minecraft/world/entity/projectile/Projectile".equals(call.owner)
                    && "m_37251_".equals(call.name)) {
                shoot = call;
                break;
            }
        }
        check("方法体里调用了 Projectile.m_37251_（把扩散交给弹道那一句）", shoot != null);
        if (shoot == null) {
            System.exit(1);
        }
        Type[] shootArgs = Type.getArgumentTypes(shoot.desc);
        check("m_37251_ 签名是 (Entity, F, F, F, F, F)",
                shootArgs.length == 6 && shootArgs[0].getDescriptor().endsWith("Entity;")
                        && shootArgs[5].getSort() == Type.FLOAT);

        System.out.println();
        System.out.println("=== 调用点的实参来源（从字节码回溯操作数栈）===");
        Deque<String> stack = new ArrayDeque<>();
        for (AbstractInsnNode insn = target.instructions.getFirst(); insn != null && insn != shoot;
                insn = insn.getNext()) {
            int op = insn.getOpcode();
            if (op == FCONST_0) {
                stack.push("常量 0.0");
            } else if (op == FCONST_1) {
                stack.push("常量 1.0");
            } else if (op == FCONST_2) {
                stack.push("常量 2.0");
            } else if (op == FLOAD || op == ALOAD || op == ILOAD) {
                int v = ((VarInsnNode) insn).var;
                String kind = op == FLOAD ? "fload" : (op == ALOAD ? "aload" : "iload");
                stack.push(kind + " " + v + "  = "
                        + (v < slotName.length && slotName[v] != null ? slotName[v] : "超出参数范围"));
            } else if (op == LDC) {
                stack.push("ldc 常量");
            } else if (insn instanceof MethodInsnNode m2) {
                for (int i = 0; i < Type.getArgumentTypes(m2.desc).length; i++) {
                    if (!stack.isEmpty()) {
                        stack.pop();
                    }
                }
                if (Type.getReturnType(m2.desc).getSort() != Type.VOID) {
                    stack.push("(" + m2.name + " 的返回值)");
                }
            }
        }
        String[] taken = new String[6];
        for (int i = 5; i >= 0; i--) {
            taken[i] = stack.isEmpty() ? "?" : stack.pop();
        }
        String[] names = { "shooter(Entity)", "x", "y", "z", "velocity", "inaccuracy(扩散)" };
        for (int i = 0; i < 6; i++) {
            System.out.println("  " + names[i] + " <- " + taken[i]);
        }

        System.out.println();
        String inacc = taken[5];
        check("扩散实参来自局部变量（不是常量）—— mixin 要替换的就是它",
                inacc != null && inacc.startsWith("fload"));
        String vel = taken[4];
        check("弹速实参来自局部变量（不是被误置 0 的那个）",
                vel != null && (vel.startsWith("fload") || vel.startsWith("常量")));

        System.out.println();
        System.out.println("=== 端到端断言：mixin 处理器的形参顺序 ===");

        String velFrom = paramOf(vel);
        String inaccFrom = paramOf(inacc);
        System.out.println("  调用点实际喂给 m_37251_ 的：");
        System.out.println("    velocity   <- " + vel + "   " + velFrom
                + "（= 处理器第几个 float）");
        System.out.println("    inaccuracy <- " + inacc + "   " + inaccFrom);
        check("弹速来自 T5（处理器第 5 个 float）—— 写错会把弹速置 0，子弹落地",
                "T5".equals(velFrom));
        check("扩散来自 T6（处理器第 6 个 float）—— 本 mixin 置 0 的就是它",
                "T6".equals(inaccFrom));

        System.out.println();
        System.out.println("=== 结论 ===");
        System.out.println("  处理器形参逐位对齐目标方法的形参表：");
        System.out.println("    ShooterDataHolder, ItemStack, LivingEntity, Projectile, int,");
        System.out.println("    float T5(=弹速), float T6(=扩散角), float T7, float T8");
        System.out.println("  要置 0 的是 T6；T5 必须原样传给 m_37251_ 的 velocity。");

        System.out.println();
        if (failures == 0) {
            System.out.println("InjectionCheck: 全部通过");
        } else {
            System.out.println("InjectionCheck: " + failures + " 项失败");
            System.exit(1);
        }
    }

    private static String paramOf(String desc) {
        if (desc == null || !desc.contains("参数")) {
            return "(无法判定)";
        }
        try {
            String num = desc.replaceAll(".*参数(\\d+).*", "$1");
            int idx = Integer.parseInt(num);
            if (idx >= 5 && idx <= 8) {
                return "T" + idx;
            }
            return "参数" + idx + "(非 float 位)";
        } catch (Exception e) {
            return "(解析失败)";
        }
    }

    private static String shortName(Type t) {
        String d = t.getDescriptor();
        if ("I".equals(d)) {
            return "int";
        }
        if ("F".equals(d)) {
            return "float";
        }
        if (d.startsWith("L") && d.endsWith(";")) {
            String s = d.substring(1, d.length() - 1);
            return s.substring(s.lastIndexOf('/') + 1);
        }
        return d;
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [OK]   " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static byte[] readEntry(String jarPath, String entry) throws Exception {
        try (JarFile jar = new JarFile(jarPath)) {
            JarEntry e = jar.getJarEntry(entry);
            if (e == null) {
                throw new IllegalStateException("jar 里没有 " + entry);
            }
            try (InputStream in = jar.getInputStream(e)) {
                return in.readAllBytes();
            }
        }
    }
}
