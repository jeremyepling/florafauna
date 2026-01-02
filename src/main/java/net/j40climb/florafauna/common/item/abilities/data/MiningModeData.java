package net.j40climb.florafauna.common.item.abilities.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record MiningModeData(MiningShape shape, Integer radius, Integer maxBlocksToBreak) {
    public static final Codec<MiningModeData> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    MiningShape.CODEC.fieldOf("shape").forGetter(MiningModeData::shape),
                    Codec.INT.fieldOf("radius").forGetter(MiningModeData::radius),
                    Codec.INT.fieldOf("maxBlocksToBreak").forGetter(MiningModeData::maxBlocksToBreak)
            ).apply(builder, MiningModeData::new));

    public static final StreamCodec<ByteBuf, MiningModeData> STREAM_CODEC = StreamCodec.composite(
            MiningShape.STREAM_CODEC, MiningModeData::shape,
            ByteBufCodecs.INT, MiningModeData::radius,
            ByteBufCodecs.INT, MiningModeData::maxBlocksToBreak,
            MiningModeData::new
    );

    public static final MiningModeData DEFAULT = new MiningModeData(MiningShape.SINGLE, 0, 64);

    public MiningModeData getNextMode() {
        int currentShapeIndex = this.shape().id();
        if (currentShapeIndex == MiningShape.values().length - 1) {
            currentShapeIndex = 0;
        } else currentShapeIndex++;
        MiningShape miningShape = MiningShape.getShapeByID(currentShapeIndex);
        return new MiningModeData(miningShape, miningShape.getRadius(), this.maxBlocksToBreak());
    }

    public String getMiningModeString() {
        return "Mining Mode: " + this.shape().name();
    }
}
