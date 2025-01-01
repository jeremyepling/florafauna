package net.j40climb.florafauna.component;

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

    public static MiningModeData getNextMode(int currentShape) {
        if (currentShape == MiningShape.values().length - 1) {
            currentShape = 0;
        } else currentShape++;
        return new MiningModeData(MiningShape.getShapeByID(currentShape),  1, 64);
    }
}