package com.taczopt.mixin;

import com.tacz.guns.client.input.ShootKey;
import com.taczopt.adsfire.AdsFireConfig;
import com.taczopt.adsfire.AdsFireState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 记录"玩家正按着左键"这件事，供会话在松手后计时收镜。
 *
 * <p>注意：这里**不再**负责拦开火。拦截全部交给
 * {@link LocalPlayerShootMixin}（拦在 shoot() 上，和 tick 顺序无关）。
 * 早期版本在 autoShoot 的 HEAD 里 cancel，因为 autoShoot 和本模组的 tick 处理器
 * 监听同一个事件、顺序不可控，会出现"autoShoot 先跑 -> 直接开火"的漏网情况。
 */
@Mixin(ShootKey.class)
public class ShootKeyMixin {

    @Shadow
    @Final
    public static KeyMapping SHOOT_KEY;

    @Inject(method = "autoShoot", at = @At("HEAD"))
    private static void taczopt$noteShootDown(TickEvent.ClientTickEvent event, CallbackInfo ci) {
        if (event == null || event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!AdsFireConfig.enabled() || SHOOT_KEY == null || !SHOOT_KEY.m_90857_()) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        LocalPlayer player = mc == null ? null : mc.f_91074_;
        if (player == null) {
            return;
        }
        AdsFireState.markShootDown(player.f_19797_);
    }
}
