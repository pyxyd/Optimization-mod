package com.taczopt.mixin;

import com.tacz.guns.client.input.AimKey;
import com.taczopt.hybridaim.HybridAimConfig;
import com.taczopt.hybridaim.HybridAimLog;
import com.taczopt.hybridaim.HybridAimState;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AimKey.class)
public class AimKeyMixin {

    @Inject(method = "onAimPress", at = @At("HEAD"), cancellable = true)
    private static void taczopt$suppressMouse(InputEvent.MouseButton.Post event, CallbackInfo ci) {
        if (HybridAimConfig.enabled) {
            HybridAimState.markSuppressionSeen();
            HybridAimLog.suppressed();
            ci.cancel();
        }
    }

    @Inject(method = "onAimHoldingPreInput", at = @At("HEAD"), cancellable = true)
    private static void taczopt$suppressHoldTick(TickEvent.ClientTickEvent event, CallbackInfo ci) {
        if (HybridAimConfig.enabled) {
            HybridAimState.markSuppressionSeen();
            ci.cancel();
        }
    }
}
