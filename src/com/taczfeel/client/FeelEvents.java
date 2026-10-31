package com.taczfeel.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.client.event.BeforeRenderHandEvent;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.IGun;
import com.taczfeel.FeelConfig;
import com.taczfeel.FeelLog;
import com.taczfeel.FeelMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 所有客户端钩子都在这里。
 * 每个钩子都单独 try/catch：任何一项出问题只会打日志跳过，不会让游戏崩。
 */
@Mod.EventBusSubscriber(modid = FeelMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FeelEvents {

    private static final ThreadLocalRandom RND = ThreadLocalRandom.current();

    private FeelEvents() {
    }

    // ------------------------------------------------------------------
    // 每 tick：热重载配置 + 推进尾音队列 + 状态回正
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        try {
            FeelConfig.reloadIfChanged();
            FeelState.tick();
            TailSounds.tick();
        } catch (Throwable t) {
            FeelLog.softError("client tick", t);
        }
    }

    // ------------------------------------------------------------------
    // 开火：给自己加冲量（只处理本地玩家）
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void onGunFire(GunFireEvent event) {
        try {
            if (event.getLogicalSide() != LogicalSide.CLIENT) {
                return;
            }
            Minecraft mc = Minecraft.m_91087_();
            if (mc == null) {
                return;
            }
            LocalPlayer player = mc.f_91074_;
            if (player == null || event.getShooter() != player) {
                return;
            }
            boolean aiming = false;
            try {
                IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(player);
                aiming = operator != null && operator.isAim();
            } catch (Throwable ignored) {
                // 开镜状态取不到就当腰射处理
            }
            float scale = GunProfiles.recoilScale(event.getGunItemStack());
            FeelState.onShot(scale, aiming);
        } catch (Throwable t) {
            FeelLog.softError("gun fire", t);
        }
    }

    // ------------------------------------------------------------------
    // 第一人称枪身视觉后坐（TaCZ 官方渲染事件，就在手/枪渲染前一刻）
    // 坐标约定：+X 右、+Y 上、+Z 朝玩家自己。绕 X 正转 = 枪口抬起。
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void onBeforeRenderHand(BeforeRenderHandEvent event) {
        if (!FeelConfig.kickEnabled) {
            return;
        }
        try {
            Minecraft mc = Minecraft.m_91087_();
            if (mc == null) {
                return;
            }
            LocalPlayer player = mc.f_91074_;
            if (player == null || !IGun.mainhandHoldGun(player)) {
                return;
            }
            float[] v = FeelState.kickVector();
            if (v == null) {
                return;
            }
            PoseStack pose = event.getPoseStack();
            if (v[3] != 0F || v[4] != 0F) {
                Quaternionf q = new Quaternionf();
                if (v[3] != 0F) {
                    q.rotateX((float) Math.toRadians(v[3]));
                }
                if (v[4] != 0F) {
                    q.rotateZ((float) Math.toRadians(v[4]));
                }
                pose.m_252781_(q);
            }
            if (v[0] != 0F || v[1] != 0F || v[2] != 0F) {
                pose.m_252880_(v[0], v[1], v[2]);
            }
        } catch (Throwable t) {
            FeelLog.softError("hand kick", t);
        }
    }

    // ------------------------------------------------------------------
    // 镜头冲击：LOWEST 保证在 TaCZ 自己的后坐之后叠加
    // ------------------------------------------------------------------
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!FeelConfig.cameraEnabled) {
            return;
        }
        try {
            float[] v = FeelState.cameraVector();
            if (v == null) {
                return;
            }
            event.setYaw(event.getYaw() + v[0]);
            event.setPitch(event.getPitch() + v[1]);
            event.setRoll(event.getRoll() + v[2]);
        } catch (Throwable t) {
            FeelLog.softError("camera punch", t);
        }
    }

    // ------------------------------------------------------------------
    // 开镜景深：世界画完、第一人称枪模还没画的那一刻做后期处理
    // （所以糊的只有世界，枪模/手/HUD 永远锐利，不需要读深度图）
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!FeelConfig.dofEnabled) {
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        try {
            Minecraft mc = Minecraft.m_91087_();
            if (mc == null || mc.f_91073_ == null) {
                return;
            }
            LocalPlayer player = mc.f_91074_;
            if (player == null) {
                return;
            }
            if (FeelConfig.dofOnlyGun && !IGun.mainhandHoldGun(player)) {
                return;
            }
            IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(player);
            if (operator == null) {
                return;
            }
            float aim = operator.getClientAimingProgress(event.getPartialTick());
            if (aim <= 0.02F) {
                return;
            }
            DofEffect.render(event.getPartialTick(), aim);
        } catch (Throwable t) {
            FeelLog.softError("dof", t);
        }
    }

    // ------------------------------------------------------------------
    // 开镜暗角
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        try {
            Vignette.render(event.getGuiGraphics(), event.getPartialTick());
        } catch (Throwable t) {
            FeelLog.softError("vignette", t);
        }
    }

    // ------------------------------------------------------------------
    // 命中火花
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void onEntityHurtByGun(EntityHurtByGunEvent.Post event) {
        if (!FeelConfig.hitSpark) {
            return;
        }
        try {
            if (event.getLogicalSide() != LogicalSide.CLIENT) {
                return;
            }
            Minecraft mc = Minecraft.m_91087_();
            if (mc == null) {
                return;
            }
            LocalPlayer player = mc.f_91074_;
            if (player == null || event.getAttacker() != player) {
                return;
            }
            Entity hurt = event.getHurtEntity();
            if (hurt == null) {
                return;
            }
            Entity bullet = event.getBullet();
            Level level = bullet != null ? bullet.m_9236_() : hurt.m_9236_();
            if (level == null) {
                return;
            }
            double x = bullet != null ? bullet.m_20185_() : hurt.m_20185_();
            double y = bullet != null ? bullet.m_20186_() : hurt.m_20186_() + hurt.m_20206_() * 0.6D;
            double z = bullet != null ? bullet.m_20189_() : hurt.m_20189_();
            int count = FeelConfig.hitSparkCount;
            float speed = FeelConfig.hitSparkSpeed;
            for (int i = 0; i < count; i++) {
                double vx = (RND.nextDouble() - 0.5D) * speed;
                double vy = RND.nextDouble() * speed * 0.9D;
                double vz = (RND.nextDouble() - 0.5D) * speed;
                level.m_7106_(ParticleTypes.f_123797_, x, y, z, vx, vy, vz);
            }
        } catch (Throwable t) {
            FeelLog.softError("hit spark", t);
        }
    }

    // ------------------------------------------------------------------
    // 换个世界 / 退出：清干净状态，免得带着上局的冲击量进新世界
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        FeelState.reset();
        TailSounds.clear();
        GunProfiles.clear();
    }
}
