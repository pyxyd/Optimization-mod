package com.taczopt.mixin;

import com.tacz.guns.client.gui.GunRefitScreen;
import com.taczopt.refitdrag.RefitDragConfig;
import com.taczopt.refitdrag.RefitDragState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every time the refit screen is (re)built -- opening it, switching attachment page,
 * refitting -- reload the drag settings and optionally put the gun back to its default angle.
 * init() is m_7856_ under SRG.
 */
@Mixin(GunRefitScreen.class)
public abstract class GunRefitScreenInitMixin {

    @Inject(method = "m_7856_", at = @At("TAIL"))
    private void taczopt$resetRefitDrag(CallbackInfo ci) {
        RefitDragConfig.reloadIfChanged();
        com.taczopt.TaczOptimizeMod.logger().info("[taczopt] refit screen opened, drag={} ({})",
                RefitDragConfig.enabled, com.taczopt.refitdrag.RefitDragState.describe());
        if (RefitDragConfig.resetOnOpen) {
            RefitDragState.reset();
        }
    }
}
