package net.j40climb.florafauna.common.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Data component storing symbiote ability state on items.
 * When the symbiote is in item form (unbonded), this stores:
 * - Evolution tier
 * - Ability toggles (dash, featherFalling, speed)
 */
public record SymbioteAbilityState(int tier, boolean dash, boolean featherFalling, boolean speed) {

    /**
     * Codec for NBT persistence
     */
    public static final Codec<SymbioteAbilityState> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    Codec.INT.fieldOf("tier").forGetter(SymbioteAbilityState::tier),
                    Codec.BOOL.fieldOf("dash").forGetter(SymbioteAbilityState::dash),
                    Codec.BOOL.fieldOf("featherFalling").forGetter(SymbioteAbilityState::featherFalling),
                    Codec.BOOL.fieldOf("speed").forGetter(SymbioteAbilityState::speed)
            ).apply(builder, SymbioteAbilityState::new));

    /**
     * StreamCodec for network synchronization
     */
    public static final StreamCodec<ByteBuf, SymbioteAbilityState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SymbioteAbilityState::tier,
            ByteBufCodecs.BOOL, SymbioteAbilityState::dash,
            ByteBufCodecs.BOOL, SymbioteAbilityState::featherFalling,
            ByteBufCodecs.BOOL, SymbioteAbilityState::speed,
            SymbioteAbilityState::new
    );

    /**
     * Default state: tier 1, no abilities
     */
    public static final SymbioteAbilityState DEFAULT = new SymbioteAbilityState(1, false, false, false);
}
