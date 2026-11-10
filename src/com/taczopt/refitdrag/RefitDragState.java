package com.taczopt.refitdrag;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;

/**
 * Accumulated rotation while the player drags in the refit screen.
 *
 * The mouse position is fed in once per frame by RefitDragHandler (Forge's
 * ScreenEvent.Render.Pre carries the cursor), so this class never touches input APIs.
 */
public final class RefitDragState {

    private static final double MIN_TRAVEL = 3.0D;

    private static float yaw = 0F;
    private static float pitch = 0F;
    private static boolean dragging = false;
    private static double lastX = 0.0D;
    private static double lastY = 0.0D;
    private static double travelled = 0.0D;

    private RefitDragState() {
    }

    public static boolean isDragging() {
        return dragging;
    }

    public static void begin() {
        dragging = true;
        travelled = 0.0D;
    }

    public static void end() {
        dragging = false;
        travelled = 0.0D;
    }

    public static void reset() {
        yaw = 0F;
        pitch = 0F;
        dragging = false;
        travelled = 0.0D;
    }

    /**
     * Called every frame with the cursor position.
     *
     * Rotation only starts after the cursor has travelled a few pixels, so a plain click
     * on a button or an attachment slot does not nudge the gun.
     */
    public static void track(double mouseX, double mouseY) {
        if (!dragging) {
            lastX = mouseX;
            lastY = mouseY;
            return;
        }
        double deltaX = mouseX - lastX;
        double deltaY = mouseY - lastY;
        lastX = mouseX;
        lastY = mouseY;
        travelled += Math.abs(deltaX) + Math.abs(deltaY);
        if (travelled < MIN_TRAVEL) {
            return;
        }
        drag(deltaX, deltaY);
    }

    public static void drag(double deltaX, double deltaY) {
        double signX = RefitDragConfig.invertX ? -1.0D : 1.0D;
        double signY = RefitDragConfig.invertY ? -1.0D : 1.0D;
        // Dragging left must turn the muzzle left. With rotationY the raw delta pointed the
        // other way (mouse left -> muzzle right), so the yaw accumulates negatively.
        yaw = wrap(yaw - (float) (deltaX * RefitDragConfig.sensitivity * signX));
        pitch = pitch + (float) (deltaY * RefitDragConfig.sensitivity * signY);
        float limit = RefitDragConfig.pitchLimit;
        if (pitch > limit) {
            pitch = limit;
        }
        if (pitch < -limit) {
            pitch = -limit;
        }
    }

    public static boolean hasRotation() {
        return yaw != 0F || pitch != 0F;
    }

    /**
     * Applied at the tail of TaCZ's viewmodel transform, where the pose already sits in the
     * gun's own model space -- so the gun turns around itself instead of orbiting the camera.
     * PoseStack.mulPose(Quaternionf) is m_252781_ under SRG and com.mojang.math.Axis is not
     * reliably on the compile classpath here, so the quaternions are built directly.
     */
    public static void apply(PoseStack poseStack) {
        // Yaw and pitch have to be composed into ONE quaternion, yaw first and then pitch
        // around the already-yawed local X. Two consecutive world-axis rotations look like a
        // roll once the gun is turned sideways, which is what made vertical dragging feel wrong.
        // In this pose the barrel runs along X, so rotating about X is a ROLL around the
        // barrel (the gun spins in place like a drill) instead of the muzzle going up or
        // down. Pitch therefore has to use Z -- the axis that is perpendicular to both the
        // barrel and the world up direction.
        Quaternionf rotation = new Quaternionf()
                .rotationY((float) Math.toRadians(yaw))
                .rotateZ((float) Math.toRadians(pitch));
        poseStack.m_252781_(rotation);
    }

    public static String describe() {
        return "yaw=" + Math.round(yaw) + " pitch=" + Math.round(pitch) + " dragging=" + dragging;
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
