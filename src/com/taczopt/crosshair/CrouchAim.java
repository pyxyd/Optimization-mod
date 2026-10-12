package com.taczopt.crosshair;

import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

/**
 * 「蹲下瞄准 = 开镜」的判定。
 *
 * <p>需求：蹲着开镜时按"真正的 ADS"处理 —— 只有后坐力，**没有扩散**。
 * 这和 TaCZ 自带的蹲姿（{@code InaccuracyType.SNEAK}）不是一回事：
 * SNEAK 只是把扩散换成"蹲"那一档（比如 ak12 是 5.0），仍然会散布。
 * 这里要的是**扩散归零**。
 *
 * <p>判定条件：目标是玩家 + 开镜到位（{@code getSynAimingProgress() == 1.0}）+ 姿态是蹲。
 * 三条缺一不可 —— 只按蹲姿会把"蹲着腰射"也算进去，那种情况显然该有扩散。
 *
 * <p>客户端和服务端都要判：客户端决定准星表现，服务端决定弹道。
 */
public final class CrouchAim {

    private CrouchAim() {
    }

    /**
     * 是否处于"蹲下瞄准"。
     *
     * @param shooter 射手；非玩家一律 false
     */
    public static boolean active(LivingEntity shooter) {
        if (!(shooter instanceof Player)) {
            return false;
        }
        try {
            // 必须真的开镜到位，否则"蹲着腰射"也会被算成无扩散
            if (IGunOperator.fromLivingEntity(shooter).getSynAimingProgress() != 1.0f) {
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
