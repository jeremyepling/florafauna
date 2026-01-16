package net.j40climb.florafauna.common.item.abilities.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Configuration data for multi-tool ability.
 * When attached to an item, allows it to perform tool modifications on blocks:
 * - Strip wood (like axe)
 * - Create paths (like shovel)
 * - Till farmland (like hoe)
 *
 * @param strip Whether the item can strip logs/wood
 * @param flatten Whether the item can create grass paths
 * @param till Whether the item can till dirt to farmland
 */
public record MultiToolAbilityData(
        boolean strip,
        boolean flatten,
        boolean till
) {
    /** Default: all tool modifications enabled */
    public static final MultiToolAbilityData DEFAULT = new MultiToolAbilityData(true, true, true);

    /** No tool modifications enabled */
    public static final MultiToolAbilityData NONE = new MultiToolAbilityData(false, false, false);

    /** Axe only */
    public static final MultiToolAbilityData AXE_ONLY = new MultiToolAbilityData(true, false, false);

    /** Shovel only */
    public static final MultiToolAbilityData SHOVEL_ONLY = new MultiToolAbilityData(false, true, false);

    /** Hoe only */
    public static final MultiToolAbilityData HOE_ONLY = new MultiToolAbilityData(false, false, true);

    public static final Codec<MultiToolAbilityData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("strip").forGetter(MultiToolAbilityData::strip),
                    Codec.BOOL.fieldOf("flatten").forGetter(MultiToolAbilityData::flatten),
                    Codec.BOOL.fieldOf("till").forGetter(MultiToolAbilityData::till)
            ).apply(instance, MultiToolAbilityData::new)
    );

    public static final StreamCodec<ByteBuf, MultiToolAbilityData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, MultiToolAbilityData::strip,
            ByteBufCodecs.BOOL, MultiToolAbilityData::flatten,
            ByteBufCodecs.BOOL, MultiToolAbilityData::till,
            MultiToolAbilityData::new
    );

    /**
     * @return true if any tool modification is enabled
     */
    public boolean hasAnyEnabled() {
        return strip || flatten || till;
    }

    /**
     * Creates a copy with strip enabled/disabled.
     */
    public MultiToolAbilityData withStrip(boolean strip) {
        return new MultiToolAbilityData(strip, flatten, till);
    }

    /**
     * Creates a copy with flatten enabled/disabled.
     */
    public MultiToolAbilityData withFlatten(boolean flatten) {
        return new MultiToolAbilityData(strip, flatten, till);
    }

    /**
     * Creates a copy with till enabled/disabled.
     */
    public MultiToolAbilityData withTill(boolean till) {
        return new MultiToolAbilityData(strip, flatten, till);
    }
}
