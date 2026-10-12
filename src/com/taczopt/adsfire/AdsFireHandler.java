package com.taczopt.adsfire;

import com.taczopt.TaczOptimizeMod;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.input.AimKey;
import com.tacz.guns.client.input.ShootKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端 tick 处理器：按住左键 -> 先开镜 -> 镜走完后再开火 -> 松开左键收镜。
 *
 * <p>分工：
 * <ul>
 *   <li>本类：管理会话状态、请求开镜、在开镜到位时补上第一发</li>
 *   <li>{@link com.taczopt.adsfire.mixin.LocalPlayerShootMixin}：会话未开火前，
 *       把所有 shoot() 调用挡掉（拦在唯一的必经之路上，与 tick 顺序无关）</li>
 *   <li>{@link com.taczopt.adsfire.mixin.LocalPlayerAimMixin}：会话期间不许别人收镜</li>
 * </ul>
 *
 * <p>用 HIGHEST 优先级是为了确保"会话"在 TaCZ 的 ShootKey.autoShoot 之前就建好，
 * 这样即使 autoShoot 本 tick 就想开火，也会被 shoot() 上的拦截挡住。
 *
 * <p>开火时机：开镜进度必须达到 {@link #AIM_READY_PROGRESS}（也就是枪真的抬到位了），
 * 否则就一直等，最多等到 {@link #HARD_TIMEOUT_TICKS} 兜底。
 * {@code aim_wait_ticks} 表示"至少等这么多 tick"，用来保证慢枪的开镜动画来得及走完。
 */
@Mod.EventBusSubscriber(modid = TaczOptimizeMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AdsFireHandler {

    /** 开镜进度到这个值才算"镜到位"（LocalPlayerAim 里 progress 是 0~1） */
    private static final float AIM_READY_PROGRESS = 0.99f;
    /** 玩家自己按着开镜键时，至少等这么久，免得抢在玩家前面把镜撤了 */
    private static final int MANUAL_AIM_EXTRA_TICKS = 8;
    /** 保底超时：进度一直不达标也最多等这么多 tick，防止卡住不放 */
    private static final int HARD_TIMEOUT_TICKS = 40;

    private AdsFireHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        AdsFireConfig.reloadIfChanged();
        if (!AdsFireConfig.enabled()) {
            AdsFireState.reset();
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        LocalPlayer player = mc == null ? null : mc.f_91074_;
        if (player == null) {
            AdsFireState.reset();
            return;
        }
        IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(player);
        if (operator == null) {
            return;
        }
        int tick = player.f_19797_;
        boolean shootDown = ShootKey.SHOOT_KEY != null && ShootKey.SHOOT_KEY.m_90857_();
        if (shootDown) {
            AdsFireState.markShootDown(tick);
        }

        ItemStack main = player.m_21205_();
        IGun gun = IGun.getIGunOrNull(main);
        String gunId = null;
        boolean enabledForThisGun = false;
        if (gun != null) {
            ResourceLocation id = gun.getGunId(main);
            if (id != null) {
                gunId = id.toString();
                enabledForThisGun = AdsFireConfig.isOn(gunId) && hasDisplay(main);
            }
        }

        // 手里不是启用了这个功能的枪 -> 收摊
        if (!enabledForThisGun) {
            if (AdsFireState.isActive()) {
                releaseAim(operator);
            }
            return;
        }

        // 已经开镜 -> 不干预开火（全自动连发就走这里）
        if (operator.isAim()) {
            if (AdsFireState.isActive() && !AdsFireState.hasFired()) {
                // 玩家自己抢先开了镜，把欠的那一发补上
                fireNow(operator, tick);
            }
            if (AdsFireState.isActive() && AdsFireState.hasFired() && !shootDown
                    && AdsFireConfig.releaseWhenStop()
                    && tick - AdsFireState.lastShootDownTick() > AdsFireConfig.holdTicks()) {
                releaseAim(operator);
            }
            return;
        }

        // ---- 以下都是"没开镜"的情况 ----

        if (!AdsFireState.isActive()) {
            if (!shootDown) {
                return; // 没按左键、也没开镜：什么都不用做
            }
            // 按下左键的第一刻：建会话，本 tick 的开火会被 LocalPlayerShootMixin 挡掉
            AdsFireState.begin(gunId, tick);
        }

        // 换枪了 -> 作废
        String sessionGun = AdsFireState.sessionGunId();
        if (sessionGun != null && !sessionGun.equals(gunId)) {
            releaseAim(operator);
            return;
        }

        if (AdsFireState.hasFired()) {
            // 已经打出第一发、玩家也松手了 -> 保持一会儿再收镜
            if (AdsFireConfig.releaseWhenStop() && !shootDown
                    && tick - AdsFireState.lastShootDownTick() > AdsFireConfig.holdTicks()) {
                releaseAim(operator);
            }
            return;
        }

        if (!shootDown) {
            // 还没开火就松手了（点了一下）-> 取消，不补枪
            releaseAim(operator);
            return;
        }

        if (!AdsFireState.isAimRequested()) {
            // 先记标记再发请求：万一 aim() 内部重入，也不会重复请求
            AdsFireState.markAimRequested();
            operator.aim(true);
            return;
        }

        int waited = tick - AdsFireState.startTick();
        float progress = aimProgress(operator, mc);

        // 1) 开镜走完 + 至少等了 aim_wait_ticks -> 开火
        if (progress >= AIM_READY_PROGRESS && waited >= AdsFireConfig.aimWaitTicks()) {
            if (AdsFireConfig.debugLog()) {
                TaczOptimizeMod.logger().info("[taczopt] 开镜完成，开火。progress={} 等了{}tick",
                        progress, waited);
            }
            fireNow(operator, tick);
            return;
        }

        // 2) 玩家自己也按着开镜键 -> 多给一点时间，但别无限等
        boolean manualAim = AimKey.AIM_KEY != null && AimKey.AIM_KEY.m_90857_();
        int limit = AdsFireConfig.aimWaitTicks() + (manualAim ? MANUAL_AIM_EXTRA_TICKS : 0);

        // 3) 兜底：进度一直上不去（比如这把枪没有开镜动画）也不能卡着不开火
        if (waited >= Math.max(limit, HARD_TIMEOUT_TICKS)) {
            if (AdsFireConfig.debugLog()) {
                TaczOptimizeMod.logger().info("[taczopt] 开镜超时兜底开火。progress={} 等了{}tick",
                        progress, waited);
            }
            fireNow(operator, tick);
        }
    }

    /** 当前开镜进度，0~1 */
    private static float aimProgress(IClientPlayerGunOperator operator, Minecraft mc) {
        float partial = mc == null ? 1.0f : mc.m_91297_();
        try {
            return operator.getClientAimingProgress(partial);
        } catch (RuntimeException e) {
            return 1.0f;
        }
    }

    /** 请求开镜后立即开火（复刻 TaCZ ShootKey 里的蓄力 + 开火两步） */
    private static void fireNow(IClientPlayerGunOperator operator, int tick) {
        // 先置位再开火：shoot() 里会经过 LocalPlayerShootMixin，
        // 靠这个标记放行我们自己这一发
        AdsFireState.markFired(tick);
        boolean shootDown = ShootKey.SHOOT_KEY != null && ShootKey.SHOOT_KEY.m_90857_();
        try {
            operator.chargeShoot(shootDown);
            operator.shoot();
        } catch (RuntimeException ignored) {
            // 打不出去就这一发算了，别卡着不放
        }
    }

    /** 结束会话并收镜（不抢玩家自己按着的开镜键） */
    private static void releaseAim(IClientPlayerGunOperator operator) {
        boolean manualAim = AimKey.AIM_KEY != null && AimKey.AIM_KEY.m_90857_();
        boolean wasAutoAimed = AdsFireState.isAutoAimed();
        boolean aiming = operator.isAim();
        AdsFireState.end();
        if (wasAutoAimed && !manualAim && aiming) {
            operator.aim(false);
        }
    }

    /** 枪的 display 还没加载出来时别强开镜，会很难受 */
    private static boolean hasDisplay(ItemStack main) {
        try {
            return TimelessAPI.getGunDisplay(main).isPresent();
        } catch (RuntimeException e) {
            return false;
        }
    }
}
