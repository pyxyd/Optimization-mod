package com.taczopt.hybridaim;

import com.taczopt.TaczOptimizeMod;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.input.AimKey;
import com.tacz.guns.util.InputExtraCheck;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 混合开镜：
 *   按下时：如果本来开着镜 -> 立刻关（点一下就是关）；
 *           如果本来关着 -> 先开镜（长按期间就靠它保持），并记下这一轮是我们开的；
 *   松开时：若这一轮是我们开的、且按住时间 >= short_press_ms -> 收镜（长按松开 = 收）；
 *           否则保持（= 短按切换开）。
 * 鼠标和键盘都监听，所以开镜键绑到键盘也能用。
 *
 * TaCZ 自己那套开镜键处理由 AimKeyMixin 拦掉；如果拦截没生效，
 * 这里会直接降级退出（避免两边同时切来切去），并在日志里警告。
 */
@Mod.EventBusSubscriber(modid = TaczOptimizeMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HybridAimHandler {

    private HybridAimHandler() {
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Post event) {
        if (!HybridAimConfig.enabled) {
            return;
        }
        KeyMapping key = AimKey.AIM_KEY;
        if (key == null || !key.m_90830_(event.getButton())) {
            return;
        }
        handle(event.getAction() == GLFW.GLFW_PRESS);
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (!HybridAimConfig.enabled) {
            return;
        }
        KeyMapping key = AimKey.AIM_KEY;
        if (key == null || !key.m_90832_(event.getKey(), event.getScanCode())) {
            return;
        }
        int action = event.getAction();
        if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_RELEASE) {
            return; // 忽略长按重复事件
        }
        handle(action == GLFW.GLFW_PRESS);
    }

    private static void handle(boolean press) {
        HybridAimConfig.reloadIfChanged();
        if (!HybridAimConfig.enabled) {
            HybridAimState.reset();
            return;
        }
        if (!HybridAimState.suppressionSeen()) {
            // TaCZ 自己的处理还在，别抢输入，否则会两边各切一次
            HybridAimLog.fallback();
            return;
        }
        if (!InputExtraCheck.isInGame()) {
            HybridAimState.reset();
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft == null ? null : minecraft.f_91074_;
        if (player == null || player.m_5833_()) {
            HybridAimState.reset();
            return;
        }
        if (!IGun.mainHandHoldGun(player)) {
            HybridAimState.reset();
            return;
        }
        IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(player);
        if (operator == null) {
            return;
        }
        ItemStack main = player.m_21205_();
        IGun gun = IGun.getIGunOrNull(main);
        if (gun == null) {
            return;
        }
        ResourceLocation gunId = gun.getGunId(main);
        if (gunId == null || !HybridAimConfig.applies(gunId.toString())) {
            return;
        }

        long now = System.currentTimeMillis();
        if (press) {
            if (HybridAimState.isPressed()) {
                return;
            }
            HybridAimState.setPressed(true);
            HybridAimState.setPressStartMs(now);
            HybridAimState.setOpenedByPress(false);
            if (operator.isAim()) {
                operator.aim(false);
            } else {
                operator.aim(true);
                HybridAimState.setOpenedByPress(true);
            }
        } else {
            if (!HybridAimState.isPressed()) {
                return;
            }
            HybridAimState.setPressed(false);
            long held = now - HybridAimState.pressStartMs();
            boolean wasOpened = HybridAimState.isOpenedByPress();
            HybridAimState.setOpenedByPress(false);
            if (wasOpened && held >= HybridAimConfig.shortPressMs && operator.isAim()) {
                operator.aim(false);
            }
        }
    }
}