package com.taczopt.mixin;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.taczopt.TaczOptimizeMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 「蹲着开镜 = 只后坐力、无扩散」：把开火时的扩散在交给弹道前置 0。
 *
 * <h2>先分清：这件事和「潜行腰射扩散归零」不是一回事</h2>
 * 官方 Wiki（tacwiki「精度与晃动」）写明五种精度状态<b>互斥</b>、每枪只取一个，按优先级：
 * <pre>
 *   1. aim    完全瞄准（进度 = 100%）   &lt;- 蹲下<b>开镜</b>生效的是这一档
 *   2. lie    趴下
 *   3. sneak  潜行（蹲下）              &lt;- 蹲着<b>腰射</b>生效的是这一档
 *   4. move   移动中
 *   5. stand  站立
 * </pre>
 * 而且是<b>替换</b>不是叠加 —— 蹲着开镜时取 aim，sneak 那一档根本不参与。
 * 所以两件事必须分开处理：
 * <ul>
 *   <li><b>潜行腰射扩散</b>（蹲着不开镜）：直接改枪包数据 —— 119 把枪的
 *       {@code inaccuracy.sneak} 全改成 0。面板显示和弹道用的是同一份数据，
 *       自然一致，<b>不需要本 mixin 参与</b>。</li>
 *   <li><b>蹲着开镜</b>：生效的是 aim 档（如 m7_prismatic 是 0.05）。
 *       改 sneak 对它没用，所以由本 mixin 在扩散交给弹道前置 0。</li>
 * </ul>
 *
 * <h2>参数顺序（用操作数栈回溯验证过，别再靠"形参名"猜）</h2>
 * {@code tools\InjectionCheck.java} 读 TaCZ 字节码、回溯 {@code m_37251_} 调用点的
 * 操作数栈，得出：
 * <pre>
 *   m_37251_(Entity, x,   y,        z,       velocity, inaccuracy)
 *             │       │    │        │        │         │
 *          aload 3  fload8 fload9  fconst_0  fload6    fload7
 *          =参数2   =参数7 =参数8    常量    =参数5    =参数6
 * </pre>
 * 也就是说 {@code doBulletSpread} 的形参表是
 * {@code (…, Projectile, int, float T5, float T6, float T7, float T8)}，其中
 * <b>第 5 个 float（T5）= 弹速</b>、<b>第 6 个 float（T6）= 扩散角</b>。
 *
 * <h2>⚠ 这个顺序我改错过两次，两次现象正好相反</h2>
 * <ol>
 *   <li>写成 {@code (…, float velocity, float degree)}（即以为 T5=degree）——
 *       实际把 <b>T5=弹速置成了 0</b>，扩散原样放过。
 *       现象：<b>子弹落地、击退变零</b>，而且"还是有扩散"。</li>
 *   <li>按"形参名"再换一次 —— 又换回了错的顺序，复现了同一个 bug。</li>
 * </ol>
 * 结论：<b>只认校验器打出来的槽位表</b>，不要看形参名、不要凭"哪个更合理"推断。
 * build.ps1 里已经接了 InjectionCheck，顺序不对会直接构建失败。
 *
 * <h2>为什么不在 {@code ModernKineticGunScriptAPI.shoot} 里改</h2>
 * 那里扩散值藏在局部变量槽位 10/11 里，只能靠 {@code @ModifyVariable(ordinal = …)} 定位。
 * 那个 ordinal 依赖局部变量表，我**没法离线验证**它对不对 ——
 * 上一版就是这么写的，结果静默失效（{@code require = 0} 不报错），
 * 用户实测"还是有扩散"。
 * 这里改成按**方法名 + 参数位置**注入，不存在猜 ordinal 的问题。
 *
 * <h2>代价（明确记下来）</h2>
 * 整个方法被取消，所以走的是"无 Lua 脚本"那条路径 —— 也就是**会跳过
 * {@code calcSpread} 脚本自定义的散布图案**。用户的枪包没有任何一把定义了
 * {@code calcSpread}（全库搜过，只有 TaCZ 那个参考脚本里有），所以无影响。
 * 哪天有枪包用到它，蹲着开镜时图案会被忽略（仍然无扩散，符合本需求）。
 *
 * <h2>⚠ 静默失效的风险与验证方法</h2>
 * {@code require = 1}：注入不上就**直接报错**，不静默 —— 宁可启动失败也不要
 * "以为做了其实没做"。开一枪后日志里会有一条（只打一次）：
 * <pre>
 *   [taczopt] 蹲下瞄准 已生效：扩散 -> 0
 * </pre>
 */
