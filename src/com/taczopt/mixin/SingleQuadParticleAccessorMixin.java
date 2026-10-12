package com.taczopt.mixin;

import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 1.20.1 里 quadSize 声明在 SingleQuadParticle（SRG: f_107663_），
 * Particle.getQuadSize(float) 返回的就是这个字段，所以改它就能改方块面上原版弹孔的大小。
 */
@Mixin(SingleQuadParticle.class)
public interface SingleQuadParticleAccessorMixin {

    @Accessor("f_107663_")
    void setF_107663_(float value);
}