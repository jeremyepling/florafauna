package net.j40climb.florafauna.common.block.mininganchor;

import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * T2 (Hardened) Mining Anchor block entity.
 * Spawns Hardened Pods which keep items when broken (like shulker boxes).
 */
public class HardenedMiningAnchorBlockEntity extends AbstractMiningAnchorBlockEntity {

    public HardenedMiningAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(FloraFaunaRegistry.HARDENED_MINING_ANCHOR_BE.get(), pos, state);
    }

    @Override
    protected Block getPodBlock() {
        return FloraFaunaRegistry.HARDENED_POD.get();
    }
}
