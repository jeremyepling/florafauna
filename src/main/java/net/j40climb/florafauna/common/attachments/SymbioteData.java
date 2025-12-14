package net.j40climb.florafauna.common.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Data structure representing the state of a symbiote bonded to a player.
 * Stores bonding status, evolution tier, and ability toggles.
 */
public record SymbioteData(boolean bonded, long bondTime, int tier, boolean dash,
                           boolean featherFalling, boolean speed) {
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
                    Codec.BOOL.fieldOf("speed").forGetter(SymbioteData::speed)
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
            SymbioteData::new
    );

    /**
     * Default symbiote state: not bonded, no progress.
     */
    public static final SymbioteData DEFAULT = new SymbioteData(false, 0L, 0, false, false, false);

}