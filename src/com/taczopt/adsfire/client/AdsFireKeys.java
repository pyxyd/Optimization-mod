package com.taczopt.adsfire.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.taczopt.TaczOptimizeMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = TaczOptimizeMod.MOD_ID, value = Dist.CLIENT)
public final class AdsFireKeys {

    public static final String CATEGORY = "key.category.taczadsfire";

    public static final KeyMapping PANEL_KEY = new KeyMapping(
            "key.taczadsfire.panel",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            CATEGORY);

    private AdsFireKeys() {
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(PANEL_KEY);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PANEL_KEY == null) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        if (mc == null || mc.f_91074_ == null) {
            return;
        }
        while (PANEL_KEY.m_90859_()) {
            if (mc.f_91080_ == null) {
                mc.m_91152_(new AdsFireScreen());
            }
        }
    }
}
