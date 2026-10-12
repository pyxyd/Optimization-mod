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

/**
 * 让指定枪械（默认 AWP）命中即秒杀。
 *
 * 注入点：TaCZ 的 EntityKineticBullet.tacAttackEntity(...) 里对 Entity.hurt(DamageSource, float)
 * 的两处调用（反汇编确认偏移 53/75，分别对应多部件实体的各个部分）。
 *
 * 做法：
 *   1) 保留 TaCZ 原本的 DamageSource，只把伤害数值换成 damage（默认 10 万）——
 *      这样死亡信息、击杀归属、命中音效都还是 TaCZ 那一套；
 *   2) 若打完对方还活着（免疫/减伤/Boss），再调用 Entity.kill()（等价 /kill，
 *      使用 genericKill 伤害类型，无视无敌与减伤）强制击杀。
 *   affect_players = false 时，目标是玩家就走原版伤害。
 *
 * 注意：伤害结算在服务端，多人服务器需要服务端也装这个模组。
 */
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