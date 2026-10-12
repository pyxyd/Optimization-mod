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

/**
 * 会话期间不许别人把镜收掉。
 *
 * <p>TaCZ 在 HoldToAim = true 时，AimKey.onAimHoldingPreInput 每个客户端 tick 都会按开镜键状态
 * 同步开镜 —— 我们请求开镜时玩家并没按开镜键，下一 tick 就会被它 aim(false) 撤掉。
 * 所以会话进行期间把收镜请求拦下来；我们自己结束会话时会先清掉标记再收镜，不会被自己拦住。
 */
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
