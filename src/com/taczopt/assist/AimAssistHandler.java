package com.taczopt.assist;

import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.api.entity.ITargetEntity;
import com.tacz.guns.api.item.IGun;
import com.taczopt.TaczOptimizeMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.NamedGuiOverlay;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Aim assist: while toggled on (Y), each client tick turns the view a little toward the nearest
 * valid target inside the configured range and cone.
 *
 * It only nudges the player's own rotation (setYRot/setXRot), which is what the crosshair and
 * TaCZ's shot direction use, so "slowly pulls toward the creature" is literally the direction the
 * bullet follows. Nothing is sent to the server and nothing about the target is changed.
 *
 * Valid targets are living entities plus TaCZ's own target entities (TargetMinecart implements
 * ITargetEntity and extends AbstractMinecart, so it is NOT a LivingEntity and the old
 * LivingEntity-only lookup could never see it). The toggle itself is persisted in the config.
 */
@Mod.EventBusSubscriber(modid = TaczOptimizeMod.MOD_ID, value = Dist.CLIENT)
public final class AimAssistHandler {

    public static final String CATEGORY = "key.category.taczopt";

    public static final KeyMapping ASSIST_KEY = new KeyMapping(
            "key.taczopt.aim_assist",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Y,
            CATEGORY);

    private static boolean wasDown = false;

    private AimAssistHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft == null ? null : minecraft.f_91074_;
        if (player == null) {
            return;
        }
        AssistConfig.reloadIfChanged();

        // Rising-edge detection instead of KeyMapping.consumeClick(): that method is private in
        // this mapping set, and isDown() is enough for a simple toggle.
        boolean down = ASSIST_KEY != null && ASSIST_KEY.m_90857_();
        if (down && !wasDown) {
            AssistState.toggle();
            player.m_5661_(Component.m_237113_("[TaCZ优化] 辅助瞄准 " + (AssistState.isOn() ? "开" : "关")), true);
            TaczOptimizeMod.logger().info("[taczopt] aim assist toggled: {}", AssistState.describe());
        }
        wasDown = down;

        if (!AssistState.isOn() || !AssistConfig.enabled) {
            return;
        }
        if (minecraft.f_91080_ != null) {
            return;
        }
        if (AssistConfig.requireGun && !IGun.mainHandHoldGun(player)) {
            return;
        }
        taczopt$step(player);
    }

    /** Small on-screen reminder while the assist is on, drawn after the hotbar. */
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!AssistState.isOn()) {
            return;
        }
        NamedGuiOverlay overlay = event.getOverlay();
        if (overlay == null || !overlay.id().toString().endsWith(":hotbar")) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        GuiGraphics graphics = event.getGuiGraphics();
        if (minecraft == null || graphics == null || minecraft.f_91062_ == null) {
            return;
        }
        Font font = minecraft.f_91062_;
        String text = "辅助瞄准 ON";
        String target = AssistState.target();
        if (!target.isEmpty()) {
            text = text + "  " + target;
        }
        graphics.m_280137_(font, text, 6, 6, 0xFF66FF66);
    }
    private static void taczopt$step(LocalPlayer player) {
        Entity target = taczopt$nearest(player);
        if (target == null) {
            AssistState.setLastTarget("");
            return;
        }
        Vec3 eye = player.m_146892_();
        Vec3 aim = taczopt$aimPoint(target);
        if (eye == null || aim == null) {
            return;
        }
        double dx = aim.f_82479_ - eye.f_82479_;
        double dy = aim.f_82480_ - eye.f_82480_;
        double dz = aim.f_82481_ - eye.f_82481_;
        if (dx == 0.0D && dz == 0.0D) {
            return;
        }
        double flat = Math.sqrt(dx * dx + dz * dz);
        double wantYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double wantPitch = -Math.toDegrees(Math.atan2(dy, flat));

        float curYaw = player.m_146908_();
        float curPitch = player.m_146909_();
        double deltaYaw = taczopt$wrap(wantYaw - curYaw);
        if (Math.abs(deltaYaw) > AssistConfig.halfAngle) {
            AssistState.setLastTarget("");
            return;
        }
        double deltaPitch = wantPitch - curPitch;

        double step = AssistConfig.speedDegrees;
        double applyYaw = Math.max(-step, Math.min(step, deltaYaw));
        double applyPitch = Math.max(-step, Math.min(step, deltaPitch));
        if (applyYaw == 0.0D && applyPitch == 0.0D) {
            return;
        }
        player.m_146922_(curYaw + (float) applyYaw);
        player.m_146926_((float) Math.max(-90.0D, Math.min(90.0D, curPitch + applyPitch)));
        AssistState.setLastTarget(taczopt$label(target));
    }

    /**
     * Where to put the crosshair on a target. Living entities get their eye position; anything
     * else (the target cart) uses the centre of its hitbox, which for the 0.75 x 2.4 cart lands on
     * the target plate instead of at the wheels.
     */
    private static Vec3 taczopt$aimPoint(Entity target) {
        if (target instanceof LivingEntity living) {
            Vec3 eye = living.m_146892_();
            if (eye != null) {
                return eye;
            }
        }
        return target.m_20191_().m_82399_();
    }

    private static String taczopt$label(Entity target) {
        if (target instanceof ITargetEntity) {
            return "标靶";
        }
        return target.getClass().getSimpleName();
    }

    private static Entity taczopt$nearest(LocalPlayer player) {
        double range = AssistConfig.range;
        Vec3 pos = player.m_20182_();
        if (pos == null) {
            return null;
        }
        AABB box = new AABB(
                pos.f_82479_ - range, pos.f_82480_ - range, pos.f_82481_ - range,
                pos.f_82479_ + range, pos.f_82480_ + range, pos.f_82481_ + range);
        List<Entity> candidates;
        try {
            candidates = player.m_9236_().m_45976_(Entity.class, box);
        } catch (Throwable t) {
            return null;
        }
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity candidate : candidates) {
            if (candidate == player || !candidate.m_6084_()) {
                continue;
            }
            if (candidate instanceof Player && !AssistConfig.includePlayers) {
                continue;
            }
            // TargetMinecart is an AbstractMinecart implementing ITargetEntity, not a
            // LivingEntity, so both have to be accepted here.
            if (!(candidate instanceof LivingEntity) && !(candidate instanceof ITargetEntity)) {
                continue;
            }
            if (AssistConfig.requireLineOfSight) {
                // No pulling through walls: LivingEntity.hasLineOfSight is m_142582_, the same
                // check TaCZ's own melee path uses.
                boolean visible;
                try {
                    visible = player.m_142582_(candidate);
                } catch (Throwable t) {
                    visible = true;
                }
                if (!visible) {
                    continue;
                }
            }
            Vec3 other = candidate.m_20182_();
            if (other == null) {
                continue;
            }
            double ddx = other.f_82479_ - pos.f_82479_;
            double ddy = other.f_82480_ - pos.f_82480_;
            double ddz = other.f_82481_ - pos.f_82481_;
            double d = ddx * ddx + ddy * ddy + ddz * ddz;
            if (d < bestDistance) {
                bestDistance = d;
                best = candidate;
            }
        }
        return best;
    }

    private static double taczopt$wrap(double angle) {
        double wrapped = angle % 360.0D;
        if (wrapped > 180.0D) {
            wrapped -= 360.0D;
        }
        if (wrapped < -180.0D) {
            wrapped += 360.0D;
        }
        return wrapped;
    }
}