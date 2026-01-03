package net.j40climb.florafauna.common.block.iteminput.storageanchor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.ResourceHandler;

import javax.annotation.Nullable;

/**
 * Represents a storage container that can receive items from the ItemInput system.
 * Wraps a BlockPos and provides access to the container's item handler capability.
 *
 * @param pos Position of the storage container
 * @param priority Priority for item insertion (higher = first choice)
 * @param linked Whether this was explicitly linked (true) or auto-detected (false)
 */
public record StorageDestination(
        BlockPos pos,
        int priority,
        boolean linked
) {
    /**
     * Codec for NBT persistence.
     */
    public static final Codec<StorageDestination> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    BlockPos.CODEC.fieldOf("pos").forGetter(StorageDestination::pos),
                    Codec.INT.optionalFieldOf("priority", 0).forGetter(StorageDestination::priority),
                    Codec.BOOL.optionalFieldOf("linked", false).forGetter(StorageDestination::linked)
            ).apply(builder, StorageDestination::new)
    );

    /**
     * StreamCodec for network synchronization.
     */
    public static final StreamCodec<FriendlyByteBuf, StorageDestination> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public StorageDestination decode(FriendlyByteBuf buf) {
            return new StorageDestination(
                    buf.readBlockPos(),
                    buf.readVarInt(),
                    buf.readBoolean()
            );
        }

        @Override
        public void encode(FriendlyByteBuf buf, StorageDestination dest) {
            buf.writeBlockPos(dest.pos());
            buf.writeVarInt(dest.priority());
            buf.writeBoolean(dest.linked());
        }
    };

    /**
     * Creates a new auto-detected storage destination with default priority.
     */
    public static StorageDestination autoDetected(BlockPos pos) {
        return new StorageDestination(pos, 0, false);
    }

    /**
     * Creates a new explicitly linked storage destination with high priority.
     */
    public static StorageDestination linked(BlockPos pos) {
        return new StorageDestination(pos, 100, true);
    }

    /**
     * Creates a linked destination with custom priority.
     */
    public static StorageDestination linked(BlockPos pos, int priority) {
        return new StorageDestination(pos, priority, true);
    }

    /**
     * Gets the ResourceHandler<ItemResource> capability from this destination, if available.
     * Uses the new NeoForge 21.9+ Transfer API.
     *
     * @param level The level containing the storage
     * @param accessSide The side to access the storage from (null for any side)
     * @return The resource handler, or null if not available
     */
    @Nullable
    public ResourceHandler<ItemResource> getResourceHandler(Level level, @Nullable Direction accessSide) {
        return level.getCapability(Capabilities.Item.BLOCK, pos, accessSide);
    }

    /**
     * Checks if this storage destination is still valid (block entity exists and has capability).
     */
    public boolean isValid(Level level) {
        return getResourceHandler(level, null) != null;
    }

    /**
     * Returns a new StorageDestination with updated priority.
     */
    public StorageDestination withPriority(int newPriority) {
        return new StorageDestination(pos, newPriority, linked);
    }
}
