package net.j40climb.florafauna.common.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Data structure representing the state of a symbiote bonded to a player.
 * Stores bonding status, evolution progress, energy, and health/integrity.
 */
public record SymbioteData(boolean bonded, long bondTime, int tier, int energy, int health) {
    /**
     * Codec for NBT persistence (disk save/load).
     */
    public static final Codec<SymbioteData> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    Codec.BOOL.fieldOf("bonded").forGetter(SymbioteData::bonded),
                    Codec.LONG.fieldOf("bondTime").forGetter(SymbioteData::bondTime),
                    Codec.INT.fieldOf("tier").forGetter(SymbioteData::tier),
                    Codec.INT.fieldOf("energy").forGetter(SymbioteData::energy),
                    Codec.INT.fieldOf("health").forGetter(SymbioteData::health)
            ).apply(builder, SymbioteData::new));

    /**
     * StreamCodec for network synchronization (client-server sync).
     */
    public static final StreamCodec<ByteBuf, SymbioteData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SymbioteData::bonded,
            ByteBufCodecs.VAR_LONG, SymbioteData::bondTime,
            ByteBufCodecs.INT, SymbioteData::tier,
            ByteBufCodecs.INT, SymbioteData::energy,
            ByteBufCodecs.INT, SymbioteData::health,
            SymbioteData::new
    );

    /**
     * Default symbiote state: not bonded, no progress.
     */
    public static final SymbioteData DEFAULT = new SymbioteData(false, 0L, 0, 0, 100);

    /**
     * Creates a new SymbioteData with updated health.
     *
     * @param newHealth the new health value (clamped to 0-100)
     * @return a new SymbioteData instance with the updated health
     */
    public SymbioteData withHealth(int newHealth) {
        return new SymbioteData(bonded, bondTime, tier, energy, Math.clamp(newHealth, 0, 100));
    }

    /**
     * Adds energy to the symbiote.
     *
     * @param amount the amount of energy to add
     * @return a new SymbioteData instance with the updated energy
     */
    public SymbioteData addEnergy(int amount) {
        return new SymbioteData(bonded, bondTime, tier, energy + amount, health);
    }

    /**
     * Damages the symbiote, reducing its health.
     *
     * @param amount the amount of damage to apply
     * @return a new SymbioteData instance with reduced health
     */
    public SymbioteData damage(int amount) {
        return withHealth(health - amount);
    }

    /**
     * Heals the symbiote, restoring health.
     *
     * @param amount the amount to heal
     * @return a new SymbioteData instance with increased health
     */
    public SymbioteData heal(int amount) {
        return withHealth(health + amount);
    }

    /**
     * Gets the ability multiplier based on current health.
     * Damaged symbiotes provide reduced abilities.
     *
     * @return a multiplier between 0.0 and 1.0
     */
    public float getAbilityMultiplier() {
        return health / 100.0f;
    }
}