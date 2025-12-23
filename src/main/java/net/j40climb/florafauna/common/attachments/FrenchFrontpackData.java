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
public record FrenchFrontpackData(
    boolean hasCarriedFrenchie,
    CompoundTag frenchieNBT,
    long pickupTimestamp
) {
    /**
     * Codec for NBT persistence (disk save/load).
     */
    public static final Codec<FrenchFrontpackData> CODEC = RecordCodecBuilder.create(builder ->
        builder.group(
            Codec.BOOL.fieldOf("hasCarriedFrenchie").forGetter(FrenchFrontpackData::hasCarriedFrenchie),
            CompoundTag.CODEC.fieldOf("frenchieNBT").forGetter(FrenchFrontpackData::frenchieNBT),
            Codec.LONG.fieldOf("pickupTimestamp").forGetter(FrenchFrontpackData::pickupTimestamp)
        ).apply(builder, FrenchFrontpackData::new)
    );

    /**
     * StreamCodec for network synchronization (client-server sync).
     */
    public static final StreamCodec<ByteBuf, FrenchFrontpackData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, FrenchFrontpackData::hasCarriedFrenchie,
        ByteBufCodecs.COMPOUND_TAG, FrenchFrontpackData::frenchieNBT,
        ByteBufCodecs.VAR_LONG, FrenchFrontpackData::pickupTimestamp,
        FrenchFrontpackData::new
    );

    /**
     * Default state: not carrying any Frenchie.
     */
    public static final FrenchFrontpackData DEFAULT = new FrenchFrontpackData(false, new CompoundTag(), 0L);
}
