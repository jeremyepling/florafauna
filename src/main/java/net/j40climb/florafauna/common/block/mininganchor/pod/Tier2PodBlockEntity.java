package net.j40climb.florafauna.common.block.mininganchor.pod;

import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * T2 (Hardened) Storage Pod block entity.
 * Keeps items when broken (like shulker boxes) - drops as item with contents.
 */
public class Tier2PodBlockEntity extends AbstractStoragePodBlockEntity {

    public Tier2PodBlockEntity(BlockPos pos, BlockState state) {
        super(FloraFaunaRegistry.TIER2_POD_BE.get(), pos, state);
    }

    @Override
    protected void onBlockBroken(Level level, BlockPos pos, @Nullable Player player) {
        // Drop as item with contents preserved (like shulker box)
        ItemStack podItem = new ItemStack(FloraFaunaRegistry.TIER2_POD.get());

        // Store contents in item
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

        // Drop the item
        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, podItem);
    }

    /**
     * Loads contents from an item stack (called when placed from item with contents).
     */
    public void loadFromItem(ItemStack itemStack) {
        PodContents contents = itemStack.get(FloraFaunaRegistry.POD_CONTENTS.get());
        if (contents != null && !contents.isEmpty()) {
            for (ItemStack stack : contents.items()) {
                podBuffer.add(stack.copy());
            }
            setChanged();
        }
    }
}
