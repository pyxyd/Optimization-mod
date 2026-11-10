package com.taczopt.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.event.FirstPersonRenderGunEvent;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.client.model.BedrockGunModel;
import com.taczopt.refitdrag.RefitDragConfig;
import com.taczopt.refitdrag.RefitDragState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the drag rotation to the viewmodel, but only while the refit screen is open, so
 * normal first-person rendering is untouched. Minecraft.getInstance() is m_91087_ and the
 * screen field is f_91080_ under SRG.
 */
@Mixin(FirstPersonRenderGunEvent.class)
public abstract class FirstPersonGunTransformMixin {

    private static boolean logged = false;
    private static boolean appliedLog = false;

    @Inject(method = "applyFirstPersonGunTransform", at = @At("TAIL"))
    private static void taczopt$rotateGunInRefitScreen(LocalPlayer player, ItemStack gunItemStack,
                                                       PoseStack poseStack, BedrockGunModel model,
                                                       float partialTicks, CallbackInfo ci) {
        if (!RefitDragConfig.enabled) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft == null || !(minecraft.f_91080_ instanceof GunRefitScreen)) {
            return;
        }
        if (!logged) {
            logged = true;
            com.taczopt.TaczOptimizeMod.logger().info("[taczopt] refit render hook reached");
        }
        if (RefitDragState.hasRotation()) {
            RefitDragState.apply(poseStack);
            if (!appliedLog) {
                appliedLog = true;
                com.taczopt.TaczOptimizeMod.logger().info("[taczopt] refit rotation applied: {}", RefitDragState.describe());
            }
        }
    }
}
