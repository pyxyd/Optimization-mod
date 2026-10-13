package com.taczopt.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessorMixin {

    @Accessor("f_107224_")
    int getF_107224_();

    @Accessor("f_107209_")
    double getF_107209_();

    @Accessor("f_107210_")
    double getF_107210_();

    @Accessor("f_107211_")
    double getF_107211_();

    @Accessor("f_107212_")
    double getF_107212_();

    @Accessor("f_107213_")
    double getF_107213_();

    @Accessor("f_107214_")
    double getF_107214_();
}
