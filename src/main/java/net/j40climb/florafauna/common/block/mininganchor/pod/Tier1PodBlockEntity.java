package net.j40climb.florafauna.common.block.mininganchor.pod;

import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * T0 (Feral) Storage Pod block entity.
 * Spills all items when broken - basic organic storage.
 */
public class Tier1PodBlockEntity extends AbstractStoragePodBlockEntity {

    public Tier1PodBlockEntity(BlockPos pos, BlockState state) {
        super(FloraFaunaRegistry.TIER1_POD_BE.get(), pos, state);
    }

    @Override
    protected void onBlockBroken(Level level, BlockPos pos, @Nullable Player player) {
        // Spill all items into the world
        while (!podBuffer.isEmpty()) {
            ItemStack stack = podBuffer.poll();
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            }
        }
    }
}
