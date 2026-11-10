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
        // Dragging up must raise the muzzle; with the Z axis the raw delta pointed the other
        // way, so pitch accumulates negatively.
        // The model space is flipped 180 degrees about Z just before the model is drawn, so a
        // positive Z rotation turns the muzzle the other way in world terms -- the sign is
        // therefore positive here.
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
     * Rotates the MODEL, not the pose.
     *
     * Rotating the poseStack here means rotating about the camera origin: the gun sits a little
     * in front of the camera, so any rotation swings it around the camera and it creeps closer or
     * further away. TaCZ already rotates the model in place for weapon bob by composing into
     * BedrockPart.additionalQuaternion, so the drag goes there too -- that pivots around the
     * model's own origin and leaves the distance alone.
     *
     * In this frame the barrel runs along X, so a roll around the barrel would be rotateX; pitch
     * therefore uses Z, and yaw uses Y. Drag up must raise the muzzle, hence the yaw-then-pitch
     * order and the sign already baked into pitch.
     */
    public static void apply(com.tacz.guns.client.model.BedrockGunModel model) {
        if (yaw == 0F && pitch == 0F) {
            return;
        }
        if (model == null) {
            return;
        }
        com.tacz.guns.client.model.bedrock.BedrockPart root = model.getRootNode();
        if (root == null) {
            return;
        }
        Quaternionf rotation = new Quaternionf()
                .rotationY((float) Math.toRadians(yaw))
                .rotateZ((float) Math.toRadians(pitch));
        root.additionalQuaternion.mul(rotation);
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
