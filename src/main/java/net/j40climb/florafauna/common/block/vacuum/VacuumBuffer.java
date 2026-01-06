package net.j40climb.florafauna.common.block.vacuum;

import net.j40climb.florafauna.Config;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

/**
 * A buffer for temporarily storing items in vacuum-type blocks.
 * Acts like a chest inventory with automatic stack merging.
 *
 * Key behaviors:
 * - Items are merged with existing stacks when possible
 * - Never voids items (canAccept checks first)
 * - Provides FIFO-style poll for transfer
 */
public class VacuumBuffer implements IItemBuffer {
    private final NonNullList<ItemStack> stacks;
    private final int maxStacks;

    /**
     * Creates a new buffer with the specified capacity.
     *
     * @param maxStacks Maximum number of unique stacks to hold
     */
    public VacuumBuffer(int maxStacks) {
        this.maxStacks = maxStacks;
        this.stacks = NonNullList.withSize(maxStacks, ItemStack.EMPTY);
    }

    /**
     * Creates a buffer with default capacity from config.
     */
    public static VacuumBuffer create() {
        return new VacuumBuffer(Config.maxBufferedStacks);
    }

    /**
     * Checks if the buffer can accept the given stack.
     * Returns true if there's space to merge or add.
     *
     * @param stack The stack to check
     * @return true if the buffer can accept at least 1 item
     */
    @Override
    public boolean canAccept(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        // Check if we can merge with existing stacks
        for (ItemStack existing : stacks) {
            if (existing.isEmpty()) {
                return true; // Found empty slot
            }
            if (ItemStack.isSameItemSameComponents(existing, stack)) {
                if (existing.getCount() < existing.getMaxStackSize()) {
                    return true; // Can merge
                }
            }
        }
        return false;
    }

    /**
     * Adds an item stack to the buffer, merging with existing stacks when possible.
     * The input stack will be modified to reflect any items that couldn't be added.
     *
     * @param stack The stack to add (will be modified)
     * @return Number of items actually added
     */
    @Override
    public int add(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        int originalCount = stack.getCount();

        // First pass: try to merge with existing stacks
        for (int i = 0; i < stacks.size() && !stack.isEmpty(); i++) {
            ItemStack existing = stacks.get(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                if (space > 0) {
                    int toAdd = Math.min(space, stack.getCount());
                    existing.grow(toAdd);
                    stack.shrink(toAdd);
                }
            }
        }

        // Second pass: add to empty slots
        for (int i = 0; i < stacks.size() && !stack.isEmpty(); i++) {
            if (stacks.get(i).isEmpty()) {
                int toAdd = Math.min(stack.getMaxStackSize(), stack.getCount());
                stacks.set(i, stack.copyWithCount(toAdd));
                stack.shrink(toAdd);
            }
        }

        return originalCount - stack.getCount();
    }

    /**
     * Removes and returns the first non-empty stack from the buffer (FIFO).
     *
     * @return The removed stack, or EMPTY if buffer is empty
     */
    @Override
    public ItemStack poll() {
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (!stack.isEmpty()) {
                stacks.set(i, ItemStack.EMPTY);
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Peeks at the first non-empty stack without removing it.
     *
     * @return The first non-empty stack, or EMPTY if buffer is empty
     */
    @Override
    public ItemStack peek() {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Returns the index of the first non-empty slot, or -1 if empty.
     */
    public int getFirstNonEmptySlot() {
        for (int i = 0; i < stacks.size(); i++) {
            if (!stacks.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Gets a stack at the specified index.
     */
    public ItemStack getStack(int index) {
        if (index < 0 || index >= stacks.size()) {
            return ItemStack.EMPTY;
        }
        return stacks.get(index);
    }

    /**
     * Sets a stack at the specified index.
     */
    public void setStack(int index, ItemStack stack) {
        if (index >= 0 && index < stacks.size()) {
            stacks.set(index, stack);
        }
    }

    /**
     * Checks if the buffer is full (no empty slots and all stacks at max).
     */
    @Override
    public boolean isFull() {
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                return false;
            }
            if (stack.getCount() < stack.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the buffer is empty.
     */
    @Override
    public boolean isEmpty() {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the number of non-empty slots.
     */
    public int getUsedSlots() {
        int count = 0;
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the total item count across all stacks.
     */
    @Override
    public int getTotalItemCount() {
        int count = 0;
        for (ItemStack stack : stacks) {
            count += stack.getCount();
        }
        return count;
    }

    /**
     * Alias for getTotalItemCount() for conciseness.
     */
    public int getItemCount() {
        return getTotalItemCount();
    }

    /**
     * Gets the maximum number of stacks this buffer can hold.
     */
    public int getMaxStacks() {
        return maxStacks;
    }

    /**
     * Gets the maximum capacity in items (assuming max stack size of 64).
     */
    @Override
    public int getMaxCapacity() {
        return maxStacks * 64;
    }

    /**
     * Clears all items from the buffer.
     */
    @Override
    public void clear() {
        for (int i = 0; i < stacks.size(); i++) {
            stacks.set(i, ItemStack.EMPTY);
        }
    }

    /**
     * Returns a copy of all non-empty stacks for dropping on block break.
     */
    public List<ItemStack> getContentsForDrop() {
        List<ItemStack> drops = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
        }
        return drops;
    }

    // ==================== SERIALIZATION ====================

    private static final String KEY_ITEMS = "buffer_items";

    /**
     * Saves the buffer contents using ValueOutput.
     */
    public void serialize(ValueOutput output) {
        output.store(KEY_ITEMS, ItemStack.OPTIONAL_CODEC.listOf(), stacks.stream().toList());
    }

    /**
     * Loads buffer contents using ValueInput.
     */
    public void deserialize(ValueInput input) {
        clear();
        List<ItemStack> loaded = input.read(KEY_ITEMS, ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < Math.min(loaded.size(), stacks.size()); i++) {
            stacks.set(i, loaded.get(i));
        }
    }
}
