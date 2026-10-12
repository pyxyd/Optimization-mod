package com.taczopt.crosshair;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.AimInaccuracyModifier;
import com.tacz.guns.resource.modifier.custom.InaccuracyModifier;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.HitboxHelper;
import com.taczopt.TaczOptimizeMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;

/**
 * 客户端：把准星换成《三角洲行动》那样的空心十字腰射准星。
 *
 * <h2>三方来源，一个都不能漏</h2>
 * <ol>
 *   <li><b>原版准星</b>：{@link RenderGuiOverlayEvent.Pre} 里 {@code setCanceled(true)} 即可。</li>
 *   <li><b>TaCZ 准星</b>：它挂在同一个原版 overlay 上、而且<b>不检查事件是否被取消</b>，
 *       所以只能由 {@code CrosshairEventMixin} 把它的处理器整个取消掉
 *       （准入条件与本类的 {@link #shouldTakeOver()} 完全一致）。</li>
 *   <li><b>本模组自己画</b>：{@link #onPreOverlay}。</li>
 * </ol>
 *
 * <h2>扩散怎么来</h2>
 * TaCZ 的枪械数据里有 {@code inaccuracy}（站/走/蹲/趴/开镜各一档），
 * 但客户端不直接暴露"当前值"。所以按<b>实际水平移动速度</b>在"站定"和"移动"
 * 两档之间插值 —— 玩家能感知到的就是"走起来准星张开"。
 * 附件对扩散的加成从 {@link AttachmentCacheProperty} 取（TaCZ 自己算好的）。
 */
@OnlyIn(Dist.CLIENT)
public final class CrosshairStyleClient {

    private CrosshairStyleClient() {
    }

    /**
     * 是否由本模组接管准星。
     *
     * <p>{@code CrosshairEventMixin} 和本类的事件处理器都调它，
     * 保证"TaCZ 不画"和"我们画"永远同步 —— 两边条件写岔了就会出现
     * 要么两个准星叠着、要么一个都没有。
     */
    public static boolean shouldTakeOver() {
        if (!CrosshairConfig.enabled()) {
            return false;
        }
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (player == null) {
            return false;
        }
        // 正在用盾/药水/吃东西时原版自己会隐藏准星，交还原版逻辑
        if (player.m_6117_()) {
            return false;
        }
        // 开镜时隐藏（三角洲就是这样：腰射才显示准星）。
        // 另外「蹲下瞄准」也当成开镜处理 —— 那种状态扩散为 0（见 CrouchAimSpreadMixin），
        // 准星应该消失，而不是画一个间距 0 的十字糊在屏幕中间。
        if (CrosshairConfig.hideOnAim() && (isAiming(player) || CrouchAim.active(player))) {
            return true;
        }
        // 只接管持枪场合；挖方块/空手时原版准星更好用
        boolean holdingGun;
        try {
            holdingGun = IGun.mainHandHoldGun(player);
        } catch (Throwable t) {
            holdingGun = false;
        }
        if (CrosshairConfig.onlyGuns() && !holdingGun) {
            return false;
        }
        return true;
    }

