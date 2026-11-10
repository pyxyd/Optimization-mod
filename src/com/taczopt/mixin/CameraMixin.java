package com.taczopt.mixin;

import com.taczopt.lean.LeanConfig;
import com.taczopt.lean.LeanState;
import net.minecraft.client.Camera;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shifts the camera sideways for the peek.
 *
 * Camera.setup is m_90575_; at its tail the position is final, so the lateral offset is added
 * there. The right vector for a Minecraft yaw is (cos yaw, 0, sin yaw).
 *
 * The offset is negated: leaning left (lean = -1) has to move the camera toward the player's
 * left, which is the negative of that right vector.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void m_90581_(Vec3 position);

    @Inject(method = "m_90575_", at = @At("TAIL"))
    private void taczopt$leanOffset(BlockGetter level, Entity entity, boolean detached, boolean thirdPerson,
                                    float partialTick, CallbackInfo ci) {
        if (!LeanConfig.enabled) {
            return;
        }
        float lean = LeanState.current();
        if (lean == 0F || LeanConfig.distance <= 0.0D) {
            return;
        }
        Camera self = (Camera) (Object) this;
        double yaw = Math.toRadians(self.m_90589_());
        double offset = -lean * LeanConfig.distance;
        Vec3 base = self.m_90583_();
        this.m_90581_(new Vec3(
                base.f_82479_ + Math.cos(yaw) * offset,
                base.f_82480_,
                base.f_82481_ + Math.sin(yaw) * offset));
    }
}
