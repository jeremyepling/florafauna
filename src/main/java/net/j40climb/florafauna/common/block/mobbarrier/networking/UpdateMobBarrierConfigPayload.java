package net.j40climb.florafauna.common.block.mobbarrier.networking;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.DecoderException;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.mobbarrier.data.MobBarrierConfig;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Network packet to update MobBarrier configuration.
 * Sent from client to server when the player changes config settings in the GUI.
 */
public record UpdateMobBarrierConfigPayload(MobBarrierConfig config) implements CustomPacketPayload {

    public static final Type<UpdateMobBarrierConfigPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "update_mob_barrier_config")
    );

    private static final int MAX_EDIT_BYTES = 32767;
    private static final StreamCodec<ByteBuf, List<String>> EDIT_LIST =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(MAX_EDIT_BYTES));

    // Same two lists and flag as the item component. Bound untrusted edits before allocating.
    public static final StreamCodec<ByteBuf, UpdateMobBarrierConfigPayload> STREAM_CODEC = StreamCodec.of(
            (buf, update) -> MobBarrierConfig.STREAM_CODEC.encode(buf, update.config),
            buf -> {
                if (buf.readableBytes() > MAX_EDIT_BYTES) {
                    throw new DecoderException("Mob barrier edit exceeds " + MAX_EDIT_BYTES + " bytes");
                }
                return new UpdateMobBarrierConfigPayload(new MobBarrierConfig(
                        EDIT_LIST.decode(buf), EDIT_LIST.decode(buf), buf.readBoolean()));
            });

    public static boolean fitsEditPacket(MobBarrierConfig config) {
        return encodedListSize(config.entityIds()) + encodedListSize(config.entityTags()) + 1 <= MAX_EDIT_BYTES;
    }

    private static long encodedListSize(List<String> entries) {
        long size = VarInt.getByteSize(entries.size());
        for (String entry : entries) {
            int bytes = ByteBufUtil.utf8Bytes(entry);
            size += VarInt.getByteSize(bytes) + (long) bytes;
            if (size > MAX_EDIT_BYTES) break;
        }
        return size;
    }

    public static void onServerReceived(final UpdateMobBarrierConfigPayload data, final IPayloadContext context) {
        data.applyTo(context.player());
    }

    public void applyTo(Player player) {
        ItemStack heldItem = player.getMainHandItem();

        if (heldItem.is(FloraFaunaRegistry.MOB_BARRIER.asItem())
                && fitsEditPacket(config)
                && config.entityIds().stream().allMatch(MobBarrierConfig::isValidEntityId)
                && config.entityTags().stream().allMatch(MobBarrierConfig::isValidTagFormat)) {
            heldItem.set(FloraFaunaRegistry.MOB_BARRIER_CONFIG.get(), config);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
