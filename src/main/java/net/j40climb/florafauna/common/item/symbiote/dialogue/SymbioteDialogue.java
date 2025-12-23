package net.j40climb.florafauna.common.item.symbiote.dialogue;

import net.j40climb.florafauna.common.RegisterAttachmentTypes;
import net.j40climb.florafauna.common.item.symbiote.tracking.SymbioteEventTracker;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;

/**
 * Main dialogue system manager for the symbiote.
 * Coordinates trigger checking, event tracking, and message sending.
 */
public class SymbioteDialogue {

    /**
     * Attempt to trigger a dialogue event.
     * Checks if the event should trigger based on first-time tracking.
     *
     * @param player The player to send the message to
     * @param trigger The dialogue trigger to attempt
     */
    public static void tryTrigger(ServerPlayer player, SymbioteDialogueTrigger trigger) {
        SymbioteEventTracker tracker = player.getData(RegisterAttachmentTypes.SYMBIOTE_EVENT_TRACKER);

        // If this is a first-time-only event, check if it's already been triggered
        if (trigger.isFirstTimeOnly()) {
            if (tracker.hasTriggered(trigger.getKey())) {
                // Already triggered, don't send message
                return;
            }

            // Mark as triggered
            SymbioteEventTracker newTracker = tracker.withTriggered(trigger.getKey());
            player.setData(RegisterAttachmentTypes.SYMBIOTE_EVENT_TRACKER, newTracker);
        }

        // Send the message
        SymbioteMessageSender.sendMessage(player, trigger.getLocalizationKey());
    }

    /**
     * Force trigger a dialogue event without checking first-time status.
     * Useful for events that should always trigger (like bonding/unbonding).
     *
     * @param player The player to send the message to
     * @param trigger The dialogue trigger to force
     */
    public static void forceTrigger(ServerPlayer player, SymbioteDialogueTrigger trigger) {
        SymbioteMessageSender.sendMessage(player, trigger.getLocalizationKey());
    }

    /**
     * Reset a specific event tracker (for testing or admin commands)
     *
     * @param player The player to reset tracking for
     * @param trigger The trigger to reset
     */
    public static void resetTrigger(ServerPlayer player, SymbioteDialogueTrigger trigger) {
        SymbioteEventTracker tracker = player.getData(RegisterAttachmentTypes.SYMBIOTE_EVENT_TRACKER);
        // Create new tracker without this event
        Set<String> newSet = new HashSet<>(tracker.triggeredEvents());
        newSet.remove(trigger.getKey());
        SymbioteEventTracker newTracker = new SymbioteEventTracker(newSet);
        player.setData(RegisterAttachmentTypes.SYMBIOTE_EVENT_TRACKER, newTracker);
    }

    /**
     * Reset all event tracking for a player.
     * Available for future use (e.g., admin commands).
     * NOT used on unbonding - symbiote memory persists across bond/unbond cycles.
     *
     * @param player The player to reset tracking for
     */
    public static void resetAllTriggers(ServerPlayer player) {
        player.setData(RegisterAttachmentTypes.SYMBIOTE_EVENT_TRACKER, SymbioteEventTracker.DEFAULT);
    }
}
