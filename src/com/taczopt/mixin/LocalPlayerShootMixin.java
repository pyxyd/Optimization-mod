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

/**
 * 真正拦下"还没开镜就打出去"的那一发。
 *
 * <p>为什么拦这里而不是拦 ShootKey：TaCZ 的 {@code ShootKey.autoShoot} 和本模组的 tick 处理器
 * 都监听同一个 {@code TickEvent.ClientTickEvent}，谁先跑由事件总线的注册顺序决定，不可控。
 * 如果 autoShoot 先跑，它就直接 shoot() 了，那边无论怎么改都来不及开镜。
 *
 * <p>而 TaCZ 客户端所有开火（左键、蓄力枪、其它模组调用）最终都必须经过
 * {@code LocalPlayerShoot.shoot()}。拦在这里就是拦在唯一的必经之路上，和 tick 顺序无关。
 *
 * <p>返回 {@link ShootResult#FORGE_EVENT_CANCEL}：它不等于 SUCCESS，所以 ShootKey 不会把
 * lastTimeShootSuccess 置真（下一 tick 还会重试），玩家那边只是"这一发被推迟了"。
 */
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
        // 会话进行中、但这一发还没打出去 -> 现在不许开火，等开镜到位
        if (AdsFireState.isActive() && !AdsFireState.hasFired()) {
            if (AdsFireConfig.debugLog()) {
                TaczOptimizeMod.logger().info("[taczopt] 拦下一次开火（等开镜走完）。来源：{}", caller());
            }
            cir.setReturnValue(ShootResult.FORGE_EVENT_CANCEL);
        }
    }

    /**
     * debug 用：找出是谁触发的 shoot()。
     * 必须跳过本模组自己的注入方法和 TaCZ 的 shoot/preCheck 内部帧，
     * 否则打出来的永远是 LocalPlayerShoot#caller 这种没用的信息。
     */
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
