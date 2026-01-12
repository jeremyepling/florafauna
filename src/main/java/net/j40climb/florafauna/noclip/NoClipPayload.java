package net.j40climb.florafauna.noclip;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.FloraFauna;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Network payload to sync no-clip state from client to server.
 * When received on the server, updates the player's clipping state via the ClippingEntity interface.
 */
public record NoClipPayload(boolean clipping) implements CustomPacketPayload {

    public static final Type<NoClipPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "noclip"));

    public static final StreamCodec<ByteBuf, NoClipPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, NoClipPayload::clipping,
            NoClipPayload::new
    );

    /**
     * Handles the payload when received on the server.
     * Sets the clipping state on the ServerPlayer via the ClippingEntity mixin interface.
     */
    public static void onServerReceived(final NoClipPayload data, final IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            // Cast to ClippingEntity (implemented by PlayerNoClipMixin)
            if (serverPlayer instanceof ClippingEntity clippingPlayer) {
                // Only allow clipping if player can clip (creative mode check)
                if (clippingPlayer.canClip()) {
                    clippingPlayer.setClipping(data.clipping());
                } else {
                    // Force disable if player is not allowed to clip
                    clippingPlayer.setClipping(false);
                }
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
