package net.j40climb.florafauna.common.symbiote.tracking;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tracks first-time events for symbiote dialogue triggers.
 * Uses a Set to store which events have been triggered.
 * Persists across bonding/unbonding cycles - the symbiote "remembers" experiences.
 */
public record SymbioteEventTracker(Set<String> triggeredEvents) {

    /**
     * Codec for NBT persistence using a list internally
     */
    public static final Codec<SymbioteEventTracker> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    Codec.STRING.listOf()
                            .xmap(
                                    list -> (Set<String>) new HashSet<>(list),
                                    set -> List.copyOf(set)
                            )
                            .fieldOf("triggeredEvents")
                            .forGetter(SymbioteEventTracker::triggeredEvents)
            ).apply(builder, SymbioteEventTracker::new));

    /**
     * StreamCodec for network synchronization
     */
    public static final StreamCodec<ByteBuf, SymbioteEventTracker> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).map(
                    HashSet::new,
                    set -> List.copyOf(set)
            ),
            SymbioteEventTracker::triggeredEvents,
            SymbioteEventTracker::new
    );

    /**
     * Default state: no events triggered
     */
    public static final SymbioteEventTracker DEFAULT = new SymbioteEventTracker(new HashSet<>());

    /**
     * Check if an event has been triggered before
     */
    public boolean hasTriggered(String eventKey) {
        return triggeredEvents.contains(eventKey);
    }

    /**
     * Mark an event as triggered and return a new tracker
     */
    public SymbioteEventTracker withTriggered(String eventKey) {
        Set<String> newSet = new HashSet<>(triggeredEvents);
        newSet.add(eventKey);
        return new SymbioteEventTracker(newSet);
    }
}
