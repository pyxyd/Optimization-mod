package com.taczopt.mixin;

import com.tacz.guns.entity.EntityKineticBullet;
import com.taczopt.oneshot.OneShotConfig;
import com.taczopt.oneshot.OneShotLog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityKineticBullet.class)
public class EntityKineticBulletMixin {

    @Shadow
    private ResourceLocation gunId;

    @Redirect(
            method = "tacAttackEntity",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z")
    )
    private boolean taczopt$hurt(Entity target, DamageSource source, float amount) {
        OneShotConfig.reloadIfChanged();
        String id = this.gunId == null ? null : this.gunId.toString();
        if (!OneShotConfig.applies(id)) {
            return target.m_6469_(source, amount);
        }
        if (target instanceof Player && !OneShotConfig.affectPlayers) {
            return target.m_6469_(source, amount);
        }
        OneShotLog.lethalHit(id);
        boolean hurt = target.m_6469_(source, OneShotConfig.damage);
        if (OneShotConfig.forceKill && target.m_6084_()) {
            target.m_6074_();
        }
        return hurt;
    }
}
