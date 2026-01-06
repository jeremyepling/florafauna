package net.j40climb.florafauna.common.block.vacuum;

import net.minecraft.world.item.ItemStack;

/**
 * Interface for item buffers that temporarily store items.
 * Provides common operations for buffer-based item storage.
 *
 * Implementations should support:
 * - Stack merging when adding items
 * - FIFO-style retrieval via poll()
 * - Capacity checking to prevent item loss
 */
public interface IItemBuffer {

    /**
     * Checks if the buffer can accept at least 1 item from the given stack.
     *
     * @param stack The stack to check
     * @return true if the buffer can accept at least 1 item
     */
    boolean canAccept(ItemStack stack);

    /**
     * Adds an item stack to the buffer, merging with existing stacks when possible.
     * The input stack will be modified to reflect any items that couldn't be added.
     *
     * @param stack The stack to add (will be modified)
     * @return Number of items actually added
     */
    int add(ItemStack stack);

    /**
     * Removes and returns the first non-empty stack from the buffer (FIFO).
     *
     * @return The removed stack, or EMPTY if buffer is empty
     */
    ItemStack poll();

    /**
     * Peeks at the first non-empty stack without removing it.
     *
     * @return The first non-empty stack, or EMPTY if buffer is empty
     */
    ItemStack peek();

    /**
     * Checks if the buffer is full (cannot accept more items).
     *
     * @return true if buffer is at capacity
     */
    boolean isFull();

    /**
     * Checks if the buffer is empty.
     *
     * @return true if buffer contains no items
     */
    boolean isEmpty();

    /**
     * Returns the total item count across all stacks.
     *
     * @return Total number of items in the buffer
     */
    int getTotalItemCount();

    /**
     * Gets the maximum capacity in items.
     *
     * @return Maximum number of items this buffer can hold
     */
    int getMaxCapacity();

    /**
     * Clears all items from the buffer.
     */
    void clear();
}
