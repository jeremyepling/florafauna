package net.j40climb.florafauna.common.item.abilities.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Per-tool enchantment, mining speed, vein matching and tunnel lighting settings.
 * Note: fortune and silkTouch are mutually exclusive - exactly one must be true at all times.
 */
public record ToolConfig(boolean fortune, boolean silkTouch, MiningSpeed miningSpeed, boolean exactVein, boolean tunnelLighting) {

    public static final ToolConfig DEFAULT = new ToolConfig(true, false, MiningSpeed.EFFICIENCY);

    public ToolConfig(boolean fortune, boolean silkTouch, MiningSpeed miningSpeed) {
        this(fortune, silkTouch, miningSpeed, false, false);
    }

    public ToolConfig(boolean fortune, boolean silkTouch, MiningSpeed miningSpeed, boolean exactVein) {
        this(fortune, silkTouch, miningSpeed, exactVein, false);
    }

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
                    MiningSpeed.CODEC.fieldOf("miningSpeed").forGetter(ToolConfig::miningSpeed),
                    Codec.BOOL.optionalFieldOf("exactVein", false).forGetter(ToolConfig::exactVein),
                    Codec.BOOL.optionalFieldOf("tunnelLighting", false).forGetter(ToolConfig::tunnelLighting)
            ).apply(instance, ToolConfig::new)
    );

    public static final StreamCodec<ByteBuf, ToolConfig> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ToolConfig::fortune,
            ByteBufCodecs.BOOL,
            ToolConfig::silkTouch,
            MiningSpeed.STREAM_CODEC,
            ToolConfig::miningSpeed,
            ByteBufCodecs.BOOL,
            ToolConfig::exactVein,
            ByteBufCodecs.BOOL,
            ToolConfig::tunnelLighting,
            ToolConfig::new
    );

    /**
     * Toggles between Fortune 3 and Silk Touch
     */
    public ToolConfig withToggledEnchantment() {
        return new ToolConfig(!fortune, !silkTouch, miningSpeed, exactVein, tunnelLighting);
    }

    public ToolConfig withToggledVeinMatching() {
        return new ToolConfig(fortune, silkTouch, miningSpeed, !exactVein, tunnelLighting);
    }

    public ToolConfig withToggledTunnelLighting() {
        return new ToolConfig(fortune, silkTouch, miningSpeed, exactVein, !tunnelLighting);
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
        return new ToolConfig(fortune, silkTouch, speed, exactVein, tunnelLighting);
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
        return withMiningSpeed(next);
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
        return withMiningSpeed(prev);
    }

    public Component speedDescription() {
        return Component.translatable("tooltip.florafauna.hammer.speed",
                Component.translatable("gui.florafauna.tool_config.speed." + miningSpeed.getSerializedName()));
    }
}
