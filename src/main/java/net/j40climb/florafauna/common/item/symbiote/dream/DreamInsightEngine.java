package net.j40climb.florafauna.common.item.symbiote.dream;

import net.j40climb.florafauna.common.RegisterAttachmentTypes;
import net.j40climb.florafauna.common.item.symbiote.PlayerSymbioteData;
import net.j40climb.florafauna.common.item.symbiote.dialogue.SymbioteDialogueRepository;
import net.j40climb.florafauna.common.item.symbiote.progress.ProgressSignalTracker;
import net.j40climb.florafauna.common.item.symbiote.voice.SymbioteVoiceService;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Generates dream messages based on player progress signals.
 * Dreams escalate in clarity with repeated requests when no progress is made.
 */
public class DreamInsightEngine {

    /**
     * Tick threshold for dream escalation (2 in-game days = 48000 ticks)
     */
    private static final long ESCALATION_WINDOW_TICKS = 48000L;

    /**
     * Tick threshold for dream reset (5 in-game days = 120000 ticks)
     */
    private static final long RESET_WINDOW_TICKS = 120000L;

    /**
     * Process a dream request from the player.
     * Called when the player uses the /symbiote dream command.
     *
     * @param player The player requesting the dream
     * @return true if a dream was delivered, false if not bonded
     */
    public static boolean processDream(ServerPlayer player) {
        // Must be bonded
        PlayerSymbioteData symbioteData = player.getData(RegisterAttachmentTypes.PLAYER_SYMBIOTE_DATA);
        if (!symbioteData.bonded()) {
            return false;
        }

        ProgressSignalTracker progress = player.getData(RegisterAttachmentTypes.SYMBIOTE_PROGRESS);
        long currentTick = player.level().getGameTime();

        // Calculate dream level based on escalation rules
        DreamLevel level = calculateDreamLevel(progress, currentTick);

        // Build dream context
        DreamContext context = DreamContext.fromTracker(progress, currentTick, level);

        // Select and deliver dream message
        deliverDream(player, context, level);

        // Update dream tracking state
        int newDreamLevel = level.ordinal() + 1; // Store as 1-indexed for next calculation
        ProgressSignalTracker updated = progress.withDreamState(currentTick, newDreamLevel);
        player.setData(RegisterAttachmentTypes.SYMBIOTE_PROGRESS, updated);

        return true;
    }

    /**
     * Force a dream at a specific level (for debugging).
     *
     * @param player The player
     * @param level The dream level to force
     * @return true if delivered
     */
    public static boolean forceDream(ServerPlayer player, DreamLevel level) {
        PlayerSymbioteData symbioteData = player.getData(RegisterAttachmentTypes.PLAYER_SYMBIOTE_DATA);
        if (!symbioteData.bonded()) {
            return false;
        }

        ProgressSignalTracker progress = player.getData(RegisterAttachmentTypes.SYMBIOTE_PROGRESS);
        long currentTick = player.level().getGameTime();

        DreamContext context = DreamContext.fromTracker(progress, currentTick, level);
        deliverDream(player, context, level);

        return true;
    }

    /**
     * Calculate the appropriate dream level based on escalation rules.
     *
     * Rules:
     * - First dream or long gap (>5 days): L1 Reflective
     * - Recent dream (<2 days) with no progress: Escalate
     * - Recent dream (<2 days) with progress: Reset to L1
     */
    private static DreamLevel calculateDreamLevel(ProgressSignalTracker progress, long currentTick) {
        // First dream ever
        if (progress.lastDreamTick() == 0) {
            return DreamLevel.L1_REFLECTIVE;
        }

        long ticksSinceLastDream = currentTick - progress.lastDreamTick();

        // Long gap - reset to L1
        if (ticksSinceLastDream > RESET_WINDOW_TICKS) {
            return DreamLevel.L1_REFLECTIVE;
        }

        // Recent dream with progress - reset to L1
        if (progress.hasProgressSinceLastDream()) {
            return DreamLevel.L1_REFLECTIVE;
        }

        // Recent dream without progress - escalate
        if (ticksSinceLastDream < ESCALATION_WINDOW_TICKS) {
            // Escalate based on previous dream level (stored as 1-indexed)
            int previousLevel = progress.dreamLevel();
            return DreamLevel.fromOrdinal(previousLevel); // This effectively escalates
        }

        // Medium gap - maintain current level
        return DreamLevel.fromOrdinal(progress.dreamLevel() - 1);
    }

    /**
     * Deliver the dream message to the player.
     */
    private static void deliverDream(ServerPlayer player, DreamContext context, DreamLevel level) {
        // Try to get a dialogue from the repository
        Optional<String> dialogueKey = SymbioteDialogueRepository.selectDreamDialogue(level);

        if (dialogueKey.isPresent()) {
            SymbioteVoiceService.sendDreamMessage(player, dialogueKey.get(), level.getColor());
        } else {
            // Fallback if no lines loaded
            String fallbackKey = getFallbackDreamKey(level);
            SymbioteVoiceService.sendDreamMessage(player, fallbackKey, level.getColor());
        }
    }

    /**
     * Get a fallback dream key if the repository is empty.
     */
    private static String getFallbackDreamKey(DreamLevel level) {
        return switch (level) {
            case L1_REFLECTIVE -> "symbiote.dream.reflective.general_1";
            case L2_DIRECTIONAL -> "symbiote.dream.directional.general_1";
            case L3_ANCHORED -> "symbiote.dream.anchored.general_1";
        };
    }

    /**
     * Get the current dream level for a player (for display).
     */
    public static DreamLevel getCurrentDreamLevel(ServerPlayer player) {
        ProgressSignalTracker progress = player.getData(RegisterAttachmentTypes.SYMBIOTE_PROGRESS);
        long currentTick = player.level().getGameTime();
        return calculateDreamLevel(progress, currentTick);
    }
}
