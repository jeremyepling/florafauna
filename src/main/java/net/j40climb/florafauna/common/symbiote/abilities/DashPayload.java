package net.j40climb.florafauna.common.symbiote.abilities;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public enum DashPayload implements CustomPacketPayload {
    INSTANCE;

    public static final Type<DashPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "dash_payload"));
    public static final StreamCodec<ByteBuf, DashPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void onServerReceived(DashPayload data, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            dash(player);
        }
    }

    public static void dash(ServerPlayer player) {
        PlayerSymbioteData symbiote = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
        if (!symbiote.symbioteState().areAbilitiesActive() || !symbiote.dash()) {
            return;
        }
        player.setDeltaMovement(player.getViewVector(1.0F).scale(3.0));
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        player.resetFallDistance();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}