package net.j40climb.florafauna.common.entity.frenchie.frontpack.networking;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.entity.frenchie.frontpack.FrontpackEvents;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Network payload for putting down a carried Frenchie.
 * Client sends this when player shift-right-clicks air while carrying a Frenchie.
 * Server handles by spawning the Frenchie from NBT and clearing the attachment.
 */
public enum PutDownFrenchiePayload implements CustomPacketPayload {
    INSTANCE;

    public static final Type<PutDownFrenchiePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "put_down_frenchie"));

    public static final StreamCodec<ByteBuf, PutDownFrenchiePayload> STREAM_CODEC =
        StreamCodec.unit(INSTANCE);

    public static void onServerReceived(final PutDownFrenchiePayload data, final IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.isAlive() && !player.isSpectator() && player.isShiftKeyDown()) {
            FrontpackEvents.putDown(player);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
