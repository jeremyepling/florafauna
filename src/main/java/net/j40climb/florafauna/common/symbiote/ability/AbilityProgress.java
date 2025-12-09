package net.j40climb.florafauna.common.symbiote.ability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Tracks progress toward unlocking a single symbiote ability.
 *
 * @param currentCount Number of items consumed toward this ability
 * @param unlocked     Whether the ability has been unlocked
 * @param active       Whether the player has this ability enabled
 */
public record AbilityProgress(int currentCount, boolean unlocked, boolean active) {

    public static final AbilityProgress DEFAULT = new AbilityProgress(0, false, false);

    public static final Codec<AbilityProgress> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    Codec.INT.fieldOf("currentCount").forGetter(AbilityProgress::currentCount),
                    Codec.BOOL.fieldOf("unlocked").forGetter(AbilityProgress::unlocked),
                    Codec.BOOL.fieldOf("active").forGetter(AbilityProgress::active)
            ).apply(builder, AbilityProgress::new));

    public static final StreamCodec<ByteBuf, AbilityProgress> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, AbilityProgress::currentCount,
            ByteBufCodecs.BOOL, AbilityProgress::unlocked,
            ByteBufCodecs.BOOL, AbilityProgress::active,
            AbilityProgress::new
    );

    /**
     * Adds items toward this ability's progress.
     *
     * @param amount   Number of items to add
     * @param required Total items required to unlock
     * @return New AbilityProgress with updated count and possibly unlocked
     */
    public AbilityProgress addProgress(int amount, int required) {
        int newCount = Math.min(currentCount + amount, required);
        boolean nowUnlocked = unlocked || newCount >= required;
        return new AbilityProgress(newCount, nowUnlocked, active);
    }

    /**
     * Toggles whether this ability is active (only works if unlocked).
     */
    public AbilityProgress toggleActive() {
        if (!unlocked) {
            return this;
        }
        return new AbilityProgress(currentCount, unlocked, !active);
    }

    /**
     * Sets whether this ability is active (only works if unlocked).
     */
    public AbilityProgress setActive(boolean active) {
        if (!unlocked) {
            return this;
        }
        return new AbilityProgress(currentCount, unlocked, active);
    }

    /**
     * Gets the progress percentage (0.0 to 1.0).
     */
    public float getProgressPercent(int required) {
        if (required <= 0) return 1.0f;
        return Math.min((float) currentCount / required, 1.0f);
    }
}