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
 * Moves the EYE POSITION for the peek, not just the camera.
 *
 * Shifting the camera alone only changes what you see; TaCZ spawns the bullet from
 * shooter.getEyePosition() (ModernKineticGunItem) and the crosshair raycast starts there too.
 * Offsetting the eye position moves the camera (Camera.setup reads it), the aim ray and -- in
 * singleplayer, where the integrated server runs this same class -- the bullet spawn, so the
 * shot really does leave from the leaned spot.
 *
 * getEyePosition(float) is m_20252_ and it is final; getEyePosition() delegates to it, so one
 * injection covers both call styles.
 *
 * Only the local player is affected: on a dedicated server without this mod the eye stays put
 * and bullets come from the body, which is the same limitation a client-side camera-only lean
 * would have had.
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
        // In singleplayer the integrated server holds a different instance of the same player;
        // matching on the UUID keeps the bullet spawn aligned with the client's eye.
        return entity.m_20148_().equals(minecraft.f_91074_.m_20148_());
    }
}
