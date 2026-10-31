package com.taczfeel.mixin;

import com.tacz.guns.client.sound.GunSoundInstance;
import com.taczfeel.FeelConfig;
import com.taczfeel.client.FeelState;
import com.taczfeel.client.HookLog;
import com.taczfeel.client.SoundJudge;
import com.taczfeel.client.SoundPitch;
import com.taczfeel.client.TailSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 挂到 TaCZ 的声音实例上：它每造一条音效，我们就判断这是不是"枪声"，
 * 是的话给音高加一点随机（连发不再像复读机），并把这条声音排进尾音队列。
 * <p>
 * 目标构造器：
 * (SoundEvent, SoundSource, float volume, float pitch, Entity, int distance,
 *  ResourceLocation registryName, boolean flagA, boolean flagB)
 * 其中 volume / pitch 就是最终用的音量与音高（已在字节码里核对过）。
 */
@Mixin(value = GunSoundInstance.class, remap = false)
public class GunSoundInstanceMixin {

    private static final String CTOR = "<init>(Lnet/minecraft/sounds/SoundEvent;"
            + "Lnet/minecraft/sounds/SoundSource;FFLnet/minecraft/world/entity/Entity;"
            + "ILnet/minecraft/resources/ResourceLocation;ZZ)V";

    @Inject(method = CTOR, at = @At("TAIL"), remap = false)
    private void taczfeel$afterInit(SoundEvent sound, SoundSource source, float volume, float pitch,
                                    Entity entity, int distance, ResourceLocation registryName,
                                    boolean flagA, boolean flagB, CallbackInfo ci) {
        // 我们自己造尾音时不再递归
        if (TailSounds.isBuilding() || entity == null || registryName == null) {
            return;
        }
        if (!FeelConfig.soundEnabled) {
            return;
        }
        if (!SoundJudge.isShot(registryName)) {
            return;
        }
        HookLog.soundOnce("hooked GunSoundInstance ctor, last=" + registryName);

        // 1) 音高随机
        if (FeelConfig.soundPitchRandom > 0.0001F) {
            float factor = 1.0F + (ThreadLocalRandom.current().nextFloat() * 2.0F - 1.0F)
                    * FeelConfig.soundPitchRandom;
            SoundPitch.set(this, pitch * factor);
        }

        // 2) 尾音叠层
        TailSounds.onShotSound(sound, source, volume, pitch, entity, distance, registryName, flagA);
    }
}
