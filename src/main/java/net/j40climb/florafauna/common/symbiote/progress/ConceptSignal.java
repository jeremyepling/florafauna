package net.j40climb.florafauna.common.symbiote.progress;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Tracks progress on a single concept/feature.
 * Stores state, timestamps, interaction counts, and stall score.
 */
public record ConceptSignal(
    String conceptId,
    SignalState state,
    long lastTransitionTick,
    int interactionCount,
    int stallScore
) {
    /**
     * Codec for enum values using string names
     */
    private static final Codec<SignalState> STATE_CODEC = Codec.STRING.xmap(
        name -> SignalState.valueOf(name.toUpperCase()),
        state -> state.name().toLowerCase()
    );

    /**
     * StreamCodec for SignalState enum using ordinal
     */
    private static final StreamCodec<ByteBuf, SignalState> STATE_STREAM_CODEC = ByteBufCodecs.INT.map(
        ordinal -> SignalState.values()[ordinal],
        SignalState::ordinal
    );

    /**
     * Codec for NBT persistence
     */
    public static final Codec<ConceptSignal> CODEC = RecordCodecBuilder.create(builder ->
        builder.group(
            Codec.STRING.fieldOf("conceptId").forGetter(ConceptSignal::conceptId),
            STATE_CODEC.fieldOf("state").forGetter(ConceptSignal::state),
            Codec.LONG.fieldOf("lastTransitionTick").forGetter(ConceptSignal::lastTransitionTick),
            Codec.INT.fieldOf("interactionCount").forGetter(ConceptSignal::interactionCount),
            Codec.INT.fieldOf("stallScore").forGetter(ConceptSignal::stallScore)
        ).apply(builder, ConceptSignal::new));

    /**
     * StreamCodec for network synchronization
     */
    public static final StreamCodec<ByteBuf, ConceptSignal> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, ConceptSignal::conceptId,
        STATE_STREAM_CODEC, ConceptSignal::state,
        ByteBufCodecs.VAR_LONG, ConceptSignal::lastTransitionTick,
        ByteBufCodecs.INT, ConceptSignal::interactionCount,
        ByteBufCodecs.INT, ConceptSignal::stallScore,
        ConceptSignal::new
    );

    /**
     * Create a new signal for a concept that has just been seen
     */
    public static ConceptSignal firstSeen(String conceptId, long currentTick) {
        return new ConceptSignal(conceptId, SignalState.SEEN, currentTick, 1, 0);
    }

    /**
     * Create a copy with updated state
     */
    public ConceptSignal withState(SignalState newState, long currentTick) {
        return new ConceptSignal(conceptId, newState, currentTick, interactionCount, 0);
    }

    /**
     * Create a copy with incremented interaction count
     */
    public ConceptSignal incrementInteraction(long currentTick) {
        return new ConceptSignal(conceptId, state, currentTick, interactionCount + 1, 0);
    }

    /**
     * Create a copy with updated stall score
     */
    public ConceptSignal withStallScore(int newStallScore) {
        return new ConceptSignal(conceptId, state, lastTransitionTick, interactionCount, newStallScore);
    }

    /**
     * Calculate current stall score based on time since last interaction.
     * Score ranges 0-100. Higher means more stalled.
     *
     * @param currentTick Current game time
     * @return Stall score (0-100)
     */
    public int calculateStallScore(long currentTick) {
        if (state == SignalState.INTEGRATED || state == SignalState.UNSEEN) {
            return 0; // Terminal states don't stall
        }

        long ticksSince = currentTick - lastTransitionTick;
        // 5 hours of game time (360000 ticks) = 100% stalled
        int score = (int) Math.min(100, (ticksSince * 100) / 360000L);
        return Math.max(0, score);
    }

    /**
     * Check if this signal is considered stalled (score >= threshold)
     */
    public boolean isStalled(long currentTick, int threshold) {
        return calculateStallScore(currentTick) >= threshold;
    }
}
