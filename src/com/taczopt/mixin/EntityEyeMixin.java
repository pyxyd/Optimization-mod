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
 * Moves the EYE POSITION so the peek actually shoots from where you are looking.
 *
 * TaCZ spawns the bullet from shooter.getEyePosition() and the crosshair ray starts there, so the
 * eye has to travel with the camera or the shot would leave from the body while the crosshair is
 * already off to the side. Entity.getX/getZ are deliberately left alone: shifting those made the
 * camera and TaCZ's aim path disagree and the shots went visibly wide.
 */
@Mixin(Entity.class)
public abstract class EntityEyeMixin {

    @Inject(method = "m_20252_", at = @At("RETURN"), cancellable = true)
    private void taczopt$leanEyePosition(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if (!LeanConfig.enabled) {
            return;
        }
        float lean = LeanState.current();
        if (lean == 0F || LeanConfig.distance <= 0.0D) {
            return;
        }
        Entity self = (Entity) (Object) this;
        if (!taczopt$isLocalPlayer(self)) {
            return;
        }
        Vec3 base = cir.getReturnValue();
        if (base == null) {
            return;
        }
        double yaw = Math.toRadians(self.m_146908_());
        double offset = -lean * LeanConfig.distance;
        cir.setReturnValue(new Vec3(
                base.f_82479_ + Math.cos(yaw) * offset,
                base.f_82480_,
                base.f_82481_ + Math.sin(yaw) * offset));
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