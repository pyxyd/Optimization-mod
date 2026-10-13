package com.taczopt.hitfeedback;

import com.taczopt.TaczOptimizeMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class HitFeedbackNetwork {

    private static final String CHANNEL_NAME = "taczopt:hit";
    private static final String VERSION = "1.0.5";
    private static int nextId = 0;

    @SuppressWarnings({"deprecation", "removal"})
    private static final ResourceLocation CHANNEL_ID = new ResourceLocation(CHANNEL_NAME);

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            CHANNEL_ID,
            () -> VERSION,
            VERSION::equals,
            VERSION::equals);

    private HitFeedbackNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(nextId++, HitFeedbackMessage.class,
                HitFeedbackMessage::encode,
                HitFeedbackMessage::decode,
                HitFeedbackMessage::handle);
    }

    public static void sendToPlayer(HitFeedbackMessage msg, net.minecraft.server.level.ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public record HitFeedbackMessage(boolean headshot, boolean killed, String gunId) {

        public void encode(FriendlyByteBuf buf) {
            buf.writeBoolean(headshot);
            buf.writeBoolean(killed);

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
