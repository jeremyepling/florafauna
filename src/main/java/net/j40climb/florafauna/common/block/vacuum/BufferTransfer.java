package net.j40climb.florafauna.common.block.vacuum;

import net.minecraft.world.item.ItemStack;

/**
 * Utility for transferring items between IItemBuffer implementations.
 * Handles partial transfers and respects buffer capacities.
 */
public class BufferTransfer {

    /**
     * Result of a transfer operation.
     */
    public record TransferResult(int itemsTransferred, boolean sourceEmpty, boolean destinationFull) {
        public static final TransferResult NONE = new TransferResult(0, false, false);
    }

    /**
     * Transfers items from source to destination buffer.
     * Transfers one stack at a time, respecting destination capacity.
     *
     * @param source The buffer to take items from
     * @param destination The buffer to put items into
     * @param maxItems Maximum number of items to transfer (0 = unlimited)
     * @return Result containing transfer statistics
     */
    public static TransferResult transfer(IItemBuffer source, IItemBuffer destination, int maxItems) {
        if (source.isEmpty() || destination.isFull()) {
            return new TransferResult(0, source.isEmpty(), destination.isFull());
        }

        int totalTransferred = 0;
        int remainingLimit = maxItems <= 0 ? Integer.MAX_VALUE : maxItems;

        while (remainingLimit > 0 && !source.isEmpty() && !destination.isFull()) {
            // Peek at the next stack
            ItemStack stack = source.peek();
            if (stack.isEmpty()) {
                break;
            }

            // Check if destination can accept this item type
            if (!destination.canAccept(stack)) {
                // Can't transfer this item type, stop
                break;
            }

            // Poll the stack from source
            ItemStack polled = source.poll();
            if (polled.isEmpty()) {
                break;
            }

            // Limit the transfer amount
            int toTransfer = Math.min(polled.getCount(), remainingLimit);
            ItemStack transferStack = polled.copyWithCount(toTransfer);

            // Add to destination
            int added = destination.add(transferStack);
            totalTransferred += added;
            remainingLimit -= added;

            // Handle remainder (items that couldn't be transferred)
            int remainder = polled.getCount() - added;
            if (remainder > 0) {
                // Put remainder back in source
                ItemStack remainderStack = polled.copyWithCount(remainder);
                source.add(remainderStack);
                // Destination is full for this item type
                break;
            }
        }

        return new TransferResult(totalTransferred, source.isEmpty(), destination.isFull());
    }

    /**
     * Transfers all possible items from source to destination.
     *
     * @param source The buffer to take items from
     * @param destination The buffer to put items into
     * @return Result containing transfer statistics
     */
    public static TransferResult transferAll(IItemBuffer source, IItemBuffer destination) {
        return transfer(source, destination, 0);
    }

    /**
     * Transfers a single stack from source to destination.
     *
     * @param source The buffer to take items from
     * @param destination The buffer to put items into
     * @return Number of items transferred
     */
    public static int transferOneStack(IItemBuffer source, IItemBuffer destination) {
        if (source.isEmpty() || destination.isFull()) {
            return 0;
        }

        ItemStack stack = source.peek();
        if (stack.isEmpty() || !destination.canAccept(stack)) {
            return 0;
        }

        ItemStack polled = source.poll();
        if (polled.isEmpty()) {
            return 0;
        }

        int added = destination.add(polled.copy());

        // Put remainder back if not all was transferred
        int remainder = polled.getCount() - added;
        if (remainder > 0) {
            source.add(polled.copyWithCount(remainder));
        }

        return added;
    }
}
