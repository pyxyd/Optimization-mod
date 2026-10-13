package com.taczopt.crosshair;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.AimInaccuracyModifier;
import com.tacz.guns.resource.modifier.custom.InaccuracyModifier;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.HitboxHelper;
import com.taczopt.TaczOptimizeMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class CrosshairStyleClient {

    private CrosshairStyleClient() {
    }

    public static boolean shouldTakeOver() {
        if (!CrosshairConfig.enabled()) {
            return false;
        }
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (player == null) {
            return false;
        }

        if (player.m_6117_()) {
            return false;
        }

        if (CrosshairConfig.hideOnAim() && (isAiming(player) || CrouchAim.active(player))) {
            return true;
        }

        boolean holdingGun;
        try {
            holdingGun = IGun.mainHandHoldGun(player);
        } catch (Throwable t) {
            holdingGun = false;
        }
        if (CrosshairConfig.onlyGuns() && !holdingGun) {
            return false;
        }
        return true;
    }

    private static boolean isAiming(LocalPlayer player) {
        try {
            return IClientPlayerGunOperator.fromLocalPlayer(player).isAim();
        } catch (Throwable t) {
            return false;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPreOverlay(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) {
            return;
        }
        if (!shouldTakeOver()) {
            return;
        }

        event.setCanceled(true);

        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (player == null) {
            return;
        }
        if (CrosshairConfig.hideOnAim() && (isAiming(player) || CrouchAim.active(player))) {
            return;
        }
        drawCrosshair(event.getGuiGraphics(), player);
    }

    private static void drawCrosshair(GuiGraphics graphics, LocalPlayer player) {
        try {
            int centerX = Minecraft.m_91087_().m_91268_().m_85445_() / 2;
            int centerY = Minecraft.m_91087_().m_91268_().m_85446_() / 2;

            ItemStack held = player.m_21205_();
            IGun iGun = IGun.getIGunOrNull(held);

            int gap;
            if (CrosshairConfig.dynamic() && iGun != null) {
                float ina = inaccuracy(player, iGun, held);
                gap = CrosshairLogic.gapFor(ina, CrosshairConfig.spreadScale(),
                        1, CrosshairConfig.gapMax());
            } else {
                gap = CrosshairConfig.gap();
            }

            int color = 0xFF000000 | CrosshairConfig.color();
            int thickness = CrosshairConfig.thickness();
            int arm = CrosshairConfig.arm();
            int dot = CrosshairConfig.dot();

            if (CrosshairConfig.debugLog()) {
                logOnce(gap, iGun, player, held);
            }

            for (CrosshairLogic.Bar b : CrosshairLogic.cross(gap, arm, thickness)) {
                fill(graphics, centerX, centerY, b, color);
            }
            if (dot > 0) {
                fill(graphics, centerX, centerY, CrosshairLogic.dot(dot), color);
            }
        } catch (Throwable t) {
            TaczOptimizeMod.logger().warn("[taczopt] 画准星失败", t);
        }
    }

    @SuppressWarnings("deprecation")
    private static float inaccuracy(LocalPlayer player, IGun iGun, ItemStack held) {
        IGunOperator op = null;
        try {
            op = IGunOperator.fromLivingEntity(player);
        } catch (Throwable ignored) {

        }

        float aimingProgress = 0f;
        try {
            if (op != null) {
                aimingProgress = op.getSynAimingProgress();
            }
        } catch (Throwable ignored) {

        }
        boolean swimming = false;
        boolean crouching = false;
        try {
            swimming = player.m_6069_() || player.m_20089_() == Pose.SWIMMING;
            crouching = player.m_20089_() == Pose.CROUCHING;
        } catch (Throwable ignored) {

        }
        boolean moving = false;
        try {

            moving = HipFireSpread.isMove(
                    Math.abs(player.f_19787_ - player.f_19867_),
                    HitboxHelper.getPlayerVelocity(player).m_82553_());
        } catch (Throwable ignored) {

            try {
                moving = HipFireSpread.isMove(
                        Math.abs(player.f_19787_ - player.f_19867_), Double.NaN);
            } catch (Throwable ignored2) {

            }
        }
        HipFireSpread.Stance stance =
                HipFireSpread.stanceFor(aimingProgress, swimming, crouching, moving);

        Float base = null;
        try {
            AttachmentCacheProperty cache = op == null ? null : op.getCacheProperty();
            if (cache != null) {
                base = lookup(cache.getCache(InaccuracyModifier.ID), stance);
                if (base == null) {

                    base = lookup(cache.getCache(AimInaccuracyModifier.ID), stance);
                }
            }
        } catch (Throwable ignored) {

        }
        if (base == null) {
            try {
                GunData data = TimelessAPI.getClientGunIndex(iGun.getGunId(held))
                        .map(ClientGunIndex::getGunData).orElse(null);
                if (data != null) {
                    base = lookup(data.getInaccuracy(), stance);
                }
            } catch (Throwable ignored) {

            }
        }
        if (base == null) {
            base = HipFireSpread.defaultFor(stance);
        }

        float heatMul = 1.0f;
        try {
            if (iGun.hasHeatData(held)) {
                GunData data = TimelessAPI.getClientGunIndex(iGun.getGunId(held))
                        .map(ClientGunIndex::getGunData).orElse(null);
                var heat = data == null ? null : data.getHeatData();
                if (heat != null) {
                    heatMul = HipFireSpread.heatMultiplier(true,
                            iGun.getHeatAmount(held), heat.getHeatMax(),
                            heat.getMinInaccuracy(), heat.getMaxInaccuracy());
                }
            }
        } catch (Throwable ignored) {

        }

        return HipFireSpread.spread(base, heatMul);
    }

    private static Float lookup(Map<?, Float> table, HipFireSpread.Stance stance) {
        if (table == null) {
            return null;
        }
        String want = stance.name();
        for (Map.Entry<?, Float> e : table.entrySet()) {
            if (e.getValue() != null && want.equalsIgnoreCase(String.valueOf(e.getKey()))) {
                return e.getValue();
            }
        }
        return null;
    }

    private static String lastLogged = null;

    private static void logOnce(int gap, IGun iGun, LocalPlayer player, ItemStack held) {
        try {
            String gunId = iGun == null ? "(空手)" : String.valueOf(iGun.getGunId(held));
            boolean sneaking = player.m_20089_() == Pose.CROUCHING;
            boolean swimming = player.m_6069_() || player.m_20089_() == Pose.SWIMMING;
            boolean moving = HipFireSpread.isMove(
                    Math.abs(player.f_19787_ - player.f_19867_),
                    HitboxHelper.getPlayerVelocity(player).m_82553_());
            String key = gunId + "|" + sneaking + "|" + swimming + "|" + moving + "|" + gap;
            if (key.equals(lastLogged)) {
                return;
            }
            lastLogged = key;
            TaczOptimizeMod.logger().info(
                    "[taczopt] 准星：枪={} 蹲={} 游={} 移动={} -> 间距={}px（基础: 线长{} 线宽{} 中心点{}，扩散倍率{}x）",
                    gunId, sneaking, swimming, moving, gap,
                    CrosshairConfig.arm(), CrosshairConfig.thickness(), CrosshairConfig.dot(),
                    CrosshairConfig.spreadScale());
        } catch (Throwable ignored) {

        }
    }

    private static void fill(GuiGraphics graphics, int centerX, int centerY,
                             CrosshairLogic.Bar b, int color) {
        graphics.m_280509_(centerX + b.x0(), centerY + b.y0(),
                centerX + b.x1() + 1, centerY + b.y1() + 1, color);
    }
}
