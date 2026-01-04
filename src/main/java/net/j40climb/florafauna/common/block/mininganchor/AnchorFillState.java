package net.j40climb.florafauna.common.block.mininganchor;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * Fill state for Mining Anchor blocks.
 * Used for waypoint color and symbiote dialogue triggers.
 */
public enum AnchorFillState implements StringRepresentable {
    NORMAL("normal"),   // < 75% full
    WARNING("warning"), // >= 75% full
    FULL("full");       // 100% full

    public static final Codec<AnchorFillState> CODEC = StringRepresentable.fromEnum(AnchorFillState::values);

    public static final StreamCodec<ByteBuf, AnchorFillState> STREAM_CODEC =
            ByteBufCodecs.idMapper(i -> values()[i], AnchorFillState::ordinal);

    private final String name;

    AnchorFillState(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /**
     * Determines the fill state based on stored and max capacity.
     *
     * @param storedCount Current number of items stored
     * @param maxCapacity Maximum item capacity
     * @return The appropriate fill state
     */
    public static AnchorFillState fromFillRatio(int storedCount, int maxCapacity) {
        if (maxCapacity <= 0) {
            return NORMAL;
        }
        float ratio = (float) storedCount / maxCapacity;
        if (ratio >= 1.0f) {
            return FULL;
        } else if (ratio >= 0.75f) {
            return WARNING;
        } else {
            return NORMAL;
        }
    }

    /**
     * Returns the warning threshold ratio (0.75).
     */
    public static float getWarningThreshold() {
        return 0.75f;
    }
}
