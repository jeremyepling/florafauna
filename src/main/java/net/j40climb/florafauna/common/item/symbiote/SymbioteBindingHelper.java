package net.j40climb.florafauna.common.item.symbiote;

import net.j40climb.florafauna.common.item.symbiote.progress.ProgressSignalTracker;
import net.j40climb.florafauna.common.item.symbiote.voice.VoiceCooldownState;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Helper class for symbiote binding and unbinding operations.
 * Centralizes the logic so it can be called from both the Cocoon Chamber
 * and the admin command.
 */
public final class SymbioteBindingHelper {

    private SymbioteBindingHelper() {} // Utility class

    /**
     * Result of a binding operation.
     */
    public record BindResult(boolean success, String messageKey) {}

    /**
     * Result of an unbinding operation.
     */
    public record UnbindResult(boolean success, String messageKey, ItemStack symbioteItem) {
        public static UnbindResult failure(String messageKey) {
            return new UnbindResult(false, messageKey, ItemStack.EMPTY);
        }
    }

    /**
     * Binds a symbiote to the player.
     * The symbiote item is consumed in the process.
     *
     * @param player The server player to bind to
     * @param symbioteItem The dormant symbiote item to bind (will be consumed)
     * @return BindResult indicating success/failure and a message key
     */
    public static BindResult bindSymbiote(ServerPlayer player, ItemStack symbioteItem) {
        // Check if player already has a bonded symbiote
        PlayerSymbioteData currentData = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
        if (currentData.symbioteState().isBonded()) {
            return new BindResult(false, "symbiote.florafauna.already_bonded");
        }

        // Check if the item is a valid dormant symbiote
        if (!(symbioteItem.getItem() instanceof DormantSymbioteItem)) {
            return new BindResult(false, "symbiote.florafauna.invalid_item");
        }

        // Read symbiote state from item components
        SymbioteData itemData = symbioteItem.getOrDefault(
                FloraFaunaRegistry.SYMBIOTE_DATA,
                SymbioteData.DEFAULT
        );
        ProgressSignalTracker progressTracker = symbioteItem.getOrDefault(
                FloraFaunaRegistry.SYMBIOTE_PROGRESS,
                ProgressSignalTracker.DEFAULT
        );

        // Bond the symbiote - use helper method to preserve cocoon state
        PlayerSymbioteData bondedData = currentData.withSymbioteFromItem(itemData, player.level().getGameTime());

        player.setData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA, bondedData);
        player.setData(FloraFaunaRegistry.SYMBIOTE_PROGRESS_ATTACHMENT, progressTracker);
        player.setData(FloraFaunaRegistry.VOICE_COOLDOWNS, VoiceCooldownState.DEFAULT);

        // Consume the item
        symbioteItem.shrink(1);

        return new BindResult(true, "symbiote.florafauna.bonded_success");
    }

    /**
     * Unbinds the symbiote from the player and creates a dormant symbiote item.
     * The symbiote's memories (progress) are preserved in the item.
     *
     * @param player The server player to unbind from
     * @return UnbindResult containing the created item (if successful)
     */
    public static UnbindResult unbindSymbiote(ServerPlayer player) {
        PlayerSymbioteData currentData = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);

        if (!currentData.symbioteState().isBonded()) {
            return UnbindResult.failure("symbiote.florafauna.not_bonded");
        }

        // Read current player state (progress tracker preserves memories)
        ProgressSignalTracker progressTracker = player.getData(FloraFaunaRegistry.SYMBIOTE_PROGRESS_ATTACHMENT);

        // Create dormant symbiote item with current player state
        ItemStack symbioteItem = new ItemStack(FloraFaunaRegistry.DORMANT_SYMBIOTE.get());

        // Copy player symbiote state to item component
        SymbioteData itemData = currentData.toItemData();
        symbioteItem.set(FloraFaunaRegistry.SYMBIOTE_DATA, itemData);
        symbioteItem.set(FloraFaunaRegistry.SYMBIOTE_PROGRESS, progressTracker);

        // Reset symbiote bond state while preserving cocoon state
        PlayerSymbioteData unbondedData = currentData.withSymbioteReset();
        player.setData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA, unbondedData);
        player.setData(FloraFaunaRegistry.SYMBIOTE_PROGRESS_ATTACHMENT, ProgressSignalTracker.DEFAULT);
        player.setData(FloraFaunaRegistry.VOICE_COOLDOWNS, VoiceCooldownState.DEFAULT);

        return new UnbindResult(true, "symbiote.florafauna.unbonded_success", symbioteItem);
    }

    /**
     * Finds a dormant symbiote item in the player's inventory.
     *
     * @param player The player to search
     * @return The first dormant symbiote item found, or ItemStack.EMPTY if none
     */
    public static ItemStack findDormantSymbioteInInventory(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof DormantSymbioteItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
