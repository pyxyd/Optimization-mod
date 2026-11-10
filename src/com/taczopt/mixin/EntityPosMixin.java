package com.taczopt.mixin;

import com.taczopt.lean.LeanConfig;
import com.taczopt.lean.LeanState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Peek by displacing the ENTITY, the way the reference mod GD656Peek does it.
 *
 * Moving the camera alone changes only the picture, and moving the eye position alone changes
 * aim and bullets but not the camera -- Camera.setup builds its position from getX/getY/getZ
 * plus getEyeY, so it never reads getEyePosition. Displacing the entity coordinates instead is
 * a single source of truth: the camera follows (getX/getY/getZ), the eye position follows
 * (getEyePosition interpolates getX/getZ), the crosshair ray follows, TaCZ spawns the bullet
 * from the same eye position, and the third-person model is drawn at the shifted spot too.
 *
 * getX is m_20185_, getZ is m_20189_, position() is m_20182_. Only the local player is shifted,
 * matched by UUID so the integrated server's copy of that player moves as well.
 */
@Mixin(Entity.class)
public abstract class EntityPosMixin {

    @Inject(method = "m_20185_", at = @At("RETURN"), cancellable = true)
    private void taczopt$peekX(CallbackInfoReturnable<Double> cir) {
        Double offset = taczopt$offsetX((Entity) (Object) this);
        if (offset != null && cir.getReturnValue() != null) {
            cir.setReturnValue(cir.getReturnValue() + offset);
        }
    }

    @Inject(method = "m_20189_", at = @At("RETURN"), cancellable = true)
    private void taczopt$peekZ(CallbackInfoReturnable<Double> cir) {
        Double offset = taczopt$offsetZ((Entity) (Object) this);
        if (offset != null && cir.getReturnValue() != null) {
            cir.setReturnValue(cir.getReturnValue() + offset);
        }
    }

    @Inject(method = "m_20182_", at = @At("RETURN"), cancellable = true)
    private void taczopt$peekPosition(CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        Double offsetX = taczopt$offsetX(self);
        Double offsetZ = taczopt$offsetZ(self);
        Vec3 base = cir.getReturnValue();
        if (offsetX == null || offsetZ == null || base == null) {
            return;
        }
        cir.setReturnValue(new Vec3(base.f_82479_ + offsetX, base.f_82480_, base.f_82481_ + offsetZ));
    }

    private static double taczopt$leanAmount(Entity entity) {
        if (!LeanConfig.enabled) {
            return 0.0D;
        }
        float lean = LeanState.current();
        if (lean == 0F || LeanConfig.distance <= 0.0D) {
            return 0.0D;
        }
        if (!taczopt$isLocalPlayer(entity)) {
            return 0.0D;
        }
        return -lean * LeanConfig.distance;
    }

    private static Double taczopt$offsetX(Entity entity) {
        double amount = taczopt$leanAmount(entity);
        if (amount == 0.0D) {
            return null;
        }
        return Math.cos(Math.toRadians(entity.m_146908_())) * amount;
    }

    private static Double taczopt$offsetZ(Entity entity) {
        double amount = taczopt$leanAmount(entity);
        if (amount == 0.0D) {
            return null;
        }
        return Math.sin(Math.toRadians(entity.m_146908_())) * amount;
    }

    private static boolean taczopt$isLocalPlayer(Entity entity) {
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft == null || minecraft.f_91074_ == null) {
            return false;
        }
        if (entity == minecraft.f_91074_) {
            return true;
        }
        return entity.m_20148_().equals(minecraft.f_91074_.m_20148_());
    }
}