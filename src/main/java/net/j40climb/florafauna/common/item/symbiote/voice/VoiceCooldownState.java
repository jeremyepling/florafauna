package net.j40climb.florafauna.common.item.symbiote.voice;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.j40climb.florafauna.common.item.symbiote.observation.ObservationCategory;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks voice cooldown state for a player.
 * Manages global tier cooldowns, per-category dampening, and post-tier-2 lockout.
 */
public record VoiceCooldownState(
    long lastTier1Tick,
    long lastTier2Tick,
    Map<String, Long> categoryLastSpokeTick,
    long tier2LockoutUntilTick
) {
    /**
     * Codec for NBT persistence
     */
    public static final Codec<VoiceCooldownState> CODEC = RecordCodecBuilder.create(builder ->
        builder.group(
            Codec.LONG.fieldOf("lastTier1Tick").forGetter(VoiceCooldownState::lastTier1Tick),
            Codec.LONG.fieldOf("lastTier2Tick").forGetter(VoiceCooldownState::lastTier2Tick),
            Codec.unboundedMap(Codec.STRING, Codec.LONG)
                .fieldOf("categoryLastSpokeTick")
                .forGetter(VoiceCooldownState::categoryLastSpokeTick),
            Codec.LONG.fieldOf("tier2LockoutUntilTick").forGetter(VoiceCooldownState::tier2LockoutUntilTick)
        ).apply(builder, VoiceCooldownState::new));

    /**
     * StreamCodec for network synchronization
     */
    public static final StreamCodec<ByteBuf, VoiceCooldownState> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_LONG, VoiceCooldownState::lastTier1Tick,
        ByteBufCodecs.VAR_LONG, VoiceCooldownState::lastTier2Tick,
        ByteBufCodecs.map(
            HashMap::new,
            ByteBufCodecs.STRING_UTF8,
            ByteBufCodecs.VAR_LONG
        ), VoiceCooldownState::categoryLastSpokeTick,
        ByteBufCodecs.VAR_LONG, VoiceCooldownState::tier2LockoutUntilTick,
        VoiceCooldownState::new
    );

    /**
     * Sentinel value indicating "never used" for cooldown timestamps.
     * Using Long.MIN_VALUE ensures any currentTick will exceed the cooldown.
     */
    private static final long NEVER_USED = Long.MIN_VALUE;

    /**
     * Default state: no cooldowns active (never spoken)
     */
    public static final VoiceCooldownState DEFAULT = new VoiceCooldownState(
        NEVER_USED, NEVER_USED, new HashMap<>(), 0L
    );

    /**
     * Check if the voice can speak given the tier, category, and current tick.
     *
     * @param tier The tier of voice output
     * @param category The observation category
     * @param currentTick Current game time
     * @return true if speaking is allowed
     */
    public boolean canSpeak(VoiceTier tier, ObservationCategory category, long currentTick) {
        // Check tier-2 lockout for tier-1 messages
        if (tier == VoiceTier.TIER_1_AMBIENT && currentTick < tier2LockoutUntilTick) {
            return false;
        }

        // Check global tier cooldown
        long lastTierTick = (tier == VoiceTier.TIER_1_AMBIENT) ? lastTier1Tick : lastTier2Tick;
        // NEVER_USED means never spoken, so no cooldown applies
        if (lastTierTick != NEVER_USED && currentTick - lastTierTick < tier.getGlobalCooldownTicks()) {
            return false;
        }

        // Check category dampening
        Long lastCategoryTick = categoryLastSpokeTick.get(category.getKey());
        if (lastCategoryTick != null) {
            if (currentTick - lastCategoryTick < tier.getCategoryDampeningTicks()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Create a new state after speaking.
     *
     * @param tier The tier that just spoke
     * @param category The category that just spoke
     * @param currentTick Current game time
     * @return Updated cooldown state
     */
    public VoiceCooldownState afterSpeaking(VoiceTier tier, ObservationCategory category, long currentTick) {
        long newTier1Tick = lastTier1Tick;
        long newTier2Tick = lastTier2Tick;
        long newLockoutUntil = tier2LockoutUntilTick;

        if (tier == VoiceTier.TIER_1_AMBIENT) {
            newTier1Tick = currentTick;
        } else {
            newTier2Tick = currentTick;
            // Tier 2 triggers a lockout for Tier 1
            if (tier.triggersLockout()) {
                newLockoutUntil = currentTick + VoiceTier.POST_TIER2_LOCKOUT_TICKS;
            }
        }

        // Update category timing
        Map<String, Long> newCategoryTicks = new HashMap<>(categoryLastSpokeTick);
        newCategoryTicks.put(category.getKey(), currentTick);

        return new VoiceCooldownState(newTier1Tick, newTier2Tick, newCategoryTicks, newLockoutUntil);
    }

    /**
     * Create a reset state (for debugging)
     */
    public static VoiceCooldownState reset() {
        return DEFAULT;
    }
}
