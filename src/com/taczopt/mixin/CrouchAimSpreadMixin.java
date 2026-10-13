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

@Mixin(value = AbstractGunItem.class, remap = false)
public class CrouchAimSpreadMixin {

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

            projectile.m_37251_(shooter, t7, t8, 0.0F, velocity, 0.0F);
            if (!taczopt$logged) {
                taczopt$logged = true;
                TaczOptimizeMod.logger().info(
                        "[taczopt] 蹲下瞄准 已生效：扩散 {} -> 0（弹速 {} 原样保留）", degree, velocity);
            }
            ci.cancel();
        } catch (Throwable t) {

            TaczOptimizeMod.logger().warn("[taczopt] 蹲下瞄准 改写失败，退回原路径", t);
        }
    }

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
