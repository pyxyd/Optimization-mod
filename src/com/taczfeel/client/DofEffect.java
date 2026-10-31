package com.taczfeel.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.taczfeel.FeelConfig;
import com.taczfeel.FeelLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 开镜景深。
 * <p>
 * 实现方式是 MC 自带的后期处理链（PostChain）——Embeddium 不接管这条链路
 * （它的 shader 类只服务于区块/云层，没有后期处理 API），而它换掉的
 * LevelRenderer 又完全不影响 GameRenderer 的 framebuffer 后期处理，
 * 所以这是与 Embeddium 共存最稳的做法。
 * <p>
 * 处理时机是 RenderLevelStageEvent 的 AFTER_LEVEL，也就是"世界画完、第一人称
 * 枪模还没画"的那一刻：所以糊的是世界，枪模和 HUD 永远是锐的，不需要读深度图。
 * <p>
 * 安全策略：链构造失败/运行期抛异常都只关掉这个模块并打一条日志，
 * 绝不会每帧刷屏或者把画面搞成黑的。
 */
public final class DofEffect {

    private static final ResourceLocation CHAIN_ID =
            new ResourceLocation("taczfeel", "shaders/post/ads_dof.json");

    private static PostChain chain;
    private static Uniform focusUniform;
    private static Uniform strengthUniform;
    private static Uniform coreUniform;

    private static boolean failed = false;
    private static boolean reflectionReady = false;

    private static Field passesField;
    private static Method getEffectMethod;
    private static Method getUniformMethod;
    private static Method setFloatMethod;

    private DofEffect() {
    }

    /** @param focus 0~1，通常就是开镜进度 */
    public static void render(float partialTick, float focus) {
        if (failed || !FeelConfig.dofEnabled) {
            return;
        }
        PostChain postChain = chain();
        if (postChain == null) {
            return;
        }
        try {
            setUniform(focusUniform, focus);
            setUniform(strengthUniform, FeelConfig.dofStrength);
            setUniform(coreUniform, FeelConfig.dofCore);
            postChain.m_110023_(partialTick);
        } catch (Throwable t) {
            failed = true;
            FeelLog.warn("dof disabled after runtime error: " + t);
            invalidate();
        } finally {
            restore();
        }
    }

    /** F3+T / 换资源包后 shader program 会被重建，链必须扔掉重建 */
    public static void invalidate() {
        PostChain old = chain;
        chain = null;
        focusUniform = null;
        strengthUniform = null;
        coreUniform = null;
        if (old != null) {
            try {
                old.close();
            } catch (Throwable ignored) {
                // 关不掉就算了，反正已经不再使用
            }
        }
    }

    private static PostChain chain() {
        if (chain != null) {
            return chain;
        }
        if (failed) {
            return null;
        }
        try {
            Minecraft mc = Minecraft.m_91087_();
            if (mc == null) {
                return null;
            }
            RenderTarget main = mc.m_91385_();
            if (main == null) {
                return null;
            }
            chain = new PostChain(mc.m_91097_(), mc.m_91098_(), main, CHAIN_ID);
            resolveUniforms(chain);
            FeelLog.info("dof chain ready, uniforms: focus=" + (focusUniform != null)
                    + " strength=" + (strengthUniform != null)
                    + " core=" + (coreUniform != null));
            return chain;
        } catch (Throwable t) {
            failed = true;
            chain = null;
            FeelLog.warn("dof unavailable (post chain build failed): " + t);
            return null;
        }
    }

    private static void resolveUniforms(PostChain postChain) throws Exception {
        if (!reflectionReady) {
            reflectionReady = true;
            try {
                passesField = PostChain.class.getDeclaredField("f_110009_");
                passesField.setAccessible(true);
                getEffectMethod = PostPass.class.getMethod("m_110074_");
                getUniformMethod = EffectInstance.class.getMethod("m_108952_", String.class);
                setFloatMethod = Uniform.class.getMethod("m_5985_", float.class);
            } catch (Throwable t) {
                FeelLog.warn("dof uniform access unavailable: " + t);
                return;
            }
        }
        if (passesField == null) {
            return;
        }
        Object value = passesField.get(postChain);
        if (!(value instanceof List)) {
            return;
        }
        List<?> passes = (List<?>) value;
        if (passes.isEmpty()) {
            return;
        }
        Object effect = getEffectMethod.invoke(passes.get(0));
        if (effect == null) {
            return;
        }
        // getUniform 找不到时返回 null（safeGetUniform 会返回一个空实现，反而看不出问题）
        focusUniform = (Uniform) getUniformMethod.invoke(effect, "Focus");
        strengthUniform = (Uniform) getUniformMethod.invoke(effect, "Strength");
        coreUniform = (Uniform) getUniformMethod.invoke(effect, "Core");
        if (focusUniform == null || strengthUniform == null || coreUniform == null) {
            FeelLog.warn("dof uniform missing (focus=" + (focusUniform != null)
                    + " strength=" + (strengthUniform != null)
                    + " core=" + (coreUniform != null)
                    + "), 模糊强度会停在 shader 默认值");
        }
    }

    private static void setUniform(Uniform uniform, float value) throws Exception {
        if (uniform == null) {
            return;
        }
        setFloatMethod.invoke(uniform, value);
    }

    /** 后期处理会改动 GL 状态与绑定的 framebuffer，这里恢复成"世界的正常状态" */
    private static void restore() {
        try {
            Minecraft mc = Minecraft.m_91087_();
            if (mc != null) {
                RenderTarget main = mc.m_91385_();
                if (main != null) {
                    main.m_83947_(true);
                }
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
        } catch (Throwable ignored) {
            // 恢复失败也不该影响后面的渲染
        }
    }

    public static boolean isFailed() {
        return failed;
    }
}
