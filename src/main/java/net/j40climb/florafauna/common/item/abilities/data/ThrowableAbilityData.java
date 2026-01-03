package net.j40climb.florafauna.common.item.abilities.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Configuration data for throwable item ability.
 * When attached to an item, allows it to be thrown as a projectile.
 *
 * @param damage Damage dealt on entity hit (0 = no damage)
 * @param maxRange Maximum distance before auto-return (only used if autoReturn=true)
 * @param breakBlocks Whether to break blocks on contact
 * @param autoReturn If true, returns like loyalty trident; if false, stays like arrow
 * @param returnSpeed Speed multiplier for return (1.0 = normal, only used if autoReturn=true)
 */
public record ThrowableAbilityData(
        float damage,
        float maxRange,
        boolean breakBlocks,
        boolean autoReturn,
        float returnSpeed
) {
    // Default: 8 damage, 32 block range, no block break, auto-return enabled, 2x return speed
    public static final ThrowableAbilityData DEFAULT =
            new ThrowableAbilityData(8.0f, 32.0f, false, true, 4.0f);

    // Preset for arrow-like behavior (no auto-return)
    public static final ThrowableAbilityData ARROW_STYLE =
            new ThrowableAbilityData(8.0f, 64.0f, false, false, 1.0f);

    public static final Codec<ThrowableAbilityData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.FLOAT.fieldOf("damage").forGetter(ThrowableAbilityData::damage),
                    Codec.FLOAT.fieldOf("maxRange").forGetter(ThrowableAbilityData::maxRange),
                    Codec.BOOL.fieldOf("breakBlocks").forGetter(ThrowableAbilityData::breakBlocks),
                    Codec.BOOL.fieldOf("autoReturn").forGetter(ThrowableAbilityData::autoReturn),
                    Codec.FLOAT.fieldOf("returnSpeed").forGetter(ThrowableAbilityData::returnSpeed)
            ).apply(instance, ThrowableAbilityData::new)
    );

    public static final StreamCodec<ByteBuf, ThrowableAbilityData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ThrowableAbilityData::damage,
            ByteBufCodecs.FLOAT, ThrowableAbilityData::maxRange,
            ByteBufCodecs.BOOL, ThrowableAbilityData::breakBlocks,
            ByteBufCodecs.BOOL, ThrowableAbilityData::autoReturn,
            ByteBufCodecs.FLOAT, ThrowableAbilityData::returnSpeed,
            ThrowableAbilityData::new
    );

    /**
     * Creates a copy with modified damage.
     */
    public ThrowableAbilityData withDamage(float damage) {
        return new ThrowableAbilityData(damage, maxRange, breakBlocks, autoReturn, returnSpeed);
    }

    /**
     * Creates a copy with modified max range.
     */
    public ThrowableAbilityData withMaxRange(float maxRange) {
        return new ThrowableAbilityData(damage, maxRange, breakBlocks, autoReturn, returnSpeed);
    }

    /**
     * Creates a copy with block breaking enabled/disabled.
     */
    public ThrowableAbilityData withBreakBlocks(boolean breakBlocks) {
        return new ThrowableAbilityData(damage, maxRange, breakBlocks, autoReturn, returnSpeed);
    }

    /**
     * Creates a copy with auto-return enabled/disabled.
     */
    public ThrowableAbilityData withAutoReturn(boolean autoReturn) {
        return new ThrowableAbilityData(damage, maxRange, breakBlocks, autoReturn, returnSpeed);
    }

    /**
     * Creates a copy with modified return speed.
     */
    public ThrowableAbilityData withReturnSpeed(float returnSpeed) {
        return new ThrowableAbilityData(damage, maxRange, breakBlocks, autoReturn, returnSpeed);
    }
}
