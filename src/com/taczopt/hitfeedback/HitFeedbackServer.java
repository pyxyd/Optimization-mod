package com.taczopt.hitfeedback;

import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.taczopt.TaczOptimizeMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class HitFeedbackServer {

    private HitFeedbackServer() {
    }

    @SubscribeEvent
    public static void onPost(EntityHurtByGunEvent.Post event) {
        report(event.getHurtEntity(), event.getAttacker(), event.isHeadShot(), false, event.getGunId());
    }

    @SubscribeEvent
    public static void onKill(EntityKillByGunEvent event) {
        report(event.getKilledEntity(), event.getAttacker(), event.isHeadShot(), true, event.getGunId());
    }

    private static void report(Entity hurt, LivingEntity attacker, boolean headshot, boolean killed,
                               net.minecraft.resources.ResourceLocation gunId) {
        if (!HitFeedbackConfig.enabled()) {
            return;
        }
        try {
            if (hurt == null) {
                return;
            }

            if (!(attacker instanceof ServerPlayer shooter)) {
                return;
            }

            if (shooter.m_9236_() != hurt.m_9236_()) {
                return;
            }

            HitFeedbackNetwork.sendToPlayer(
                    new HitFeedbackNetwork.HitFeedbackMessage(headshot, killed,
                            gunId == null ? "" : gunId.toString()),
                    shooter);

            if (HitFeedbackConfig.debugLog()) {
                TaczOptimizeMod.logger().info(
                        "[taczopt] 命中标记：gun={} 爆头={} 致死={} 目标={}",
                        gunId, headshot, killed, hurt.m_6095_());
            }
        } catch (Throwable t) {

            TaczOptimizeMod.logger().warn("[taczopt] 命中标记发送失败", t);
        }
    }
}
