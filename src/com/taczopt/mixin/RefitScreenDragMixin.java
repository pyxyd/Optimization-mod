package com.taczopt.mixin;

import com.tacz.guns.client.gui.GunRefitScreen;
import com.taczopt.refitdrag.RefitDragConfig;
import com.taczopt.refitdrag.RefitDragState;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Drag the gun around in the refit screen.
 *
 * GunRefitScreen overrides no mouse method, so the capture lives on Screen and is gated on
 * the open screen being a GunRefitScreen. A press only starts a drag when nothing consumed
 * it -- injecting at RETURN and checking the result means every widget keeps working without
 * duplicating Screen's hit testing.
 *
 * Method selectors are m_* because this mod compiles against SRG names.
 */
@Mixin(Screen.class)
public abstract class RefitScreenDragMixin {

    @Inject(method = "m_6375_", at = @At("RETURN"), cancellable = true)
    private void taczopt$beginRefitDrag(double mouseX, double mouseY, int button,
                                        CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() != null && cir.getReturnValue()) {
            return;
        }
        if (button != 0 || !((Object) this instanceof GunRefitScreen)) {
            return;
        }
        RefitDragConfig.ensureLoaded();
        if (!RefitDragConfig.enabled) {
            return;
        }
        RefitDragState.begin();
        cir.setReturnValue(Boolean.TRUE);
    }

    @Inject(method = "m_7979_", at = @At("HEAD"), cancellable = true)
    private void taczopt$updateRefitDrag(double mouseX, double mouseY, int button,
                                         double dragX, double dragY,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (!RefitDragState.isDragging() || !((Object) this instanceof GunRefitScreen)) {
            return;
        }
        RefitDragState.drag(dragX, dragY);
        cir.setReturnValue(Boolean.TRUE);
    }

    @Inject(method = "m_6348_", at = @At("HEAD"))
    private void taczopt$endRefitDrag(double mouseX, double mouseY, int button,
                                      CallbackInfo ci) {
        if (RefitDragState.isDragging()) {
            RefitDragState.end();
        }
    }
}
