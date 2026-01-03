package net.j40climb.florafauna.common.block.iteminput.rootiteminput.networking;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.client.iteminput.ItemInputAnimationTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent from server to client when an item entity is claimed by an Item Input block.
 * Client uses this to start the visual absorption animation.
 */
public record ItemInputAnimationPayload(
        int entityId,
        BlockPos targetPos,
        int animationDurationTicks
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ItemInputAnimationPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "item_input_animation"));

    public static final StreamCodec<FriendlyByteBuf, ItemInputAnimationPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ItemInputAnimationPayload::entityId,
                    BlockPos.STREAM_CODEC, ItemInputAnimationPayload::targetPos,
                    ByteBufCodecs.VAR_INT, ItemInputAnimationPayload::animationDurationTicks,
                    ItemInputAnimationPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Handles the payload on the client side.
     */
    public static void onClientReceived(ItemInputAnimationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ItemInputAnimationTracker.startAnimation(
                    payload.entityId(),
                    payload.targetPos(),
                    payload.animationDurationTicks()
            );
        });
    }
}
