package com.taczopt.mixin;

import com.tacz.guns.client.gameplay.LocalPlayerAim;
import com.taczopt.adsfire.AdsFireState;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayerAim.class)
public class LocalPlayerAimMixin {

    @Shadow
    @Final
    private LocalPlayer player;

    @Inject(method = "aim", at = @At("HEAD"), cancellable = true)
    private void taczopt$blockReleaseWhileAdsFire(boolean aiming, CallbackInfo ci) {
        if (!aiming && AdsFireState.isActive() && AdsFireState.isAutoAimed()
                && this.player != null && !this.player.m_5833_()) {
            ci.cancel();
        }
    }
}
