package net.j40climb.florafauna.common.symbiote.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

/**
 * Data structure representing symbiote state.
 * Used as both an Attachment (on players) and a DataComponent (on items).
 *
 * When on player: bonded=true, bondTime=actual time
 * When on item: bonded=false, bondTime=0
 *
 * Stores bonding status, evolution tier, and ability toggles.
 */
public record SymbioteData(boolean bonded, long bondTime, int tier, boolean dash,
                           boolean featherFalling, int speedLevel, int jumpBoost) {
    public static final int MAX_SPEED_LEVEL = 2;
    static final Codec<Integer> SPEED_LEVEL_CODEC = Codec.intRange(0, MAX_SPEED_LEVEL);
    // Retain the previous boolean wire values: 0 was off, 1 granted the full bonus.
    static final StreamCodec<ByteBuf, Integer> SPEED_LEVEL_STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
            encoded -> encoded == 1 ? MAX_SPEED_LEVEL : encoded == 2 ? 1 : 0,
            level -> level == MAX_SPEED_LEVEL ? 1 : level == 1 ? 2 : 0
    );
    /**
     * Codec for NBT persistence (disk save/load).
     */
    public static final Codec<SymbioteData> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    Codec.BOOL.fieldOf("bonded").forGetter(SymbioteData::bonded),
                    Codec.LONG.fieldOf("bondTime").forGetter(SymbioteData::bondTime),
                    Codec.INT.fieldOf("tier").forGetter(SymbioteData::tier),
                    Codec.BOOL.fieldOf("dash").forGetter(SymbioteData::dash),
                    Codec.BOOL.fieldOf("featherFalling").forGetter(SymbioteData::featherFalling),
                    Codec.BOOL.fieldOf("speed").forGetter(SymbioteData::speed),
                    SPEED_LEVEL_CODEC.optionalFieldOf("speedLevel").forGetter(data -> explicitSpeedLevel(data.speedLevel)),
                    Codec.INT.fieldOf("jumpBoost").forGetter(SymbioteData::jumpBoost)
            ).apply(builder, (bonded, bondTime, tier, dash, featherFalling, speed, speedLevel, jumpBoost) ->
                    new SymbioteData(bonded, bondTime, tier, dash, featherFalling, decodeSpeedLevel(speed, speedLevel), jumpBoost)));

    /**
     * StreamCodec for network synchronization (client-server sync).
     */
    public static final StreamCodec<ByteBuf, SymbioteData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SymbioteData::bonded,
            ByteBufCodecs.VAR_LONG, SymbioteData::bondTime,
            ByteBufCodecs.INT, SymbioteData::tier,
            ByteBufCodecs.BOOL, SymbioteData::dash,
            ByteBufCodecs.BOOL, SymbioteData::featherFalling,
            SPEED_LEVEL_STREAM_CODEC, SymbioteData::speedLevel,
            ByteBufCodecs.INT, SymbioteData::jumpBoost,
            SymbioteData::new
    );

    public boolean speed() {
        return speedLevel > 0;
    }

    static Optional<Integer> explicitSpeedLevel(int level) {
        return level == 1 ? Optional.of(level) : Optional.empty();
    }

    static int decodeSpeedLevel(boolean speed, Optional<Integer> explicitLevel) {
        return explicitLevel.orElse(speed ? MAX_SPEED_LEVEL : 0);
    }

    /**
     * Default symbiote state: not bonded, tier 1, no abilities.
     * Used for both unbonded players and newly created symbiote items.
     */
    public static final SymbioteData DEFAULT = new SymbioteData(false, 0L, 1, false, false, 0, 0);

}
