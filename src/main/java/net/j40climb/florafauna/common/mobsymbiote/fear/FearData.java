package net.j40climb.florafauna.common.entity.fear;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

/**
 * Entity attachment data for the fear/stress system.
 * Tracks fear state, timing, and consecutive leak counts.
 * <p>
 * This is a general attachment that works with any fearful mob
 * (Creeper, Enderman, Blaze, etc.).
 *
 * @param fearState Current fear state
 * @param stateEnteredTick Game tick when current state was entered
 * @param leakCountSinceCooldown Consecutive leaks without returning to CALM naturally
 * @param fearSourceX X position of fear source (for avoidance direction)
 * @param fearSourceY Y position of fear source
 * @param fearSourceZ Z position of fear source
 * @param hasFearSourcePos Whether fear source position is set
 */
public record FearData(
        FearState fearState,
        long stateEnteredTick,
        int leakCountSinceCooldown,
        int fearSourceX,
        int fearSourceY,
        int fearSourceZ,
        boolean hasFearSourcePos
) {
    public static final Codec<FearData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Codec.STRING.xmap(FearState::valueOf, FearState::name)
                    .fieldOf("fearState").forGetter(d -> d.fearState),
            Codec.LONG.fieldOf("stateEnteredTick").forGetter(FearData::stateEnteredTick),
            Codec.INT.fieldOf("leakCountSinceCooldown").forGetter(FearData::leakCountSinceCooldown),
            Codec.INT.fieldOf("fearSourceX").forGetter(FearData::fearSourceX),
            Codec.INT.fieldOf("fearSourceY").forGetter(FearData::fearSourceY),
            Codec.INT.fieldOf("fearSourceZ").forGetter(FearData::fearSourceZ),
            Codec.BOOL.fieldOf("hasFearSourcePos").forGetter(FearData::hasFearSourcePos)
    ).apply(builder, FearData::new));

    public static final StreamCodec<ByteBuf, FearData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.map(FearState::valueOf, FearState::name), FearData::fearState,
            ByteBufCodecs.VAR_LONG, FearData::stateEnteredTick,
            ByteBufCodecs.VAR_INT, FearData::leakCountSinceCooldown,
            ByteBufCodecs.VAR_INT, FearData::fearSourceX,
            ByteBufCodecs.VAR_INT, FearData::fearSourceY,
            ByteBufCodecs.VAR_INT, FearData::fearSourceZ,
            ByteBufCodecs.BOOL, FearData::hasFearSourcePos,
            FearData::new
    );

    public static final FearData DEFAULT = new FearData(
            FearState.CALM, 0L, 0, 0, 0, 0, false
    );

    /**
     * Transition to a new fear state.
     *
     * @param newState The new state
     * @param tick Current game tick
     * @return New FearData with updated state
     */
    public FearData withState(FearState newState, long tick) {
        return new FearData(newState, tick, leakCountSinceCooldown,
                fearSourceX, fearSourceY, fearSourceZ, hasFearSourcePos);
    }

    /**
     * Update the leak count.
     *
     * @param count New leak count
     * @return New FearData with updated count
     */
    public FearData withLeakCount(int count) {
        return new FearData(fearState, stateEnteredTick, count,
                fearSourceX, fearSourceY, fearSourceZ, hasFearSourcePos);
    }

    /**
     * Set the fear source position for avoidance direction.
     *
     * @param pos The fear source position
     * @return New FearData with updated position
     */
    public FearData withFearSourcePos(BlockPos pos) {
        return new FearData(fearState, stateEnteredTick, leakCountSinceCooldown,
                pos.getX(), pos.getY(), pos.getZ(), true);
    }

    /**
     * Clear the fear source position.
     *
     * @return New FearData without position
     */
    public FearData withoutFearSourcePos() {
        return new FearData(fearState, stateEnteredTick, leakCountSinceCooldown,
                0, 0, 0, false);
    }

    /**
     * Get the fear source position if set.
     *
     * @return Optional containing the position, or empty if not set
     */
    public Optional<BlockPos> getFearSourcePos() {
        if (hasFearSourcePos) {
            return Optional.of(new BlockPos(fearSourceX, fearSourceY, fearSourceZ));
        }
        return Optional.empty();
    }

    /**
     * Calculate how many ticks the mob has been in the current state.
     *
     * @param currentTick Current game tick
     * @return Ticks in current state
     */
    public long getTicksInState(long currentTick) {
        return currentTick - stateEnteredTick;
    }

    /**
     * Increment the leak count (for tracking consecutive leaks).
     *
     * @return New FearData with incremented count
     */
    public FearData incrementLeakCount() {
        return withLeakCount(leakCountSinceCooldown + 1);
    }

    /**
     * Reset the leak count (when returning to CALM naturally).
     *
     * @return New FearData with zero count
     */
    public FearData resetLeakCount() {
        return withLeakCount(0);
    }
}
