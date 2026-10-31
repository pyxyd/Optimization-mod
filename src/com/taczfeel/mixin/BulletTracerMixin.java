package com.taczfeel.mixin;

import com.tacz.guns.entity.EntityKineticBullet;
import com.taczfeel.FeelConfig;
import com.taczfeel.client.HookLog;
import com.taczfeel.client.TracerColor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * 曳光弹。
 * <p>
 * TaCZ 本身就有曳光渲染（EntityBulletRenderer.renderTracerAmmo），只是默认只有
 * 曳光弹种（isTracerAmmo）才画。这里做三件事，全部走 TaCZ 自己的渲染路径，
 * 不碰任何 GL 状态，所以和 Embeddium / 光影包的兼容性和原生曳光完全一样：
 * <ol>
 *   <li>isTracerAmmo 强制为 true —— 每发子弹都画曳光</li>
 *   <li>getTracerColorOverride 在枪包/NBT 都没给颜色时补一个颜色</li>
 *   <li>getTracerSizeOverride 乘一个粗细倍数</li>
 * </ol>
 * 三个注入全部只在客户端生效，服务端弹道逻辑一个字节都不改。
 */
@Mixin(value = EntityKineticBullet.class, remap = false)
public class BulletTracerMixin {

    @Inject(method = "isTracerAmmo", at = @At("HEAD"), cancellable = true, remap = false)
    private void taczfeel$forceTracer(CallbackInfoReturnable<Boolean> cir) {
        if (!FeelConfig.tracerEnabled) {
            return;
        }
        try {
            if (!isClientSide()) {
                return;
            }
            if (FeelConfig.tracerOnlySelf) {
                Entity owner = ((EntityKineticBullet) (Object) this).m_19749_();
                Minecraft mc = Minecraft.m_91087_();
                if (owner != null && mc != null && owner != mc.f_91074_) {
                    return;
                }
            }
            if (Boolean.FALSE.equals(cir.getReturnValue())) {
                HookLog.tracerOnce("forced tracer for bullet");
            }
            cir.setReturnValue(true);
        } catch (Throwable ignored) {
            // 取值失败就保持 TaCZ 原判
        }
    }

    @Inject(method = "getTracerColorOverride", at = @At("RETURN"), cancellable = true, remap = false)
    private void taczfeel$color(CallbackInfoReturnable<Optional<float[]>> cir) {
        if (!FeelConfig.tracerEnabled || !FeelConfig.tracerForceColor) {
            return;
        }
        Optional<float[]> original = cir.getReturnValue();
        if (original != null && original.isPresent()) {
            return; // NBT 里显式指定的颜色优先
        }
        float[] color = TracerColor.parse(FeelConfig.tracerColor);
        if (color == null) {
            return;
        }
        cir.setReturnValue(Optional.of(color));
    }

    @Inject(method = "getTracerSizeOverride", at = @At("RETURN"), cancellable = true, remap = false)
    private void taczfeel$size(CallbackInfoReturnable<Float> cir) {
        if (!FeelConfig.tracerEnabled) {
            return;
        }
        float mul = FeelConfig.tracerSize;
        if (mul <= 0.0001F || Math.abs(mul - 1.0F) < 0.0001F) {
            return;
        }
        float original = cir.getReturnValueF();
        if (original <= 0.0001F) {
            original = 1.0F;
        }
        cir.setReturnValue(Math.max(0.05F, original * mul));
    }

    private boolean isClientSide() {
        Level level = ((EntityKineticBullet) (Object) this).m_9236_();
        return level != null && level.m_5776_();
    }
}
