package net.j40climb.florafauna.common.block.mininganchor;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.common.block.mininganchor.pod.Tier2PodBlockEntity;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tier 2 (Hardened) Mining Anchor block entity.
 * Spawns Hardened Pods which keep items when broken (like shulker boxes).
 * Max pods: 8 (configurable)
 */
public class Tier2MiningAnchorBlockEntity extends AbstractMiningAnchorBlockEntity {

    private static final int DEFAULT_MAX_PODS = 8;

    public Tier2MiningAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(FloraFaunaRegistry.TIER2_MINING_ANCHOR_BE.get(), pos, state);
    }

    @Override
    protected Block getPodBlock() {
        return FloraFaunaRegistry.TIER2_POD.get();
    }

    @Override
    protected int getMaxPods() {
        return Config.miningAnchorTier2MaxPods > 0 ? Config.miningAnchorTier2MaxPods : DEFAULT_MAX_PODS;
    }

    @Override
    protected int getPodCapacity() {
        return Tier2PodBlockEntity.SLOT_COUNT * 64;
    }
}
