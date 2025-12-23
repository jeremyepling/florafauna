package net.j40climb.florafauna.common.item.energyhammer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record MiningModeData(MiningShape shape, Integer radius, Integer maxBlocksToBreak) {
    public static final Codec<MiningModeData> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    MiningShape.CODEC.fieldOf("shape").forGetter(MiningModeData::shape),
                    Codec.INT.fieldOf("radius").forGetter(MiningModeData::radius), // radius of mineable area
                    Codec.INT.fieldOf("maxBlocksToBreak").forGetter(MiningModeData::maxBlocksToBreak) // not used yet
            ).apply(builder, MiningModeData::new));

    public static final StreamCodec<ByteBuf, MiningModeData> STREAM_CODEC = StreamCodec.composite(
            MiningShape.STREAM_CODEC, MiningModeData::shape,
            ByteBufCodecs.INT, MiningModeData::radius,
            ByteBufCodecs.INT, MiningModeData::maxBlocksToBreak,
            MiningModeData::new
    );

    // Default mining mode instead of a contstructor like the Neoforge example https://docs.neoforged.net/docs/1.21.1/items/datacomponents#adding-default-data-components-to-items
    public static final MiningModeData DEFAULT = new MiningModeData(MiningShape.SINGLE, 0, 64);

    public MiningModeData getNextMode() {
        int currentShapeIndex = this.shape().id();
        if (currentShapeIndex == MiningShape.values().length - 1) {
            currentShapeIndex = 0;
        } else currentShapeIndex++;
        MiningShape miningShape = MiningShape.getShapeByID(currentShapeIndex);
        return new MiningModeData(miningShape,  miningShape.getRadius(), 64);
    }

    public String getMiningModeString() {
        return "Mining Mode: " + this.shape().name();
    }
}