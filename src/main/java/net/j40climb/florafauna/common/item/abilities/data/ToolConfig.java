package net.j40climb.florafauna.common.item.abilities.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Configuration data for tools with configurable enchantments and mining speed.
 * Stores settings for fortune, silk touch, and mining speed.
 * Note: fortune and silkTouch are mutually exclusive - exactly one must be true at all times.
 */
public record ToolConfig(boolean fortune, boolean silkTouch, MiningSpeed miningSpeed) {

    public static final ToolConfig DEFAULT = new ToolConfig(true, false, MiningSpeed.EFFICIENCY);

    /**
     * Ensures exactly one of fortune or silkTouch is enabled.
     */
    public ToolConfig {
        if (fortune == silkTouch) {
            throw new IllegalArgumentException("Exactly one of Fortune or Silk Touch must be enabled");
        }
    }

    public static final Codec<ToolConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("fortune").forGetter(ToolConfig::fortune),
                    Codec.BOOL.fieldOf("silkTouch").forGetter(ToolConfig::silkTouch),
                    MiningSpeed.CODEC.fieldOf("miningSpeed").forGetter(ToolConfig::miningSpeed)
            ).apply(instance, ToolConfig::new)
    );

    public static final StreamCodec<ByteBuf, ToolConfig> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ToolConfig::fortune,
            ByteBufCodecs.BOOL,
            ToolConfig::silkTouch,
            MiningSpeed.STREAM_CODEC,
            ToolConfig::miningSpeed,
            ToolConfig::new
    );

    /**
     * Toggles between Fortune 3 and Silk Touch
     */
    public ToolConfig withToggledEnchantment() {
        if (fortune) {
            return new ToolConfig(false, true, miningSpeed);
        } else {
            return new ToolConfig(true, false, miningSpeed);
        }
    }

    /**
     * Gets the current enchantment type as a string.
     */
    public String getEnchantmentType() {
        if (fortune) return "Fortune III";
        if (silkTouch) return "Silk Touch";
        return "None";
    }

    /**
     * Creates a new config with the specified mining speed.
     */
    public ToolConfig withMiningSpeed(MiningSpeed speed) {
        return new ToolConfig(fortune, silkTouch, speed);
    }

    /**
     * Cycles to the next mining speed value.
     */
    public ToolConfig withNextMiningSpeed() {
        MiningSpeed next = switch (miningSpeed) {
            case STANDARD -> MiningSpeed.EFFICIENCY;
            case EFFICIENCY -> MiningSpeed.INSTABREAK;
            case INSTABREAK -> MiningSpeed.STANDARD;
        };
        return new ToolConfig(fortune, silkTouch, next);
    }

    /**
     * Cycles to the previous mining speed value.
     */
    public ToolConfig withPreviousMiningSpeed() {
        MiningSpeed prev = switch (miningSpeed) {
            case STANDARD -> MiningSpeed.INSTABREAK;
            case EFFICIENCY -> MiningSpeed.STANDARD;
            case INSTABREAK -> MiningSpeed.EFFICIENCY;
        };
        return new ToolConfig(fortune, silkTouch, prev);
    }
}
