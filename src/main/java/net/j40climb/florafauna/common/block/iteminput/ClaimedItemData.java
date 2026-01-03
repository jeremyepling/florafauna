package net.j40climb.florafauna.common.block.iteminput;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Entity attachment data for items claimed by an ItemInput block.
 * Attached to ItemEntity instances to track ownership and animation state.
 *
 * Lifecycle:
 * 1. ItemInput claims an item entity → attaches ClaimedItemData
 * 2. Animation plays on client (item moves toward block)
 * 3. After animationDuration ticks, item is absorbed into buffer
 * 4. ItemEntity is discarded
 *
 * @param claimed Whether this item is claimed (always true when attached)
 * @param itemInputPos Position of the ItemInput block that claimed this item
 * @param claimedAtTick Server tick when the item was claimed
 * @param animationDuration Duration in ticks for the absorption animation
 */
public record ClaimedItemData(
        boolean claimed,
        BlockPos itemInputPos,
        long claimedAtTick,
        int animationDuration
) {
    /**
     * Codec for persistence (not typically needed for entity attachments, but good practice).
     */
    public static final Codec<ClaimedItemData> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    Codec.BOOL.fieldOf("claimed").forGetter(ClaimedItemData::claimed),
                    BlockPos.CODEC.fieldOf("itemInputPos").forGetter(ClaimedItemData::itemInputPos),
                    Codec.LONG.fieldOf("claimedAtTick").forGetter(ClaimedItemData::claimedAtTick),
                    Codec.INT.fieldOf("animationDuration").forGetter(ClaimedItemData::animationDuration)
            ).apply(builder, ClaimedItemData::new)
    );

    /**
     * StreamCodec for network synchronization.
     */
    public static final StreamCodec<ByteBuf, ClaimedItemData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ClaimedItemData::claimed,
            BlockPos.STREAM_CODEC.cast(), ClaimedItemData::itemInputPos,
            ByteBufCodecs.VAR_LONG, ClaimedItemData::claimedAtTick,
            ByteBufCodecs.VAR_INT, ClaimedItemData::animationDuration,
            ClaimedItemData::new
    );

    /**
     * Default state: not claimed.
     */
    public static final ClaimedItemData DEFAULT = new ClaimedItemData(false, BlockPos.ZERO, 0L, 0);

    /**
     * Creates a new ClaimedItemData for an item that has just been claimed.
     *
     * @param itemInputPos Position of the claiming ItemInput block
     * @param currentTick Current server tick
     * @param animationDuration Duration of the absorption animation
     * @return New ClaimedItemData instance
     */
    public static ClaimedItemData create(BlockPos itemInputPos, long currentTick, int animationDuration) {
        return new ClaimedItemData(true, itemInputPos, currentTick, animationDuration);
    }

    /**
     * Checks if the animation has completed based on the current tick.
     *
     * @param currentTick Current server tick
     * @return true if the animation duration has elapsed
     */
    public boolean isAnimationComplete(long currentTick) {
        return claimed && (currentTick - claimedAtTick) >= animationDuration;
    }

    /**
     * Gets the progress of the animation as a value from 0.0 to 1.0.
     *
     * @param currentTick Current tick
     * @return Animation progress (clamped to 0.0-1.0)
     */
    public float getAnimationProgress(long currentTick) {
        if (!claimed || animationDuration <= 0) {
            return 0.0f;
        }
        float progress = (float) (currentTick - claimedAtTick) / animationDuration;
        return Math.min(1.0f, Math.max(0.0f, progress));
    }
}
