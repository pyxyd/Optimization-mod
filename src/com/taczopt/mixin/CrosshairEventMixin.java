package com.taczopt.mixin;

import com.tacz.guns.client.event.RenderCrosshairEvent;
import com.taczopt.crosshair.CrosshairStyleClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderCrosshairEvent.class, remap = false)
public class CrosshairEventMixin {

    @Inject(method = "onRenderOverlay", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void taczopt$replaceCrosshair(RenderGuiOverlayEvent.Pre event, CallbackInfo ci) {
        try {

            if (!event.getOverlay().id().equals(
                    net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.CROSSHAIR.id())) {
                return;
            }
            LocalPlayer player = Minecraft.m_91087_().f_91074_;
            if (player == null) {
                return;
            }

            if (!CrosshairStyleClient.shouldTakeOver()) {
                return;
            }
            ci.cancel();
        } catch (Throwable ignored) {

        }
    }
}
