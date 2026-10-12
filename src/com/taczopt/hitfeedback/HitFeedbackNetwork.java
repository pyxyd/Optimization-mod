package com.taczopt.hitfeedback;

import com.taczopt.TaczOptimizeMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 命中标记的网络通道。
 *
 * <p>为什么要自己开一条通道：TaCZ 的 {@code EntityHurtByGunEvent} 里写死了
 * {@code LogicalSide.SERVER}，客户端根本收不到；它自己同步给客户端的
 * {@code ServerMessageGunHurt} 只发给"目标附近的玩家"，射手离得远就看不到。
 * 所以"谁打中了"这件事必须由服务端主动通知射手本人，只能自己发。
 *
 * <p>⚠ 通道名必须独一无二（{@code taczopt:hit}）。如果和别的模组撞名，
 * Forge 会在通道注册阶段直接抛异常。
 *
 * <p>⚠ 版本串取 {@code "1.0.5"}，跟 TaCZ 的
 * {@code com.tacz.guns.network.NetworkHandler.VERSION} 保持一致 ——
 * 这个串参与 Forge 的通道握手，写得不一致会导致连不上服务器。
 */
public final class HitFeedbackNetwork {

    private static final String CHANNEL_NAME = "taczopt:hit";
    private static final String VERSION = "1.0.5";
    private static int nextId = 0;

    /**
     * 频道 id。
     *
     * <p>⚠ 必须用 {@code new ResourceLocation(String)}，而它在 1.20.1 标了
     * {@code @Deprecated(forRemoval = true)}。试过 {@code ResourceLocation.tryParse}
     * —— <b>这个方法在 1.20.1 不存在</b>（javac: 找不到符号）。所以保留旧构造器并
     * 抑制警告：不抑制的话 javac 会写 stderr，而 build.ps1 把"javac 的 stderr
     * 非空"当成构建失败信号（详见其中 Invoke-Native 的注释）。
     * 这里的字符串是写死的合法常量，不存在解析失败的可能。
     */
    @SuppressWarnings({"deprecation", "removal"})
    private static final ResourceLocation CHANNEL_ID = new ResourceLocation(CHANNEL_NAME);

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            CHANNEL_ID,
            () -> VERSION,
            VERSION::equals,
            VERSION::equals);

    private HitFeedbackNetwork() {
    }

    /** 只在 mod 构造里调用一次 */
    public static void register() {
        CHANNEL.registerMessage(nextId++, HitFeedbackMessage.class,
                HitFeedbackMessage::encode,
                HitFeedbackMessage::decode,
                HitFeedbackMessage::handle);
    }

    /** 服务端调用：把一次命中告诉某个玩家（通常是射手） */
    public static void sendToPlayer(HitFeedbackMessage msg, net.minecraft.server.level.ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    /**
     * 一次命中的标记数据。
     *
     * <p>只带"怎么画"所需的最少信息 —— 伤害数值不需要走网络：
     * 之前那版要显示伤害数字，还得在服务端量血量差、把两个浮点数发过来；
     * 现在标记只看"打中头没有 / 打没打死"，包和逻辑都小了一圈。
     *
     * @param headshot 是否爆头（决定标记颜色：爆头橙色，否则白色）
     * @param killed   是否致死（红色，优先级高于爆头）
     * @param gunId    枪械 id，仅用于调试日志，可能为空串
     */
    public record HitFeedbackMessage(boolean headshot, boolean killed, String gunId) {

        public void encode(FriendlyByteBuf buf) {
            buf.writeBoolean(headshot);
            buf.writeBoolean(killed);
            // SRG 名：writeUtf(String,int) = m_130072_，readUtf(int) = m_130136_
            buf.m_130072_(gunId == null ? "" : gunId, 128);
        }

        public static HitFeedbackMessage decode(FriendlyByteBuf buf) {
            return new HitFeedbackMessage(buf.readBoolean(), buf.readBoolean(), buf.m_130136_(128));
        }

        public void handle(java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                try {
                    HitFeedbackClient.onHit(this);
                } catch (Throwable t) {
                    TaczOptimizeMod.logger().warn("[taczopt] 命中标记处理失败", t);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
