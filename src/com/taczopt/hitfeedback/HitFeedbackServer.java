package com.taczopt.hitfeedback;

import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.taczopt.TaczOptimizeMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 服务端：把"这一枪打中了谁、是不是头、有没有打死"告诉射手本人。
 *
 * <h2>为什么需要自己发</h2>
 * TaCZ 的 {@code EntityHurtByGunEvent} 只在<b>服务端</b>触发
 * （{@code EntityKineticBullet.onHitEntity} 里写死 {@code LogicalSide.SERVER}），
 * 它自己同步给客户端的 {@code ServerMessageGunHurt} 又只发给"目标附近的玩家" ——
 * 远距离狙击的人根本收不到。所以只能自己发一条给射手。
 *
 * <h2>为什么两个事件都要监听</h2>
 * {@code Post} <b>只在目标没死时</b>才发；打死的那一枪走的是
 * {@code EntityKillByGunEvent}。而且那个类<b>不是</b> {@code EntityHurtByGunEvent}
 * 的子类，方法名也不一样（{@code getKilledEntity()} / {@code getBaseDamage()}）。
 *
 * <p>这一版不再量血量差 —— 标记只需要"头 / 没头"和"死 / 没死"，
 * 那个 {@code Pre} 记血量、{@code Post} 取差的机制连同 {@code Pre} 监听一起删掉了。
 */
public final class HitFeedbackServer {

    private HitFeedbackServer() {
    }

    /** Post：目标没死 */
    @SubscribeEvent
    public static void onPost(EntityHurtByGunEvent.Post event) {
        report(event.getHurtEntity(), event.getAttacker(), event.isHeadShot(), false, event.getGunId());
    }

    /** Kill：目标死了（Post 不会发，所以必须单独监听） */
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
            // 只反馈给"射手本人"，而且只有玩家才有界面可显示
            if (!(attacker instanceof ServerPlayer shooter)) {
                return;
            }
            // 射手得和目标是同一个维度
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
            // 标记失败绝不能影响战斗本身
            TaczOptimizeMod.logger().warn("[taczopt] 命中标记发送失败", t);
        }
    }
}
