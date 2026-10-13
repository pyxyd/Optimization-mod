package com.taczopt.adsfire;

import com.taczopt.TaczOptimizeMod;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.input.AimKey;
import com.tacz.guns.client.input.ShootKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TaczOptimizeMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AdsFireHandler {

    private static final float AIM_READY_PROGRESS = 0.99f;

    private static final int MANUAL_AIM_EXTRA_TICKS = 8;

    private static final int HARD_TIMEOUT_TICKS = 40;

    private AdsFireHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        AdsFireConfig.reloadIfChanged();
        if (!AdsFireConfig.enabled()) {
            AdsFireState.reset();
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        LocalPlayer player = mc == null ? null : mc.f_91074_;
        if (player == null) {
            AdsFireState.reset();
            return;
        }
        IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(player);
        if (operator == null) {
            return;
        }
        int tick = player.f_19797_;
        boolean shootDown = ShootKey.SHOOT_KEY != null && ShootKey.SHOOT_KEY.m_90857_();
        if (shootDown) {
            AdsFireState.markShootDown(tick);
        }

        ItemStack main = player.m_21205_();
        IGun gun = IGun.getIGunOrNull(main);
        String gunId = null;
        boolean enabledForThisGun = false;
        if (gun != null) {
            ResourceLocation id = gun.getGunId(main);
            if (id != null) {
                gunId = id.toString();
                enabledForThisGun = AdsFireConfig.isOn(gunId) && hasDisplay(main);
            }
        }

        if (!enabledForThisGun) {
            if (AdsFireState.isActive()) {
                releaseAim(operator);
            }
            return;
        }

        if (operator.isAim()) {
            if (AdsFireState.isActive() && !AdsFireState.hasFired()) {

                fireNow(operator, tick);
            }
            if (AdsFireState.isActive() && AdsFireState.hasFired() && !shootDown
                    && AdsFireConfig.releaseWhenStop()
                    && tick - AdsFireState.lastShootDownTick() > AdsFireConfig.holdTicks()) {
                releaseAim(operator);
            }
            return;
        }

        if (!AdsFireState.isActive()) {
            if (!shootDown) {
                return;
            }

            AdsFireState.begin(gunId, tick);
        }

        String sessionGun = AdsFireState.sessionGunId();
        if (sessionGun != null && !sessionGun.equals(gunId)) {
            releaseAim(operator);
            return;
        }

        if (AdsFireState.hasFired()) {

            if (AdsFireConfig.releaseWhenStop() && !shootDown
                    && tick - AdsFireState.lastShootDownTick() > AdsFireConfig.holdTicks()) {
                releaseAim(operator);
            }
            return;
        }

        if (!shootDown) {

            releaseAim(operator);
            return;
        }

        if (!AdsFireState.isAimRequested()) {

            AdsFireState.markAimRequested();
            operator.aim(true);
            return;
        }

        int waited = tick - AdsFireState.startTick();
        float progress = aimProgress(operator, mc);

        if (progress >= AIM_READY_PROGRESS && waited >= AdsFireConfig.aimWaitTicks()) {
            if (AdsFireConfig.debugLog()) {
                TaczOptimizeMod.logger().info("[taczopt] 开镜完成，开火。progress={} 等了{}tick",
                        progress, waited);
            }
            fireNow(operator, tick);
            return;
        }

        boolean manualAim = AimKey.AIM_KEY != null && AimKey.AIM_KEY.m_90857_();
        int limit = AdsFireConfig.aimWaitTicks() + (manualAim ? MANUAL_AIM_EXTRA_TICKS : 0);

        if (waited >= Math.max(limit, HARD_TIMEOUT_TICKS)) {
            if (AdsFireConfig.debugLog()) {
                TaczOptimizeMod.logger().info("[taczopt] 开镜超时兜底开火。progress={} 等了{}tick",
                        progress, waited);
            }
            fireNow(operator, tick);
        }
    }

    private static float aimProgress(IClientPlayerGunOperator operator, Minecraft mc) {
        float partial = mc == null ? 1.0f : mc.m_91297_();
        try {
            return operator.getClientAimingProgress(partial);
        } catch (RuntimeException e) {
            return 1.0f;
        }
    }

    private static void fireNow(IClientPlayerGunOperator operator, int tick) {

        AdsFireState.markFired(tick);
        boolean shootDown = ShootKey.SHOOT_KEY != null && ShootKey.SHOOT_KEY.m_90857_();
        try {
            operator.chargeShoot(shootDown);
            operator.shoot();
        } catch (RuntimeException ignored) {

        }
    }

    private static void releaseAim(IClientPlayerGunOperator operator) {
        boolean manualAim = AimKey.AIM_KEY != null && AimKey.AIM_KEY.m_90857_();
        boolean wasAutoAimed = AdsFireState.isAutoAimed();
        boolean aiming = operator.isAim();
        AdsFireState.end();
        if (wasAutoAimed && !manualAim && aiming) {
            operator.aim(false);
        }
    }

    private static boolean hasDisplay(ItemStack main) {
        try {
            return TimelessAPI.getGunDisplay(main).isPresent();
        } catch (RuntimeException e) {
            return false;
        }
    }
}
