package net.j40climb.florafauna.common.block.mobbarrier.networking;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.mobbarrier.data.MobBarrierConfig;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Network packet to update MobBarrier configuration.
 * Sent from client to server when the player changes config settings in the GUI.
 */
public record UpdateMobBarrierConfigPayload(MobBarrierConfig config) implements CustomPacketPayload {

    public static final Type<UpdateMobBarrierConfigPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "update_mob_barrier_config")
    );

    public static final StreamCodec<ByteBuf, UpdateMobBarrierConfigPayload> STREAM_CODEC = StreamCodec.composite(
            MobBarrierConfig.STREAM_CODEC,
            UpdateMobBarrierConfigPayload::config,
            UpdateMobBarrierConfigPayload::new
    );

    public static void onServerReceived(final UpdateMobBarrierConfigPayload data, final IPayloadContext context) {
        Player player = context.player();
        ItemStack heldItem = player.getMainHandItem();

        if (heldItem.is(FloraFaunaRegistry.MOB_BARRIER.asItem())) {
            heldItem.set(FloraFaunaRegistry.MOB_BARRIER_CONFIG.get(), data.config);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
