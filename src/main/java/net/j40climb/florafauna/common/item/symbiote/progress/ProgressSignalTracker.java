package net.j40climb.florafauna.common.item.symbiote.progress;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tracks progress signals for all concepts and dream state for a player.
 * Uses a state machine (SEEN -> TOUCHED -> STABILIZED -> INTEGRATED or NEGLECTED)
 * to track concept progression and stall detection for dreams.
 */
public record ProgressSignalTracker(
    Map<String, ConceptSignal> signals,
    long lastDreamTick,
    int dreamLevel,
    long lastProgressTick
) {
    /**
     * Codec for NBT persistence using unbounded map
     */
    public static final Codec<ProgressSignalTracker> CODEC = RecordCodecBuilder.create(builder ->
        builder.group(
            Codec.unboundedMap(Codec.STRING, ConceptSignal.CODEC)
                .fieldOf("signals")
                .forGetter(ProgressSignalTracker::signals),
            Codec.LONG.fieldOf("lastDreamTick").forGetter(ProgressSignalTracker::lastDreamTick),
            Codec.INT.fieldOf("dreamLevel").forGetter(ProgressSignalTracker::dreamLevel),
            Codec.LONG.fieldOf("lastProgressTick").forGetter(ProgressSignalTracker::lastProgressTick)
        ).apply(builder, ProgressSignalTracker::new));

    /**
     * StreamCodec for network synchronization
     */
    public static final StreamCodec<ByteBuf, ProgressSignalTracker> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.map(
            HashMap::new,
            ByteBufCodecs.STRING_UTF8,
            ConceptSignal.STREAM_CODEC
        ), ProgressSignalTracker::signals,
        ByteBufCodecs.VAR_LONG, ProgressSignalTracker::lastDreamTick,
        ByteBufCodecs.INT, ProgressSignalTracker::dreamLevel,
        ByteBufCodecs.VAR_LONG, ProgressSignalTracker::lastProgressTick,
        ProgressSignalTracker::new
    );

    /**
     * Default state: no signals tracked, no dreams
     */
    public static final ProgressSignalTracker DEFAULT = new ProgressSignalTracker(
        new HashMap<>(), 0L, 0, 0L
    );

    /**
     * Get a specific concept signal, if it exists
     */
    public Optional<ConceptSignal> getSignal(String conceptId) {
        return Optional.ofNullable(signals.get(conceptId));
    }

    /**
     * Check if a concept has reached at least a certain state
     */
    public boolean hasReachedState(String conceptId, SignalState minState) {
        return getSignal(conceptId)
            .map(signal -> signal.state().getProgressLevel() >= minState.getProgressLevel())
            .orElse(false);
    }

    /**
     * Create a copy with an updated signal
     */
    public ProgressSignalTracker withSignalUpdated(String conceptId, ConceptSignal signal) {
        Map<String, ConceptSignal> newSignals = new HashMap<>(signals);
        newSignals.put(conceptId, signal);
        return new ProgressSignalTracker(newSignals, lastDreamTick, dreamLevel, lastProgressTick);
    }

    /**
     * Create a copy with updated dream state
     */
    public ProgressSignalTracker withDreamState(long dreamTick, int newDreamLevel) {
        return new ProgressSignalTracker(signals, dreamTick, newDreamLevel, lastProgressTick);
    }

    /**
     * Create a copy with updated last progress tick
     */
    public ProgressSignalTracker withProgressTick(long tick) {
        return new ProgressSignalTracker(signals, lastDreamTick, dreamLevel, tick);
    }

    /**
     * Get all signals that are considered stalled
     *
     * @param currentTick Current game time
     * @param threshold Minimum stall score to be considered stalled (0-100)
     * @return List of stalled signals, sorted by stall score descending
     */
    public List<ConceptSignal> getStalledSignals(long currentTick, int threshold) {
        return signals.values().stream()
            .filter(signal -> signal.isStalled(currentTick, threshold))
            .sorted((a, b) -> Integer.compare(
                b.calculateStallScore(currentTick),
                a.calculateStallScore(currentTick)
            ))
            .collect(Collectors.toList());
    }

    /**
     * Get signals in a specific state
     */
    public List<ConceptSignal> getSignalsInState(SignalState state) {
        return signals.values().stream()
            .filter(signal -> signal.state() == state)
            .collect(Collectors.toList());
    }

    /**
     * Get all signals that are partially complete (TOUCHED or STABILIZED)
     */
    public List<ConceptSignal> getPartiallyCompleteSignals() {
        return signals.values().stream()
            .filter(signal -> signal.state() == SignalState.TOUCHED
                           || signal.state() == SignalState.STABILIZED)
            .collect(Collectors.toList());
    }

    /**
     * Check if any meaningful progress has been made since the last dream
     */
    public boolean hasProgressSinceLastDream() {
        return lastProgressTick > lastDreamTick;
    }
}
