package net.j40climb.florafauna.common.block.mininganchor;

import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * T0 (Feral) Mining Anchor block entity.
 * Spawns Feral Pods which spill items when broken.
 */
public class FeralMiningAnchorBlockEntity extends AbstractMiningAnchorBlockEntity {

    public FeralMiningAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(FloraFaunaRegistry.FERAL_MINING_ANCHOR_BE.get(), pos, state);
    }

    @Override
    protected Block getPodBlock() {
        return FloraFaunaRegistry.FERAL_POD.get();
    }
}
