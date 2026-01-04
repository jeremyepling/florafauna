package net.j40climb.florafauna.common.block.vacuum;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Entity attachment data that tracks whether an ItemEntity originated from a block drop.
 * This allows vacuum blocks to filter and only collect items from mining/breaking blocks.
 *
 * @param isBlockDrop Whether this item was dropped from a block
 * @param droppedAtTick Server tick when the item was dropped
 */
public record BlockDropData(
        boolean isBlockDrop,
        long droppedAtTick
) {
    /**
     * Codec for persistence.
     */
    public static final Codec<BlockDropData> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    Codec.BOOL.fieldOf("isBlockDrop").forGetter(BlockDropData::isBlockDrop),
                    Codec.LONG.fieldOf("droppedAtTick").forGetter(BlockDropData::droppedAtTick)
            ).apply(builder, BlockDropData::new)
    );

    /**
     * StreamCodec for network synchronization.
     */
    public static final StreamCodec<ByteBuf, BlockDropData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, BlockDropData::isBlockDrop,
            ByteBufCodecs.VAR_LONG, BlockDropData::droppedAtTick,
            BlockDropData::new
    );

    /**
     * Default state: not a block drop.
     */
    public static final BlockDropData DEFAULT = new BlockDropData(false, 0L);

    /**
     * Creates a BlockDropData for an item that was just dropped from a block.
     *
     * @param currentTick Current server tick
     * @return New BlockDropData instance
     */
    public static BlockDropData create(long currentTick) {
        return new BlockDropData(true, currentTick);
    }
}
