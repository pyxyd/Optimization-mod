package com.taczopt.hybridaim;

/** 按键状态机 + 拦截是否生效的标记 */
public final class HybridAimState {

    private static boolean pressed = false;
    private static long pressStartMs = 0L;
    private static boolean openedByPress = false;
    private static boolean suppressionSeen = false;

    private HybridAimState() {
    }

    public static boolean isPressed() {
        return pressed;
    }

    public static void setPressed(boolean value) {
        pressed = value;
    }

    public static long pressStartMs() {
        return pressStartMs;
    }

    public static void setPressStartMs(long value) {
        pressStartMs = value;
    }

    public static boolean isOpenedByPress() {
        return openedByPress;
    }

    public static void setOpenedByPress(boolean value) {
        openedByPress = value;
    }

    /** TaCZ 自己的开镜键处理被成功拦下（= 混合逻辑独占输入） */
    public static void markSuppressionSeen() {
        suppressionSeen = true;
    }

    public static boolean suppressionSeen() {
        return suppressionSeen;
    }

    public static void reset() {
        pressed = false;
        openedByPress = false;
    }
}