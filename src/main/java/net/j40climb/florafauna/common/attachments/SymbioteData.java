package net.j40climb.florafauna.common.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

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
                           boolean featherFalling, boolean speed, int jumpHeight) {
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
                    Codec.INT.fieldOf("jumpHeight").forGetter(SymbioteData::jumpHeight)
            ).apply(builder, SymbioteData::new));

    /**
     * StreamCodec for network synchronization (client-server sync).
     */
    public static final StreamCodec<ByteBuf, SymbioteData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SymbioteData::bonded,
            ByteBufCodecs.VAR_LONG, SymbioteData::bondTime,
            ByteBufCodecs.INT, SymbioteData::tier,
            ByteBufCodecs.BOOL, SymbioteData::dash,
            ByteBufCodecs.BOOL, SymbioteData::featherFalling,
            ByteBufCodecs.BOOL, SymbioteData::speed,
            ByteBufCodecs.INT, SymbioteData::jumpHeight,
            SymbioteData::new
    );

    /**
     * Default symbiote state: not bonded, tier 1, no abilities.
     * Used for both unbonded players and newly created symbiote items.
     */
    public static final SymbioteData DEFAULT = new SymbioteData(false, 0L, 1, false, false, false, 0);

}