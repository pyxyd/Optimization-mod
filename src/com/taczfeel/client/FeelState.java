package com.taczfeel.client;

import com.taczfeel.FeelConfig;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 所有"冲击量"的状态机：开火时加冲量，每帧按指数衰减回正。
 * 纯客户端主线程使用，不需要同步。
 */
public final class FeelState {

    private static final ThreadLocalRandom RND = ThreadLocalRandom.current();

    private static long lastShotNanos = Long.MIN_VALUE;
    private static long lastDecayNanos = System.nanoTime();

    // 枪身后坐（格 / 度）
    private static float kBack;
    private static float kUp;
    private static float kSide;
    private static float kPitch;
    private static float kRoll;

    // 镜头冲击（度）
    private static float cYaw;
    private static float cPitch;
    private static float cRoll;

    private FeelState() {
    }

    /** 刚开过火（给音效混音判断"这一段是不是枪声"用） */
    public static boolean isShotWindow() {
        return System.nanoTime() - lastShotNanos < 250_000_000L;
    }

    public static void markShot() {
        lastShotNanos = System.nanoTime();
    }

    /**
     * 开火：加冲量。
     *
     * @param recoilScale 这把枪的后坐力倍数
     * @param aiming      是否处于开镜状态
     */
    public static void onShot(float recoilScale, boolean aiming) {
        markShot();
        float s = Math.max(0.05F, recoilScale);
        if (FeelConfig.kickEnabled) {
            float stance = aiming ? FeelConfig.kickAdsScale : FeelConfig.kickHipfireScale;
            float k = s * stance;
            if (k > 0F) {
                float max = FeelConfig.kickMax;
                kBack = clamp(kBack + FeelConfig.kickBack * k, -max, max);
                kUp = clamp(kUp + FeelConfig.kickUp * k, -max, max);
                kSide = clamp(kSide + (RND.nextFloat() * 2F - 1F) * FeelConfig.kickSide * k, -max, max);
                kPitch = clamp(kPitch + FeelConfig.kickPitch * k, 0F, 20F);
                kRoll = clamp(kRoll + (RND.nextFloat() * 2F - 1F) * FeelConfig.kickRoll * k, -20F, 20F);
            }
        }
        if (FeelConfig.cameraEnabled) {
            float stance = aiming ? FeelConfig.cameraAdsScale : FeelConfig.cameraHipfireScale;
            float k = s * stance;
            if (k > 0F) {
                // MC 的 pitch 正数是往下看，所以往"上跳"是减
                cPitch = clamp(cPitch - FeelConfig.cameraPitch * k, -12F, 0F);
                cYaw = clamp(cYaw + (RND.nextFloat() * 2F - 1F) * FeelConfig.cameraYaw * k, -8F, 8F);
                cRoll = clamp(cRoll + (RND.nextFloat() * 2F - 1F) * FeelConfig.cameraRoll * k, -8F, 8F);
            }
        }
    }

    /** 坐标 + 角度：[side, up, back, pitchDeg, rollDeg]，全为 0 时返回 null 省掉渲染开销 */
    public static float[] kickVector() {
        decayNow();
        if (Math.abs(kBack) < 1.0E-4F && Math.abs(kUp) < 1.0E-4F && Math.abs(kSide) < 1.0E-4F
                && Math.abs(kPitch) < 1.0E-3F && Math.abs(kRoll) < 1.0E-3F) {
            return null;
        }
        return new float[]{kSide, kUp, kBack, kPitch, kRoll};
    }

    /** 镜头：[yaw, pitch, roll]，全为 0 时返回 null */
    public static float[] cameraVector() {
        decayNow();
        if (Math.abs(cYaw) < 1.0E-3F && Math.abs(cPitch) < 1.0E-3F && Math.abs(cRoll) < 1.0E-3F) {
            return null;
        }
        return new float[]{cYaw, cPitch, cRoll};
    }

    /** 每 tick 也推一把，保证不渲染时状态也会回正 */
    public static void tick() {
        decayNow();
    }

    public static void reset() {
        kBack = kUp = kSide = kPitch = kRoll = 0F;
        cYaw = cPitch = cRoll = 0F;
        lastDecayNanos = System.nanoTime();
    }

    private static void decayNow() {
        long now = System.nanoTime();
        float dt = (now - lastDecayNanos) / 1.0E9F;
        if (dt <= 0F) {
            return;
        }
        lastDecayNanos = now;
        if (dt > 0.2F) {
            // 掉帧 / 切换世界：直接归零，别让枪卡在半空
            reset();
            return;
        }
        float kk = (float) Math.exp(-FeelConfig.kickRecover * dt);
        kBack *= kk;
        kUp *= kk;
        kSide *= kk;
        kPitch *= kk;
        kRoll *= kk;

        float ck = (float) Math.exp(-FeelConfig.cameraRecover * dt);
        cYaw *= ck;
        cPitch *= ck;
        cRoll *= ck;

        if (Math.abs(kBack) < 1.0E-5F) {
            kBack = 0F;
        }
        if (Math.abs(kUp) < 1.0E-5F) {
            kUp = 0F;
        }
        if (Math.abs(kSide) < 1.0E-5F) {
            kSide = 0F;
        }
        if (Math.abs(kPitch) < 1.0E-4F) {
            kPitch = 0F;
        }
        if (Math.abs(kRoll) < 1.0E-4F) {
            kRoll = 0F;
        }
        if (Math.abs(cYaw) < 1.0E-4F) {
            cYaw = 0F;
        }
        if (Math.abs(cPitch) < 1.0E-4F) {
            cPitch = 0F;
        }
        if (Math.abs(cRoll) < 1.0E-4F) {
            cRoll = 0F;
        }
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }
}
