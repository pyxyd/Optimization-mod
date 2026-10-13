package com.taczopt.crosshair;

import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

public final class CrouchAim {

    private CrouchAim() {
    }

    public static boolean active(LivingEntity shooter) {
        if (!(shooter instanceof Player)) {
            return false;
        }
        try {

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
