package net.j40climb.florafauna.common.block.mininganchor;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.common.block.mininganchor.pod.PodContents;
import net.j40climb.florafauna.common.block.mininganchor.pod.Tier2PodBlockEntity;
import net.j40climb.florafauna.common.block.vacuum.ItemBuffer;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    protected void resolvePodContents(BlockPos podPos) {
        if (level == null) return;

        BlockEntity be = level.getBlockEntity(podPos);
        if (!(be instanceof Tier2PodBlockEntity pod)) return;

        // Create pod item with contents preserved (like shulker box)
        ItemStack podItem = new ItemStack(FloraFaunaRegistry.TIER2_POD.get());

        ItemBuffer podBuffer = pod.getBuffer();
        if (!podBuffer.isEmpty()) {
            List<ItemStack> contents = new ArrayList<>();
            while (!podBuffer.isEmpty()) {
                ItemStack stack = podBuffer.poll();
                if (!stack.isEmpty()) {
                    contents.add(stack);
                }
            }
            podItem.set(FloraFaunaRegistry.POD_CONTENTS.get(), PodContents.of(contents));
        }

        // Drop the pod item
        Containers.dropItemStack(
            level,
            podPos.getX() + 0.5,
            podPos.getY() + 0.5,
            podPos.getZ() + 0.5,
            podItem
        );
    }
}
