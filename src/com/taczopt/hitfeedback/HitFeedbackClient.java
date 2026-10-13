package com.taczopt.hitfeedback;

import com.taczopt.TaczOptimizeMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public final class HitFeedbackClient {

    private static final class Entry {
        final boolean headshot;
        final int color;
        final long bornMs;

        Entry(boolean headshot, int color, long bornMs) {
            this.headshot = headshot;
            this.color = color;
            this.bornMs = bornMs;
        }
    }

    private static Entry current = null;

    private static final int OUTER = 7;

    private static final int INNER = 2;

    private static final int THICKNESS = 2;

    private static long shakeUntilMs = 0L;

    private HitFeedbackClient() {
    }

    public static void onHit(HitFeedbackNetwork.HitFeedbackMessage msg) {
        if (!HitFeedbackConfig.enabled()) {
            return;
        }
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (player == null) {
            return;
        }

        current = new Entry(msg.headshot(), HitFeedbackLogic.colorFor(msg.headshot(), msg.killed()),
                System.currentTimeMillis());

        if (HitFeedbackConfig.sound()) {

            player.m_5496_(SoundEvents.f_11871_, 0.7f, msg.headshot() ? 1.6f : 1.2f);
        }
        if (HitFeedbackConfig.shake()) {
            shakeUntilMs = System.currentTimeMillis() + 90L;
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!HitFeedbackConfig.enabled()) {
            return;
        }
        Entry e = current;
        if (e == null) {
            return;
        }

        Minecraft mc = Minecraft.m_91087_();
        if (mc.m_91268_() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        int duration = HitFeedbackConfig.durationMs();
        long age = now - e.bornMs;
        float t = (float) age / (float) duration;
        if (t >= 1f) {
            current = null;
            return;
        }

        float alpha = t < 0.25f ? 1f : 1f - (t - 0.25f) / 0.75f;
        if (!HitFeedbackLogic.visible(alpha)) {
            return;
        }

        float scale = HitFeedbackConfig.scale();
        GuiGraphics graphics = event.getGuiGraphics();

        int centerX = mc.m_91268_().m_85445_() / 2 + HitFeedbackConfig.offsetX();
        int centerY = mc.m_91268_().m_85446_() / 2 + HitFeedbackConfig.offsetY();

        int color = alphaColor(e.color, alpha);

        HitFeedbackLogic.Stroke[] strokes = HitFeedbackLogic.markerStrokes(OUTER, INNER);
        for (HitFeedbackLogic.Stroke s : strokes) {
            drawStroke(graphics, centerX, centerY, scale, s, color);
        }
    }

    private static void drawStroke(GuiGraphics graphics, int centerX, int centerY,
                                   float scale, HitFeedbackLogic.Stroke s, int color) {
        int dx = s.x1() - s.x0();
        int dy = s.y1() - s.y0();
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        int lineWidth = Math.max(1, Math.round(THICKNESS * scale));
        for (int i = 0; i <= steps; i++) {
            float f = steps == 0 ? 0f : (float) i / (float) steps;
            int px = Math.round((s.x0() + dx * f) * scale);
            int py = Math.round((s.y0() + dy * f) * scale);
            int x = centerX + px;
            int y = centerY + py;
            graphics.m_280509_(x, y, x + lineWidth, y + 1, color);
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!HitFeedbackConfig.enabled() || !HitFeedbackConfig.shake()) {
            return;
        }
        if (System.currentTimeMillis() >= shakeUntilMs) {
            return;
        }
        double time = event.getPartialTick() * 40.0;

        event.setYaw(event.getYaw() + (float) Math.sin(time) * 0.22f);
        event.setPitch(event.getPitch() + (float) Math.cos(time * 1.3) * 0.16f);
    }

    private static int alphaColor(int rgb, float alpha) {
        int a = (int) (Math.max(0f, Math.min(1f, alpha)) * 255f);
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
