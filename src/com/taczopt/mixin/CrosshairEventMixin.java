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

/**
 * 把 TaCZ 自己的准星拦下来，交给本模组的腰射准星。
 *
 * <h2>为什么必须用 Mixin，不能只 cancel 事件</h2>
 * TaCZ 的准星挂在<b>同一个</b>原版 overlay 上
 * （{@code VanillaGuiOverlay.CROSSHAIR} 的 {@code Pre}），而且它
 * <b>不检查事件有没有被取消</b> —— 直接画。所以光在
 * {@code RenderGuiOverlayEvent.Pre} 里 {@code setCanceled(true)}
 * 只能拦住原版准星，拦不住 TaCZ 那张贴图，结果两个准星会叠在一起。
 *
 * <h2>注入点为什么在 HEAD 就 cancel</h2>
 * TaCZ 的 {@code onRenderOverlay} 结构是：
 * <pre>
 *   if (overlay != CROSSHAIR) return;
 *   player = mc.player; if (player == null) return;
 *   if (!IGun.mainHandHoldGun(player)) return;   // 手上没枪就什么都不做
 *   event.setCanceled(true);                     // 拦原版准星
 *   ...
 * </pre>
 * 也就是说它的准入条件（"是原版准星 + 手上有枪"）正好是
 * {@link CrosshairStyleClient#shouldTakeOver()} 的条件。所以在 HEAD
 * 判断一次、满足就整个取消，比想办法只切掉其中一段更稳 ——
 * 那种做法要依赖 javac 生成的局部变量表（{@code istore}），换个版本就失效。
 *
 * <h2>代价（明确记下来）</h2>
 * 整个方法被取消，意味着 <b>TaCZ 自带的命中标记（四角箭头）也一起不画了</b>。
 * 本模组自己有命中标记 {X / 双 X}，所以这是想要的效果；
 * 而且 {@code crosshair.enabled=false} 时本 mixin 完全不介入，
 * TaCZ 的准星和箭头都会原样回来。
 *
 * <p>{@code require = 0}：TaCZ 换版本改了方法名也不能把游戏搞崩 ——
 * 注入不上就退化成"两个准星叠着"，而不是启动失败。
 */
@Mixin(value = RenderCrosshairEvent.class, remap = false)
public class CrosshairEventMixin {

    @Inject(method = "onRenderOverlay", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void taczopt$replaceCrosshair(RenderGuiOverlayEvent.Pre event, CallbackInfo ci) {
        try {
            // 只处理原版准星那个 overlay（TaCZ 也只监听它）
            if (!event.getOverlay().id().equals(
                    net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.CROSSHAIR.id())) {
                return;
            }
            LocalPlayer player = Minecraft.m_91087_().f_91074_;
            if (player == null) {
                return;
            }
            // TaCZ 的准入条件是"是原版准星 + 手上有枪"，本模组的接管条件
            // （CrosshairStyleClient.shouldTakeOver）正好覆盖它并额外考虑了开镜隐藏。
            if (!CrosshairStyleClient.shouldTakeOver()) {
                return;
            }
            ci.cancel();
        } catch (Throwable ignored) {
            // 准星替换失败绝不能影响游戏
        }
    }
}
