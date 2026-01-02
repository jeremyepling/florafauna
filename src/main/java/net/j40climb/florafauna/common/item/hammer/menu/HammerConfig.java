package net.j40climb.florafauna.common.item.hammer.menu;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.common.item.hammer.data.MiningSpeed;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Configuration data for the Energy Hammer tool.
 * Stores settings for fortune, silk touch, and mining speed.
 * Note: fortune and silkTouch are mutually exclusive - exactly one must be true at all times.
 */
public record HammerConfig(boolean fortune, boolean silkTouch, MiningSpeed miningSpeed) {

    public static final HammerConfig DEFAULT = new HammerConfig(true, false, MiningSpeed.EFFICIENCY);

    /**
     * Ensures exactly one of fortune or silkTouch is enabled.
     */
    public HammerConfig {
        if (fortune == silkTouch) {
            throw new IllegalArgumentException("Exactly one of Fortune or Silk Touch must be enabled");
        }
    }

    public static final Codec<HammerConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("fortune").forGetter(HammerConfig::fortune),
                    Codec.BOOL.fieldOf("silkTouch").forGetter(HammerConfig::silkTouch),
                    MiningSpeed.CODEC.fieldOf("miningSpeed").forGetter(HammerConfig::miningSpeed)
            ).apply(instance, HammerConfig::new)
    );

    public static final StreamCodec<ByteBuf, HammerConfig> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            HammerConfig::fortune,
            ByteBufCodecs.BOOL,
            HammerConfig::silkTouch,
            MiningSpeed.STREAM_CODEC,
            HammerConfig::miningSpeed,
            HammerConfig::new
    );

    /**
     * Toggles between Fortune 3 <-> Silk Touch
     */
    public HammerConfig withToggledEnchantment() {
        if (fortune) {
            // Fortune -> Silk Touch
            return new HammerConfig(false, true, miningSpeed);
        } else {
            // Silk Touch -> Fortune
            return new HammerConfig(true, false, miningSpeed);
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
    public HammerConfig withMiningSpeed(MiningSpeed speed) {
        return new HammerConfig(fortune, silkTouch, speed);
    }

    /**
     * Cycles to the next mining speed value.
     */
    public HammerConfig withNextMiningSpeed() {
        MiningSpeed next = switch (miningSpeed) {
            case STANDARD -> MiningSpeed.EFFICIENCY;
            case EFFICIENCY -> MiningSpeed.INSTABREAK;
            case INSTABREAK -> MiningSpeed.STANDARD;
        };
        return new HammerConfig(fortune, silkTouch, next);
    }

    /**
     * Cycles to the previous mining speed value.
     */
    public HammerConfig withPreviousMiningSpeed() {
        MiningSpeed prev = switch (miningSpeed) {
            case STANDARD -> MiningSpeed.INSTABREAK;
            case EFFICIENCY -> MiningSpeed.STANDARD;
            case INSTABREAK -> MiningSpeed.EFFICIENCY;
        };
        return new HammerConfig(fortune, silkTouch, prev);
    }
}
