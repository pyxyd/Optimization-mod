package com.taczopt.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Particle 里的字段（SRG 名从已安装的 jar 反汇编确认）：
 *   f_107209_ = xo, f_107210_ = yo, f_107211_ = zo
 *   f_107212_ = x,  f_107213_ = y,  f_107214_ = z
 *   f_107224_ = age
 * 1.20.1 里 x/y/z 是 f_107212_..f_107214_，已用 Particle 构造函数字节码（dload_1 -> f_82479_ 等）核对。
 * 注意：Mixin 要求 accessor 方法名以 get/set/is 开头。
 */
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