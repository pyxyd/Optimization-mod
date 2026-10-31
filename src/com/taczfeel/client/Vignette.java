package com.taczfeel.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.taczfeel.FeelConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * 开镜暗角：一张 256x256 的径向渐变贴图，按屏幕尺寸拉伸铺满，透明度跟着开镜进度走。
 * 不碰任何渲染管线设置，只在 GUI 层叠一层带混合的贴图，兼容 Embeddium / 光影包。
 */
public final class Vignette {

    private static final ResourceLocation TEXTURE = new ResourceLocation("taczfeel", "textures/gui/vignette.png");
    private static final float TEX_SIZE = 256.0F;

    private Vignette() {
    }

    public static void render(GuiGraphics graphics, float partialTick) {
        if (!FeelConfig.vignetteEnabled || FeelConfig.vignetteAlpha <= 0.001F) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        if (mc == null) {
            return;
        }
        LocalPlayer player = mc.f_91074_;
        // 有界面（背包/ESC/聊天输入）就不画，避免盖住 UI
        if (player == null || mc.f_91080_ != null) {
            return;
        }
        if (FeelConfig.vignetteOnlyGun && !IGun.mainhandHoldGun(player)) {
            return;
        }
        float aim;
        try {
            IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(player);
            aim = operator == null ? 0F : operator.getClientAimingProgress(partialTick);
        } catch (Throwable t) {
            return;
        }
        if (aim <= 0.02F) {
            return;
        }
        float alpha = aim * FeelConfig.vignetteAlpha;
        if (alpha <= 0.005F) {
            return;
        }

        int width = graphics.m_280182_();
        int height = graphics.m_280206_();
        if (width <= 0 || height <= 0) {
            return;
        }

        int rgb = parseColor(FeelConfig.vignetteColor);
        float r = ((rgb >> 16) & 0xFF) / 255.0F;
        float g = ((rgb >> 8) & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;

        PoseStack pose = graphics.m_280168_();
        pose.m_85836_();
        pose.m_85841_(width / TEX_SIZE, height / TEX_SIZE, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(r, g, b, alpha);
        graphics.m_280218_(TEXTURE, 0, 0, 0, 0, 256, 256);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        // 混合状态不回滚：GUI 这一层本来就是开着混合画的，回滚反而会把
        // 之后别的 mod 在 Post 阶段画的东西变成不透明。
        pose.m_85849_();
    }

    /** "000000" / "FF8800" / 带 # 的写法都吃 */
    private static int parseColor(String value) {
        if (value == null) {
            return 0;
        }
        String s = value.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.length() != 6) {
            return 0;
        }
        try {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
