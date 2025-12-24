package net.j40climb.florafauna.common.item.energyhammer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Configuration data for the Energy Hammer tool.
 * Stores settings for fortune, silk touch, and mining speed.
 * Note: fortune and silkTouch are mutually exclusive - exactly one must be true at all times.
 */
public record EnergyHammerConfig(boolean fortune, boolean silkTouch, MiningSpeed miningSpeed) {

    public static final EnergyHammerConfig DEFAULT = new EnergyHammerConfig(true, false, MiningSpeed.EFFICIENCY);

    /**
     * Ensures exactly one of fortune or silkTouch is enabled.
     */
    public EnergyHammerConfig {
        if (fortune == silkTouch) {
            throw new IllegalArgumentException("Exactly one of Fortune or Silk Touch must be enabled");
        }
    }

    public static final Codec<EnergyHammerConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("fortune").forGetter(EnergyHammerConfig::fortune),
                    Codec.BOOL.fieldOf("silkTouch").forGetter(EnergyHammerConfig::silkTouch),
                    MiningSpeed.CODEC.fieldOf("miningSpeed").forGetter(EnergyHammerConfig::miningSpeed)
            ).apply(instance, EnergyHammerConfig::new)
    );

    public static final StreamCodec<ByteBuf, EnergyHammerConfig> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            EnergyHammerConfig::fortune,
            ByteBufCodecs.BOOL,
            EnergyHammerConfig::silkTouch,
            MiningSpeed.STREAM_CODEC,
            EnergyHammerConfig::miningSpeed,
            EnergyHammerConfig::new
    );

    /**
     * Toggles between Fortune 3 <-> Silk Touch
     */
    public EnergyHammerConfig withToggledEnchantment() {
        if (fortune) {
            // Fortune -> Silk Touch
            return new EnergyHammerConfig(false, true, miningSpeed);
        } else {
            // Silk Touch -> Fortune
            return new EnergyHammerConfig(true, false, miningSpeed);
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
    public EnergyHammerConfig withMiningSpeed(MiningSpeed speed) {
        return new EnergyHammerConfig(fortune, silkTouch, speed);
    }

    /**
     * Cycles to the next mining speed value.
     */
    public EnergyHammerConfig withNextMiningSpeed() {
        MiningSpeed next = switch (miningSpeed) {
            case STANDARD -> MiningSpeed.EFFICIENCY;
            case EFFICIENCY -> MiningSpeed.INSTABREAK;
            case INSTABREAK -> MiningSpeed.STANDARD;
        };
        return new EnergyHammerConfig(fortune, silkTouch, next);
    }

    /**
     * Cycles to the previous mining speed value.
     */
    public EnergyHammerConfig withPreviousMiningSpeed() {
        MiningSpeed prev = switch (miningSpeed) {
            case STANDARD -> MiningSpeed.INSTABREAK;
            case EFFICIENCY -> MiningSpeed.STANDARD;
            case INSTABREAK -> MiningSpeed.EFFICIENCY;
        };
        return new EnergyHammerConfig(fortune, silkTouch, prev);
    }
}
