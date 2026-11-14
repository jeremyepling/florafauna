package net.j40climb.florafauna.common.entity.frenchie.frontpack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.common.entity.frenchie.FrenchieVariant;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Data structure for storing a carried Frenchie on a player.
 * Uses NeoForge's Attachment system for automatic sync and persistence.
 *
 * When hasCarriedFrenchie=true, the frenchieNBT contains the full entity state
 * (variant, health, age, pose, etc.) and the Frenchie entity is despawned.
 */
public record FrontpackData(
    boolean hasCarriedFrenchie,
    CompoundTag frenchieNBT,
    long pickupTimestamp
) {
    /**
     * Codec for NBT persistence (disk save/load).
     */
    public static final Codec<FrontpackData> CODEC = RecordCodecBuilder.create(builder ->
        builder.group(
            Codec.BOOL.fieldOf("hasCarriedFrenchie").forGetter(FrontpackData::hasCarriedFrenchie),
            CompoundTag.CODEC.fieldOf("frenchieNBT").forGetter(FrontpackData::frenchieNBT),
            Codec.LONG.fieldOf("pickupTimestamp").forGetter(FrontpackData::pickupTimestamp)
        ).apply(builder, FrontpackData::new)
    );

    /**
     * StreamCodec for network synchronization (client-server sync).
     */
    public static final StreamCodec<ByteBuf, FrontpackData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, FrontpackData::hasCarriedFrenchie,
        ByteBufCodecs.COMPOUND_TAG, FrontpackData::appearanceTag,
        ByteBufCodecs.VAR_LONG, FrontpackData::pickupTimestamp,
        FrontpackData::new
    );

    public @Nullable FrenchieVariant carriedCoat() {
        return hasCarriedFrenchie ? FrenchieVariant.byId(frenchieNBT.getIntOr("Variant", 0)) : null;
    }

    // Keep the original packet layout. Tracking clients need the coat, not the saved entity.
    private CompoundTag appearanceTag() {
        CompoundTag appearance = new CompoundTag();
        if (hasCarriedFrenchie) {
            appearance.putInt("Variant", frenchieNBT.getIntOr("Variant", 0));
        }
        return appearance;
    }

    /**
     * Default state: not carrying any Frenchie.
     */
    public static final FrontpackData DEFAULT = new FrontpackData(false, new CompoundTag(), 0L);
}
