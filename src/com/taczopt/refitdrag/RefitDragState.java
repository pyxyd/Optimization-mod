package com.taczopt.refitdrag;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;

/** Accumulated rotation while the player drags in the refit screen. */
public final class RefitDragState {

    private static float yaw = 0F;
    private static float pitch = 0F;
    private static boolean dragging = false;

    private RefitDragState() {
    }

    public static boolean isDragging() {
        return dragging;
    }

    public static void begin() {
        dragging = true;
    }

    public static void end() {
        dragging = false;
    }

    public static void drag(double deltaX, double deltaY) {
        double signX = RefitDragConfig.invertX ? -1.0D : 1.0D;
        double signY = RefitDragConfig.invertY ? -1.0D : 1.0D;
        yaw = wrap(yaw + (float) (deltaX * RefitDragConfig.sensitivity * signX));
        pitch = pitch + (float) (deltaY * RefitDragConfig.sensitivity * signY);
        float limit = RefitDragConfig.pitchLimit;
        if (pitch > limit) {
            pitch = limit;
        }
        if (pitch < -limit) {
            pitch = -limit;
        }
    }

    public static void reset() {
        yaw = 0F;
        pitch = 0F;
        dragging = false;
    }

    public static boolean hasRotation() {
        return yaw != 0F || pitch != 0F;
    }

    /**
     * Applied at the tail of TaCZ's viewmodel transform, where the pose already sits in the
     * gun's own model space -- so the gun turns around itself instead of orbiting the camera.
     * PoseStack.mulPose(Quaternionf) is m_252781_ under SRG, and com.mojang.math.Axis is not
     * reliably on the compile classpath here, so the quaternion is built directly.
     */
    public static void apply(PoseStack poseStack) {
        if (pitch != 0F) {
            poseStack.m_252781_(new Quaternionf().rotationX((float) Math.toRadians(pitch)));
        }
        if (yaw != 0F) {
            poseStack.m_252781_(new Quaternionf().rotationY((float) Math.toRadians(yaw)));
        }
    }

    private static float wrap(float angle) {
        float wrapped = angle % 360F;
        if (wrapped > 180F) {
            wrapped -= 360F;
        }
        if (wrapped < -180F) {
            wrapped += 360F;
        }
        return wrapped;
    }
}
