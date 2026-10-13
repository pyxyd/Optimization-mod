package com.taczopt.mixin;

import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.client.gameplay.LocalPlayerShoot;
import com.taczopt.adsfire.AdsFireConfig;
import com.taczopt.adsfire.AdsFireState;
import com.taczopt.TaczOptimizeMod;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayerShoot.class)
public class LocalPlayerShootMixin {

    @Shadow
    @Final
    private LocalPlayer player;

    @Inject(method = "shoot", at = @At("HEAD"), cancellable = true)
    private void taczopt$holdFireUntilAimed(CallbackInfoReturnable<ShootResult> cir) {
        if (this.player == null || this.player.m_5833_()) {
            return;
        }

        if (AdsFireState.isActive() && !AdsFireState.hasFired()) {
            if (AdsFireConfig.debugLog()) {
                TaczOptimizeMod.logger().info("[taczopt] 拦下一次开火（等开镜走完）。来源：{}", caller());
            }
            cir.setReturnValue(ShootResult.FORGE_EVENT_CANCEL);
        }
    }

    private static String caller() {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cn = st[i].getClassName();
            if (!cn.startsWith("com.taczopt") && !cn.equals("com.tacz.guns.client.gameplay.LocalPlayerShoot")) {
                return cn + "#" + st[i].getMethodName() + ":" + st[i].getLineNumber();
            }
        }
        return "unknown";
    }
}
