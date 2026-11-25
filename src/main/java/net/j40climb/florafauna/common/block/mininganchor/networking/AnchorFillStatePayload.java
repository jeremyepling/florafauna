package net.j40climb.florafauna.common.block.mininganchor.networking;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.mininganchor.AnchorFillState;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
//? if neoforge {
import net.neoforged.neoforge.network.handling.IPayloadContext;
//?} else {
/*import net.j40climb.florafauna.platform.network.IPayloadContext;
*///?}

/**
 * Server -> Client payload to sync anchor fill state to bound players.
 * Updates the lastAnnouncedFillState in PlayerSymbioteData.
 */
public record AnchorFillStatePayload(BlockPos anchorPos, AnchorFillState fillState) implements CustomPacketPayload {

    public static final Type<AnchorFillStatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "anchor_fill_state"));

    public static final StreamCodec<ByteBuf, AnchorFillStatePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AnchorFillStatePayload::anchorPos,
            AnchorFillState.STREAM_CODEC, AnchorFillStatePayload::fillState,
            AnchorFillStatePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Client-side handler: updates the player's symbiote data with the new fill state.
     */
    public static void handleClient(AnchorFillStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            var currentData = net.j40climb.florafauna.platform.AttachmentAccess.get(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);

            // Only update if this is for the player's active waypoint anchor
            if (currentData.hasActiveWaypointAnchor() &&
                currentData.activeWaypointAnchorPos() != null &&
                currentData.activeWaypointAnchorPos().equals(payload.anchorPos())) {

                var newData = currentData.withLastAnnouncedFillState(payload.fillState());
                net.j40climb.florafauna.platform.AttachmentAccess.set(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA, newData);
            }
        });
    }
}