@Mixin(value = AbstractGunItem.class, remap = false)
public class CrouchAimSpreadMixin {

    /** 日志只打一次，免得连发刷屏 */
    @Unique
    private static boolean taczopt$logged = false;

    @Inject(method = "doBulletSpread", at = @At("HEAD"), cancellable = true, remap = false)
    private void taczopt$noSpreadWhenCrouchAiming(
            com.tacz.guns.entity.shooter.ShooterDataHolder dataHolder,
            net.minecraft.world.item.ItemStack itemStack,
            LivingEntity shooter,
            Projectile projectile,
            int ammoCount,
            float t5,
            float t6,
            float t7,
            float t8,
            CallbackInfo ci) {
        // 形参名故意用 t5..t8 而不是 velocity/degree ——
        // 这两个名字我按感觉猜错过两次（见类注释），所以干脆不猜，
        // 只按 InjectionCheck 验出来的槽位号对应：
        //   t5 = 第 5 个 float = 弹速
        //   t6 = 第 6 个 float = 扩散角（要置 0 的就是它）
        //   t7 / t8 = 原样转发，不动
        final float velocity = t5;
        final float degree = t6;
        boolean zero;
        try {
            zero = taczopt$shouldZero(shooter);
        } catch (Throwable t) {
            zero = false;
        }
        if (!zero) {
            return;
        }
        try {
            // 复制 TaCZ 原本那一句（见类注释里的槽位对照），只把扩散换成 0：
            //   m_37251_(shooter, x=参数7, y=参数8, z=0, velocity=参数5, inaccuracy=参数6)
            projectile.m_37251_(shooter, t7, t8, 0.0F, velocity, 0.0F);
            if (!taczopt$logged) {
                taczopt$logged = true;
                TaczOptimizeMod.logger().info(
                        "[taczopt] 蹲下瞄准 已生效：扩散 {} -> 0（弹速 {} 原样保留）", degree, velocity);
            }
            ci.cancel();
        } catch (Throwable t) {
            // 复制失败就别取消，让 TaCZ 自己走原路径，不要让这一枪打不出去
            TaczOptimizeMod.logger().warn("[taczopt] 蹲下瞄准 改写失败，退回原路径", t);
        }
    }

    /**
     * 是否该抹掉扩散：<b>蹲</b> + <b>开镜到位</b>。
     *
     * <p>为什么不"只要蹲就抹"：蹲着<b>腰射</b>用户也要有扩散。
     * 这一点由 {@code getSynAimingProgress() == 1.0} 区分 —— 它只在开镜真正完成时才是 1.0。
     *
     * <p>{@code IGunOperator} 是 TaCZ 用 Mixin 混进 {@code LivingEntity} 的，
     * 运行时可以直接强转；仍然套 try/catch，拿不到就当没开镜。
     */
    @Unique
    private static boolean taczopt$shouldZero(LivingEntity shooter) {
        if (shooter == null) {
            return false;
        }
        try {
            if (!(shooter instanceof IGunOperator op) || op.getSynAimingProgress() != 1.0f) {
                return false;
            }
        } catch (Throwable t) {
            return false;
        }
        try {
            return shooter.m_20089_() == Pose.CROUCHING;
        } catch (Throwable t) {
            return false;
        }
    }
}
