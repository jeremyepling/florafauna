package net.j40climb.florafauna.common.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public record FoundBlockData(BlockState block, BlockPos pos) {
    public static final Codec<FoundBlockData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BlockState.CODEC.fieldOf("block").forGetter(FoundBlockData::block),
                    BlockPos.CODEC.fieldOf("position").forGetter(FoundBlockData::pos)
            ).apply(instance, FoundBlockData::new));

    public String getOutputString() {
        return block.getBlock().getName().getString() + " at " + "(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
    }
}
