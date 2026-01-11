package net.j40climb.florafauna.common.block.mininganchor;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.common.block.mininganchor.pod.Tier1PodBlockEntity;
import net.j40climb.florafauna.common.block.vacuum.ItemBuffer;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tier 1 (Feral) Mining Anchor block entity.
 * Spawns Feral Pods which spill items when broken.
 * Max pods: 4 (configurable)
 */
public class Tier1MiningAnchorBlockEntity extends AbstractMiningAnchorBlockEntity {

    private static final int DEFAULT_MAX_PODS = 4;

    public Tier1MiningAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(FloraFaunaRegistry.TIER1_MINING_ANCHOR_BE.get(), pos, state);
    }

    @Override
    protected Block getPodBlock() {
        return FloraFaunaRegistry.TIER1_POD.get();
    }

    @Override
    protected int getMaxPods() {
        return Config.miningAnchorTier1MaxPods > 0 ? Config.miningAnchorTier1MaxPods : DEFAULT_MAX_PODS;
    }

    @Override
    protected int getPodCapacity() {
        return Tier1PodBlockEntity.SLOT_COUNT * 64;
    }

    @Override
    protected void resolvePodContents(BlockPos podPos) {
        if (level == null) return;

        BlockEntity be = level.getBlockEntity(podPos);
        if (!(be instanceof Tier1PodBlockEntity pod)) return;

        // Spill all items in tight radius around pod position
        ItemBuffer podBuffer = pod.getBuffer();
        while (!podBuffer.isEmpty()) {
            ItemStack stack = podBuffer.poll();
            if (!stack.isEmpty()) {
                Containers.dropItemStack(
                    level,
                    podPos.getX() + 0.5,
                    podPos.getY() + 0.5,
                    podPos.getZ() + 0.5,
                    stack
                );
            }
        }
    }
}
