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
