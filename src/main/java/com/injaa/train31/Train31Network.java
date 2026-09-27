package com.injaa.train31;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class Train31Network {
    private Train31Network() {}
    private static final String PROTOCOL = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(Train31Mod.MODID, "cinematic"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    public static void init() {
        CHANNEL.messageBuilder(ClientState.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientState::encode)
                .decoder(ClientState::decode)
                .consumerMainThread(ClientState::handle)
                .add();
    }

    public record ClientState(int storyTick, float fogStrength, int cameraEntityId, boolean cctv, boolean storyActive) {
        static void encode(ClientState m, FriendlyByteBuf b) {
            b.writeVarInt(m.storyTick);
            b.writeFloat(m.fogStrength);
            b.writeInt(m.cameraEntityId);
            b.writeBoolean(m.cctv);
            b.writeBoolean(m.storyActive);
        }
        static ClientState decode(FriendlyByteBuf b) {
            return new ClientState(b.readVarInt(), b.readFloat(), b.readInt(), b.readBoolean(), b.readBoolean());
        }
        static void handle(ClientState m, Supplier<NetworkEvent.Context> supplier) {
            supplier.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.injaa.train31.client.ClientRuntime.accept(m)));
            supplier.get().setPacketHandled(true);
        }
    }
}
