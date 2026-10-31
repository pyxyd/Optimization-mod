package com.taczfeel.mixin;

import com.tacz.guns.client.resource.pojo.display.gun.MuzzleFlash;
import com.taczfeel.FeelConfig;
import com.taczfeel.client.HookLog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 枪口火光放大：TaCZ 的枪焰尺寸完全由 MuzzleFlash.getScale() 决定
 * （渲染时是 0.5 * scale * 时间淡出），所以在这里乘一个倍数就够了，
 * 不用碰任何渲染状态，也不会影响枪模本身的大小。
 */
@Mixin(value = MuzzleFlash.class, remap = false)
public class MuzzleFlashScaleMixin {

    @Inject(method = "getScale", at = @At("RETURN"), cancellable = true, remap = false)
    private void taczfeel$scale(CallbackInfoReturnable<Float> cir) {
        if (!FeelConfig.muzzleEnabled) {
            return;
        }
        float k = FeelConfig.muzzleScale;
        if (k <= 0.0001F || Math.abs(k - 1.0F) < 0.0001F) {
            return;
        }
        HookLog.muzzleOnce("hooked MuzzleFlash.getScale, scale=" + k);
        cir.setReturnValue(cir.getReturnValueF() * k);
    }
}
