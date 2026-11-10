package com.taczopt.lean;

import com.mojang.blaze3d.platform.InputConstants;
import com.taczopt.TaczOptimizeMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Left/right peek.
 *
 * The camera shift itself happens in CameraMixin (Camera.setup tail); this class owns the keys,
 * the eased state and the roll, which rides Forge's ViewportEvent.ComputeCameraAngles.
 *
 * The peek is client-side only: your body stays where it is, so shots still leave from your
 * real position -- it is a viewing aid, not a mechanical advantage the server knows about.
 */
@Mod.EventBusSubscriber(modid = TaczOptimizeMod.MOD_ID, value = Dist.CLIENT)
public final class LeanHandler {

    public static final String CATEGORY = "key.category.taczopt";

    public static final KeyMapping LEAN_LEFT = new KeyMapping(
            "key.taczopt.lean_left",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT,
            CATEGORY);

    public static final KeyMapping LEAN_RIGHT = new KeyMapping(
            "key.taczopt.lean_right",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT,
            CATEGORY);

    private static boolean announced = false;

    private LeanHandler() {
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(LEAN_LEFT);
        event.register(LEAN_RIGHT);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        LeanConfig.ensureLoaded();
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft == null || minecraft.f_91074_ == null || minecraft.f_91080_ != null) {
            LeanState.setTarget(0);
            LeanState.update(1F);
            return;
        }
        if (!LeanConfig.enabled) {
            LeanState.setTarget(0);
            LeanState.update(1F);
            return;
        }
        int want = 0;
        if (LEAN_LEFT != null && LEAN_LEFT.m_90857_()) {
            want -= 1;
        }
        if (LEAN_RIGHT != null && LEAN_RIGHT.m_90857_()) {
            want += 1;
        }
        LeanState.setTarget(want);
        LeanState.update(1F);
        if (!announced && want != 0) {
            announced = true;
            TaczOptimizeMod.logger().info("[taczopt] lean active: distance={} roll={} speed={} ({})",
                    LeanConfig.distance, LeanConfig.rollDegrees, LeanConfig.speed, LeanState.describe());
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!LeanConfig.enabled) {
            return;
        }
        float lean = LeanState.current();
        if (lean == 0F) {
            return;
        }
        event.setRoll(event.getRoll() + (float) (lean * LeanConfig.rollDegrees));
    }
}
