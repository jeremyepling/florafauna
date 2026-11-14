package net.j40climb.florafauna.common.block.mininganchor.pod;

import net.j40climb.florafauna.common.block.vacuum.ItemBuffer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Item handler that delegates directly to an ItemBuffer.
 * Unlike ItemStacksResourceHandler, this does NOT copy the stacks,
 * so changes are immediately reflected in the underlying buffer.
 *
 * Allows hoppers and other automation to extract items from pods.
 */
public class PodItemHandler implements ResourceHandler<ItemResource> {
    private final AbstractStoragePodBlockEntity pod;
    private final List<SnapshotJournal<ItemStack>> slots = new ArrayList<>();

    public PodItemHandler(AbstractStoragePodBlockEntity pod) {
        this.pod = pod;
        for (int i = 0; i < size(); i++) {
            slots.add(new SlotJournal(i));
        }
    }

    private final class SlotJournal extends SnapshotJournal<ItemStack> {
        private final int index;

        SlotJournal(int index) {
            this.index = index;
        }

        @Override
        protected ItemStack createSnapshot() {
            return buffer().getStack(index).copy();
        }

        @Override
        protected void revertToSnapshot(ItemStack snapshot) {
            buffer().setStack(index, snapshot);
        }

        @Override
        protected void onRootCommit(ItemStack originalState) {
            pod.markChangedAndSync();
        }
    }

    private ItemBuffer buffer() {
        return pod.getBuffer();
    }

    @Override
    public int size() {
        return buffer().getMaxStacks();
    }

    @Override
    public ItemResource getResource(int index) {
        ItemStack stack = buffer().getStack(index);
        return ItemResource.of(stack);
    }

    @Override
    public long getAmountAsLong(int index) {
        return buffer().getStack(index).getCount();
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        if (resource.isEmpty()) {
            return Item.ABSOLUTE_MAX_STACK_SIZE;
        }
        return Math.min(resource.getMaxStackSize(), Item.ABSOLUTE_MAX_STACK_SIZE);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return true;
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        ItemStack current = buffer().getStack(index);
        int currentAmount = current.getCount();

        // Check if we can insert (empty slot or matching resource)
        if (currentAmount > 0 && !resource.matches(current)) {
            return 0;
        }

        int capacity = (int) getCapacityAsLong(index, resource);
        int toInsert = Math.min(amount, capacity - currentAmount);

        if (toInsert > 0) {
            slots.get(index).updateSnapshots(transaction);
            if (currentAmount == 0) {
                buffer().setStack(index, resource.toStack(toInsert));
            } else {
                current.grow(toInsert);
            }
            return toInsert;
        }
        return 0;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        ItemStack current = buffer().getStack(index);

        if (!resource.matches(current)) {
            return 0;
        }

        int currentAmount = current.getCount();
        int toExtract = Math.min(amount, currentAmount);

        if (toExtract > 0) {
            slots.get(index).updateSnapshots(transaction);
            if (toExtract == currentAmount) {
                buffer().setStack(index, ItemStack.EMPTY);
            } else {
                current.shrink(toExtract);
            }
            return toExtract;
        }
        return 0;
    }
}
