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

/**
 * 客户端：在准星旁画命中标记（单 X，按颜色区分部位与击杀）。
 *
 * <ul>
 *   <li>打到非头部 -> <b>白色</b>单 X</li>
 *   <li>打到头部   -> <b>橙色</b>单 X</li>
 *   <li>致死       -> <b>红色</b>单 X（优先级最高）</li>
 * </ul>
 *
 * <p>形状只有一个，尺寸/位置都不随命中部位变化 —— 所以连发时标记是
 * "同一个 X 换颜色"，不会跳动。（曾经爆头画两个叠加的 X，实测糊成一团，已删。）
 *
 * <p>数据来源是 {@link HitFeedbackNetwork} 那条自定义包（服务端只发给射手本人）。
 * 这一版不再显示伤害数字 —— 数字由 GD656击杀图标 模组负责，
 * 两边都画会叠在一起看不清（实测就是这样）。
 */
@OnlyIn(Dist.CLIENT)
public final class HitFeedbackClient {

    /** 一条待显示的标记 */
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

    /**
     * 当前正在显示的那一个标记。
     *
     * <p>⚠ <b>只保留一个</b>，不要改成 List。之前是"每次命中 push 一条"，
     * 结果连续命中（比如一直打甲）时，几发之内就在同一个位置叠出好几个 X，
     * 屏幕上看着是一坨菱形花纹 —— 玩家会以为画错了。
     * 其他 FPS 的命中标记都只在准星处显示一个，这里对齐同样的行为。
     *
     * <p>连续命中时的处理是"<b>复用并重置计时</b>"，不是"抑制新标记" ——
     * 抑制的话一直打甲会有一段时间完全不显示，看起来像功能坏了；
     * 重置则表现为标记一直亮着，既看得见又不会叠加。
     */
    private static Entry current = null;

    /**
     * 单 X 的横跨半径（像素）。配合 hit.scale 使用。
     *
     * <p>现在是<b>唯一</b>的标记尺寸 —— 爆头不再换尺寸（以前爆头要缩到 4，
     * 因为两个 X 会叠在一起；现在只有一个 X，没这个问题了）。
     */
    private static final int OUTER = 7;
    /**
     * X 中间留空的半径。
     *
     * <p>留空不是为了好看 —— 是为了<b>不挡住准星</b>：标记中心正压在准星上，
     * 中间挖空之后准星那根十字还能看见（实测内孔对准星刚好）。
     */
    private static final int INNER = 2;
    /** 线宽（像素），随 scale 一起放大 */
    private static final int THICKNESS = 2;

    /** 记录最近一次命中的时刻，用来做"命中抖动"（只在玩家确实开枪命中时抖） */
    private static long shakeUntilMs = 0L;

    private HitFeedbackClient() {
    }

    /** 收到服务端的命中通知 */
    public static void onHit(HitFeedbackNetwork.HitFeedbackMessage msg) {
        if (!HitFeedbackConfig.enabled()) {
            return;
        }
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (player == null) {
            return;
        }

        // 复用同一个位置：后一发覆盖前一发，并重新计时（所以标记会"一直亮"而不是叠起来）
        // 颜色在这里就定下来（致死 > 爆头 > 普通），画的时候不用再判断
        current = new Entry(msg.headshot(), HitFeedbackLogic.colorFor(msg.headshot(), msg.killed()),
                System.currentTimeMillis());

        if (HitFeedbackConfig.sound()) {
            // 原版"经验球拾取"音效（SoundEvents.f_11871_）：
            // 短促、不刺耳，适合当命中确认音；爆头时音调抬高一点。
            player.m_5496_(SoundEvents.f_11871_, 0.7f, msg.headshot() ? 1.6f : 1.2f);
        }
        if (HitFeedbackConfig.shake()) {
            shakeUntilMs = System.currentTimeMillis() + 90L;
        }
    }

    /** 画 X */
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

        // 前 25% 保持不透明，之后线性淡出 —— 标记要"闪一下"才够干脆
        float alpha = t < 0.25f ? 1f : 1f - (t - 0.25f) / 0.75f;
        if (!HitFeedbackLogic.visible(alpha)) {
            return;
        }

        float scale = HitFeedbackConfig.scale();
        GuiGraphics graphics = event.getGuiGraphics();
        // 标记的屏幕坐标 = 准星（GUI 中心）+ 配置的偏移。
        // 屏幕坐标系 y 向下为正，所以 offset_y 正数=往下。
        int centerX = mc.m_91268_().m_85445_() / 2 + HitFeedbackConfig.offsetX();
        int centerY = mc.m_91268_().m_85446_() / 2 + HitFeedbackConfig.offsetY();

        int color = alphaColor(e.color, alpha);
        // 形态只有一个单 X —— 爆头不再换尺寸、也不再画第二个 X，只靠颜色区分。
        // 这样也顺手消掉了"爆头 X 必须缩小才读得出来"那套妥协参数。
        HitFeedbackLogic.Stroke[] strokes = HitFeedbackLogic.markerStrokes(OUTER, INNER);
        for (HitFeedbackLogic.Stroke s : strokes) {
            drawStroke(graphics, centerX, centerY, scale, s, color);
        }
    }

    /**
     * 把一条斜线段画成若干条水平细线。
     *
     * <p>为什么这么画：{@code GuiGraphics} 在 1.20.1 只有正交的 {@code fill} /
     * {@code hLine} / {@code vLine} 可用（没有任意旋转的矩形填充），
     * 而 X 是 45 度的。逐行铺 1 像素高的横线正好能得到干净的斜线，
     * 而且不引入任何额外的渲染状态。
     *
     * <p>{@code lineWidth} 是横线长度，作用在 45 度方向上等于把线加粗。
     */
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

    /**
     * 命中瞬间给镜头一点极小的抖动（90ms）。
     * 只影响相机角度、不改任何游戏状态，纯观感。
     */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!HitFeedbackConfig.enabled() || !HitFeedbackConfig.shake()) {
            return;
        }
        if (System.currentTimeMillis() >= shakeUntilMs) {
            return;
        }
        double time = event.getPartialTick() * 40.0;
        // 幅度很小：命中感有了，但不会影响瞄准
        event.setYaw(event.getYaw() + (float) Math.sin(time) * 0.22f);
        event.setPitch(event.getPitch() + (float) Math.cos(time * 1.3) * 0.16f);
    }

    private static int alphaColor(int rgb, float alpha) {
        int a = (int) (Math.max(0f, Math.min(1f, alpha)) * 255f);
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