    private static boolean isAiming(LocalPlayer player) {
        try {
            return IClientPlayerGunOperator.fromLocalPlayer(player).isAim();
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 拦原版准星 + 画自己的准星。
     *
     * <p>优先级 HIGHEST：要赶在原版真正绘制之前取消掉。
     * TaCZ 的处理器优先级未知，但它已经被 mixin 整个取消了，不参与竞争。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPreOverlay(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) {
            return;
        }
        if (!shouldTakeOver()) {
            return;
        }
        // 无论开不开镜都要拦掉原版准星：
        // 开镜时是"隐藏准星"，腰射时是"换成自己的"
        event.setCanceled(true);

        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (player == null) {
            return;
        }
        if (CrosshairConfig.hideOnAim() && (isAiming(player) || CrouchAim.active(player))) {
            return;
        }
        drawCrosshair(event.getGuiGraphics(), player);
    }

    private static void drawCrosshair(GuiGraphics graphics, LocalPlayer player) {
        try {
            int centerX = Minecraft.m_91087_().m_91268_().m_85445_() / 2;
            int centerY = Minecraft.m_91087_().m_91268_().m_85446_() / 2;

            ItemStack held = player.m_21205_();
            IGun iGun = IGun.getIGunOrNull(held);

            // ── 间距 = 该枪的腰射扩散（= 10 - 腰射精度） × spread_scale ──
            // **只缩放"扩散"这一个量**，不是缩放整个准星图形。
            // 线长 / 线宽 / 中心点是准星自己的形状参数，不跟着动 ——
            // 它们各有各的配置键，混在一起乘就分不清哪个在起作用了
            // （上一版就是那么写的，结果调 size_scale 会同时改粗细，没法单独调）。
            int gap;
            if (CrosshairConfig.dynamic() && iGun != null) {
                float ina = inaccuracy(player, iGun, held);
                gap = CrosshairLogic.gapFor(ina, CrosshairConfig.spreadScale(),
                        1, CrosshairConfig.gapMax());
            } else {
                gap = CrosshairConfig.gap();
            }

            int color = 0xFF000000 | CrosshairConfig.color();
            int thickness = CrosshairConfig.thickness();
            int arm = CrosshairConfig.arm();
            int dot = CrosshairConfig.dot();

            if (CrosshairConfig.debugLog()) {
                logOnce(gap, iGun, player, held);
            }

            for (CrosshairLogic.Bar b : CrosshairLogic.cross(gap, arm, thickness)) {
                fill(graphics, centerX, centerY, b, color);
            }
            if (dot > 0) {
                fill(graphics, centerX, centerY, CrosshairLogic.dot(dot), color);
            }
        } catch (Throwable t) {
            TaczOptimizeMod.logger().warn("[taczopt] 画准星失败", t);
        }
    }

    /**
     * 当前腰射扩散 —— <b>照搬 TaCZ 原版算法</b>（见 {@link HipFireSpread} 的说明）。
     *
     * <p>流程和 {@code ModernKineticGunScriptAPI} 开火时那段一一对应：
     * <ol>
     *   <li>先按瞄准进度 / 游泳 / 姿态 / 移动判定<b>档位</b>（离散，不插值）；</li>
     *   <li>从附件缓存 {@code GunProperties.INACCURACY} 那张表按档位取值；</li>
     *   <li>乘上过热系数（没过热数据就是 1.0）；</li>
     *   <li>夹到不小于 0。</li>
     * </ol>
     *
     * <p>⚠ 下面用到了 TaCZ 标为 {@code @Deprecated} 的
     * {@code AimInaccuracyModifier.ID} —— 那是<b>别人的</b>过时标记，不是我们的。
     * 它是开镜档 {@code GunProperties.AIM_INACCURACY} 那张表的缓存键，TaCZ 目前
     * 仍在正常使用；换掉它等于赌"新键和旧键是同一个字符串"，赌错了开镜档的扩散
     * 会静默变成兜底默认值（准星看着就是"开镜档不准"）。
     * 所以这里保留调用 + 抑制警告：既不改行为，也不让 javac 往 stderr 写警告
     * （build.ps1 把"javac 的 stderr 非空"当成构建失败信号）。
     */
    @SuppressWarnings("deprecation")
    private static float inaccuracy(LocalPlayer player, IGun iGun, ItemStack held) {
        IGunOperator op = null;
        try {
            op = IGunOperator.fromLivingEntity(player);
        } catch (Throwable ignored) {
            // 下面都用兜底值
        }

        // --- 1) 档位判定（顺序与原版一致：AIM > LIE > SNEAK > MOVE > STAND）---
        float aimingProgress = 0f;
        try {
            if (op != null) {
                aimingProgress = op.getSynAimingProgress();
            }
        } catch (Throwable ignored) {
            // 拿不到就当没开镜
        }
        boolean swimming = false;
        boolean crouching = false;
        try {
            swimming = player.m_6069_() || player.m_20089_() == Pose.SWIMMING;
            crouching = player.m_20089_() == Pose.CROUCHING;
        } catch (Throwable ignored) {
            // 拿不到姿态就按站定算
        }
        boolean moving = false;
        try {
            // 原版：玩家改用 HitboxHelper.getPlayerVelocity(p).length()，阈值 0.05
            moving = HipFireSpread.isMove(
                    Math.abs(player.f_19787_ - player.f_19867_),
                    HitboxHelper.getPlayerVelocity(player).m_82553_());
        } catch (Throwable ignored) {
            // 速度取不到就退回腿部摆动
            try {
                moving = HipFireSpread.isMove(
                        Math.abs(player.f_19787_ - player.f_19867_), Double.NaN);
            } catch (Throwable ignored2) {
                // 保持 false
            }
        }
        HipFireSpread.Stance stance =
                HipFireSpread.stanceFor(aimingProgress, swimming, crouching, moving);

        // --- 2) 档位基础值：优先附件缓存（算上附件后的最终值），退回枪械数据 ---
        Float base = null;
        try {
            AttachmentCacheProperty cache = op == null ? null : op.getCacheProperty();
            if (cache != null) {
                base = lookup(cache.getCache(InaccuracyModifier.ID), stance);
                if (base == null) {
                    // 开镜档在 TaCZ 里是另一张表（GunProperties.AIM_INACCURACY）
                    base = lookup(cache.getCache(AimInaccuracyModifier.ID), stance);
                }
            }
        } catch (Throwable ignored) {
            // 退回枪械数据
        }
        if (base == null) {
            try {
                GunData data = TimelessAPI.getClientGunIndex(iGun.getGunId(held))
                        .map(ClientGunIndex::getGunData).orElse(null);
                if (data != null) {
                    base = lookup(data.getInaccuracy(), stance);
                }
            } catch (Throwable ignored) {
                // 用兜底常量
            }
        }
        if (base == null) {
            base = HipFireSpread.defaultFor(stance);
        }

        // --- 3) 过热系数 ---
        float heatMul = 1.0f;
        try {
            if (iGun.hasHeatData(held)) {
                GunData data = TimelessAPI.getClientGunIndex(iGun.getGunId(held))
                        .map(ClientGunIndex::getGunData).orElse(null);
                var heat = data == null ? null : data.getHeatData();
                if (heat != null) {
                    heatMul = HipFireSpread.heatMultiplier(true,
                            iGun.getHeatAmount(held), heat.getHeatMax(),
                            heat.getMinInaccuracy(), heat.getMaxInaccuracy());
                }
            }
        } catch (Throwable ignored) {
            // 过热算不了就当 1.0，不影响普通枪
        }

        // --- 4) 最终扩散 ---
        return HipFireSpread.spread(base, heatMul);
    }

    /** 从 {@code Map<InaccuracyType, Float>} 里取某个档位（键按名字匹配，避免依赖枚举类型） */
    private static Float lookup(Map<?, Float> table, HipFireSpread.Stance stance) {
        if (table == null) {
            return null;
        }
        String want = stance.name();
        for (Map.Entry<?, Float> e : table.entrySet()) {
            if (e.getValue() != null && want.equalsIgnoreCase(String.valueOf(e.getKey()))) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * 调试用：档位或间距变了才打一行日志（每帧打会把日志刷爆）。
     *
     * <p>排查"准星大小不对"时把它打开（{@code crosshair.debug_log=true}），
     * 日志里就能看到当前档位、扩散值、最终间距 —— 和枪械数据一对就知道哪一步错了。
     */
    private static String lastLogged = null;

    private static void logOnce(int gap, IGun iGun, LocalPlayer player, ItemStack held) {
        try {
            String gunId = iGun == null ? "(空手)" : String.valueOf(iGun.getGunId(held));
            boolean sneaking = player.m_20089_() == Pose.CROUCHING;
            boolean swimming = player.m_6069_() || player.m_20089_() == Pose.SWIMMING;
            boolean moving = HipFireSpread.isMove(
                    Math.abs(player.f_19787_ - player.f_19867_),
                    HitboxHelper.getPlayerVelocity(player).m_82553_());
            String key = gunId + "|" + sneaking + "|" + swimming + "|" + moving + "|" + gap;
            if (key.equals(lastLogged)) {
                return;
            }
            lastLogged = key;
            TaczOptimizeMod.logger().info(
                    "[taczopt] 准星：枪={} 蹲={} 游={} 移动={} -> 间距={}px（基础: 线长{} 线宽{} 中心点{}，扩散倍率{}x）",
                    gunId, sneaking, swimming, moving, gap,
                    CrosshairConfig.arm(), CrosshairConfig.thickness(), CrosshairConfig.dot(),
                    CrosshairConfig.spreadScale());
        } catch (Throwable ignored) {
            // 调试日志失败无所谓
        }
    }

    private static void fill(GuiGraphics graphics, int centerX, int centerY,
                             CrosshairLogic.Bar b, int color) {
        graphics.m_280509_(centerX + b.x0(), centerY + b.y0(),
                centerX + b.x1() + 1, centerY + b.y1() + 1, color);
    }
}
