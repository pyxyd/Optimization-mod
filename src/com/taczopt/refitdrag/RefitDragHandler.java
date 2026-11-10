package com.taczopt.refitdrag;

import com.tacz.guns.client.gui.GunRefitScreen;
import com.taczopt.TaczOptimizeMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Mouse capture for the refit-screen drag, done with Forge events instead of a mixin.
 *
 * Screen does not declare mouseClicked/mouseDragged/mouseReleased -- they are default methods
 * on GuiEventListener -- so a mixin targeting Screen cannot inject into them, and because the
 * mixin config runs with defaultRequire 0 that failure is silent. InputEvent gives us
 * press/release, and ScreenEvent.Render.Pre carries the cursor every frame, which is all the
 * drag needs.
 */
@Mod.EventBusSubscriber(modid = TaczOptimizeMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RefitDragHandler {

    private static boolean announced = false;

    private RefitDragHandler() {
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Post event) {
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft == null || !(minecraft.f_91080_ instanceof GunRefitScreen)) {
            return;
        }
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        RefitDragConfig.reloadIfChanged();
        if (!RefitDragConfig.enabled) {
            return;
        }
        if (!announced) {
            announced = true;
            TaczOptimizeMod.logger().info("[taczopt] refit drag active: sensitivity={} pitch_limit={}",
                    RefitDragConfig.sensitivity, RefitDragConfig.pitchLimit);
        }
        if (event.getAction() == GLFW.GLFW_PRESS) {
            RefitDragState.begin();
        } else if (event.getAction() == GLFW.GLFW_RELEASE) {
            if (RefitDragState.isDragging()) {
                TaczOptimizeMod.logger().info("[taczopt] refit drag released: {}", RefitDragState.describe());
            }
            RefitDragState.end();
        }
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof GunRefitScreen)) {
            return;
        }
        if (!RefitDragConfig.enabled) {
            return;
        }
        RefitDragState.track(event.getMouseX(), event.getMouseY());
    }
}
