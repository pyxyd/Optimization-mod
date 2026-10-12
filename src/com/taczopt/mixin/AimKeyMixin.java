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

/**
 * 把 TaCZ 自己的开镜键处理拦下来（本模组要独占开镜键，否则两边各切一次会互相抵消）：
 *   onAimPress          —— 鼠标按下/松开（切换模式只认鼠标，就是官方 TODO 说的那个问题）
 *   onAimHoldingPreInput —— 按住模式每 tick 的按键同步
 * 生效时打一条日志作为正面证据；顺带用 markSuppressionSeen 让处理器知道可以接管输入了。
 */
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