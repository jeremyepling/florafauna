package net.j40climb.florafauna.common.block.ferricpoppy;

import com.mojang.serialization.MapCodec;
import net.j40climb.florafauna.setup.FloraFaunaTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ferric Poppy block - an iron-infused flower created by calm Iron Golems.
 * <p>
 * Converted from vanilla poppies when a calm Iron Golem is nearby.
 * Yields iron nuggets when crafted.
 */
public class FerricPoppyBlock extends Block {
    public static final MapCodec<FerricPoppyBlock> CODEC = simpleCodec(FerricPoppyBlock::new);

    // Shape for a cross-shaped flower (like vanilla flowers)
    private static final VoxelShape SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 10.0, 11.0);

    public FerricPoppyBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * Check if the block can survive at this position.
     * Requires plantable ground below.
     */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(FloraFaunaTags.Blocks.FERRIC_POPPY_PLANTABLE);
    }
}
