package com.taczopt;

import com.taczopt.adsfire.client.AdsFireKeys;
import com.taczopt.assist.AimAssistHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Registers every key mapping of this mod.
 *
 * This has to live on its own class with Bus.MOD: RegisterKeyMappingsEvent is a mod-bus event,
 * so the handlers that also listen to forge-bus events (ClientTickEvent) can never receive it.
 * That is why the aim-assist key (and the adsfire panel key before it) did nothing -- the
 * KeyMapping existed but was never handed to the game.
 */
@Mod.EventBusSubscriber(modid = TaczOptimizeMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class KeyRegistration {

    private static boolean announced = false;

    private KeyRegistration() {
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(AimAssistHandler.ASSIST_KEY);
        event.register(AdsFireKeys.PANEL_KEY);
        if (!announced) {
            announced = true;
            TaczOptimizeMod.logger().info("[taczopt] key mappings registered (aim assist + adsfire panel)");
        }
    }
}