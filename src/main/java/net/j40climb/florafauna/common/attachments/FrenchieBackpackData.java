package net.j40climb.florafauna.common.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Data structure for storing a carried Frenchie on a player.
 * Uses NeoForge's Attachment system for automatic sync and persistence.
 *
 * When hasCarriedFrenchie=true, the frenchieNBT contains the full entity state
 * (variant, health, age, pose, etc.) and the Frenchie entity is despawned.
 */
public record FrenchieBackpackData(
    boolean hasCarriedFrenchie,
    CompoundTag frenchieNBT,
    long pickupTimestamp
) {
    /**
     * Codec for NBT persistence (disk save/load).
     */
    public static final Codec<FrenchieBackpackData> CODEC = RecordCodecBuilder.create(builder ->
        builder.group(
            Codec.BOOL.fieldOf("hasCarriedFrenchie").forGetter(FrenchieBackpackData::hasCarriedFrenchie),
            CompoundTag.CODEC.fieldOf("frenchieNBT").forGetter(FrenchieBackpackData::frenchieNBT),
            Codec.LONG.fieldOf("pickupTimestamp").forGetter(FrenchieBackpackData::pickupTimestamp)
        ).apply(builder, FrenchieBackpackData::new)
    );

    /**
     * StreamCodec for network synchronization (client-server sync).
     */
    public static final StreamCodec<ByteBuf, FrenchieBackpackData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, FrenchieBackpackData::hasCarriedFrenchie,
        ByteBufCodecs.COMPOUND_TAG, FrenchieBackpackData::frenchieNBT,
        ByteBufCodecs.VAR_LONG, FrenchieBackpackData::pickupTimestamp,
        FrenchieBackpackData::new
    );

    /**
     * Default state: not carrying any Frenchie.
     */
    public static final FrenchieBackpackData DEFAULT = new FrenchieBackpackData(false, new CompoundTag(), 0L);
}
