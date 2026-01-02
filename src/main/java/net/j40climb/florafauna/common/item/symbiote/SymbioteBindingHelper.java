package net.j40climb.florafauna.common.item.symbiote;

import net.j40climb.florafauna.common.RegisterAttachmentTypes;
import net.j40climb.florafauna.common.RegisterDataComponentTypes;
import net.j40climb.florafauna.common.item.RegisterItems;
import net.j40climb.florafauna.common.item.symbiote.progress.ProgressSignalTracker;
import net.j40climb.florafauna.common.item.symbiote.voice.VoiceCooldownState;
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
        PlayerSymbioteData currentData = player.getData(RegisterAttachmentTypes.PLAYER_SYMBIOTE_DATA);
        if (currentData.bonded()) {
            return new BindResult(false, "symbiote.florafauna.already_bonded");
        }

        // Check if the item is a valid dormant symbiote
        if (!(symbioteItem.getItem() instanceof DormantSymbioteItem)) {
            return new BindResult(false, "symbiote.florafauna.invalid_item");
        }

        // Read symbiote state from item components
        SymbioteData itemData = symbioteItem.getOrDefault(
                RegisterDataComponentTypes.SYMBIOTE_DATA,
                SymbioteData.DEFAULT
        );
        ProgressSignalTracker progressTracker = symbioteItem.getOrDefault(
                RegisterDataComponentTypes.SYMBIOTE_PROGRESS,
                ProgressSignalTracker.DEFAULT
        );

        // Bond the symbiote - use helper method to preserve cocoon state
        PlayerSymbioteData bondedData = currentData.withSymbioteFromItem(itemData, player.level().getGameTime());

        player.setData(RegisterAttachmentTypes.PLAYER_SYMBIOTE_DATA, bondedData);
        player.setData(RegisterAttachmentTypes.SYMBIOTE_PROGRESS, progressTracker);
        player.setData(RegisterAttachmentTypes.VOICE_COOLDOWNS, VoiceCooldownState.DEFAULT);

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
        PlayerSymbioteData currentData = player.getData(RegisterAttachmentTypes.PLAYER_SYMBIOTE_DATA);

        if (!currentData.bonded()) {
            return UnbindResult.failure("symbiote.florafauna.not_bonded");
        }

        // Read current player state (progress tracker preserves memories)
        ProgressSignalTracker progressTracker = player.getData(RegisterAttachmentTypes.SYMBIOTE_PROGRESS);

        // Create dormant symbiote item with current player state
        ItemStack symbioteItem = new ItemStack(RegisterItems.DORMANT_SYMBIOTE.get());

        // Copy player symbiote state to item component
        SymbioteData itemData = currentData.toItemData();
        symbioteItem.set(RegisterDataComponentTypes.SYMBIOTE_DATA, itemData);
        symbioteItem.set(RegisterDataComponentTypes.SYMBIOTE_PROGRESS, progressTracker);

        // Reset symbiote bond state while preserving cocoon state
        PlayerSymbioteData unbondedData = currentData.withSymbioteReset();
        player.setData(RegisterAttachmentTypes.PLAYER_SYMBIOTE_DATA, unbondedData);
        player.setData(RegisterAttachmentTypes.SYMBIOTE_PROGRESS, ProgressSignalTracker.DEFAULT);
        player.setData(RegisterAttachmentTypes.VOICE_COOLDOWNS, VoiceCooldownState.DEFAULT);

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
