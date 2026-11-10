package com.taczopt.mixin;

import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.taczopt.lean.LeanConfig;
import com.taczopt.lean.LeanState;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Moves the BULLET SPAWN with the peek.
 *
 * TaCZ builds the bullet at throwerIn.getX(), throwerIn.getEyeY() - 0.1, throwerIn.getZ()
 * (EntityKineticBullet line 172) -- it never calls getEyePosition(), which is why offsetting the
 * eye position changed nothing about where the shot came from.
 *
 * The clean hook is the bullet itself: at the end of its constructor, shift the freshly placed
 * bullet sideways by the same offset the camera uses (CameraMixin). The direction is left alone
 * (it comes from the shooter's pitch/yaw via doBulletSpread), so the shot still runs along the
 * crosshair -- only the origin moves.
 */
@Mixin(EntityKineticBullet.class)
public abstract class BulletSpawnMixin {

    @Inject(
            method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;ZLcom/tacz/guns/resource/pojo/data/gun/GunData;Lcom/tacz/guns/resource/pojo/data/gun/BulletData;)V",
            at = @At("TAIL"))
    private void taczopt$leanSpawn(Level world, LivingEntity thrower, ItemStack gunItem, ResourceLocation ammoId,
                                   ResourceLocation gunId, ResourceLocation gunDisplayId, boolean isTracer,
                                   GunData gunData, BulletData bulletData, CallbackInfo ci) {
        if (!LeanConfig.enabled) {
            return;
        }
        float lean = LeanState.current();
        if (lean == 0F || LeanConfig.distance <= 0.0D) {
            return;
        }
        if (thrower == null || !taczopt$isLocalPlayer(thrower)) {
            return;
        }
        double yaw = Math.toRadians(thrower.m_146908_());
        double offset = -lean * LeanConfig.distance;
        Entity self = (Entity) (Object) this;
        Vec3 pos = self.m_20182_();
        if (pos == null) {
            return;
        }
        self.m_146884_(new Vec3(
                pos.f_82479_ + Math.cos(yaw) * offset,
                pos.f_82480_,
                pos.f_82481_ + Math.sin(yaw) * offset));
    }

    private static boolean taczopt$isLocalPlayer(LivingEntity entity) {
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