package net.j40climb.florafauna.common.item.symbiote.observation;

import net.j40climb.florafauna.common.item.symbiote.dialogue.DialogueSelectionContext;
import net.j40climb.florafauna.common.item.symbiote.dialogue.SymbioteDialogueRepository;
import net.j40climb.florafauna.common.item.symbiote.progress.ProgressSignalTracker;
import net.j40climb.florafauna.common.item.symbiote.voice.SymbioteVoiceService;
import net.j40climb.florafauna.common.item.symbiote.voice.VoiceTier;
import net.j40climb.florafauna.setup.ModRegistry;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Optional;

/**
 * Central arbiter for observation events.
 * Receives game events, scores them, determines tier, selects lines,
 * and routes output through the voice service.
 */
public class ObservationArbiter {

    /**
     * Severity threshold for potential Tier 2 events
     */
    private static final int TIER2_SEVERITY_THRESHOLD = 80;

    /**
     * Process an observation event and potentially trigger voice output.
     *
     * @param player The player who triggered the observation
     * @param category The category of the observation
     * @param severity Severity score (0-100)
     * @param context Additional context for line selection
     */
    public static void observe(
            ServerPlayer player,
            ObservationCategory category,
            int severity,
            Map<String, Object> context
    ) {
        // Get progress tracker for tier determination and line selection
        ProgressSignalTracker progress = player.getData(ModRegistry.SYMBIOTE_PROGRESS_ATTACHMENT);

        // Determine the appropriate tier
        VoiceTier tier = determineTier(category, severity, progress);

        // Build selection context
        DialogueSelectionContext selectionContext = new DialogueSelectionContext(
                tier, category, severity, context, progress
        );

        // Select a dialogue from the repository
        Optional<String> dialogueKey = SymbioteDialogueRepository.selectDialogue(selectionContext);

        // Try to speak if we have a dialogue
        if (dialogueKey.isPresent()) {
            SymbioteVoiceService.trySpeak(player, tier, category, dialogueKey.get());
        }

        // Update progress signals based on this observation
        updateProgressSignals(player, category, severity);
    }

    /**
     * Process an observation with simple context (no additional data).
     */
    public static void observe(
            ServerPlayer player,
            ObservationCategory category,
            int severity
    ) {
        observe(player, category, severity, Map.of());
    }

    /**
     * Determine the appropriate tier for an observation.
     *
     * Tier 2 (Breakthrough) is reserved for:
     * - Bonding milestone events
     * - Very high severity events (first occurrence)
     * - First-time category experiences at high severity
     *
     * @param category The observation category
     * @param severity The severity score
     * @param progress The player's progress tracker
     * @return The appropriate voice tier
     */
    private static VoiceTier determineTier(
            ObservationCategory category,
            int severity,
            ProgressSignalTracker progress
    ) {
        // Bonding milestones are always Tier 2
        if (category == ObservationCategory.BONDING_MILESTONE) {
            return VoiceTier.TIER_2_BREAKTHROUGH;
        }

        // Check if this is a first-time high-severity event
        if (severity >= TIER2_SEVERITY_THRESHOLD) {
            String conceptKey = "first_" + category.getKey();
            if (!progress.hasReachedState(conceptKey,
                    net.j40climb.florafauna.common.item.symbiote.progress.SignalState.SEEN)) {
                return VoiceTier.TIER_2_BREAKTHROUGH;
            }
        }

        // Default to ambient tier
        return VoiceTier.TIER_1_AMBIENT;
    }

    /**
     * Update progress signals based on an observation.
     * This tracks what the player has experienced.
     *
     * @param player The player
     * @param category The observation category
     * @param severity The severity
     */
    private static void updateProgressSignals(
            ServerPlayer player,
            ObservationCategory category,
            int severity
    ) {
        ProgressSignalTracker tracker = player.getData(ModRegistry.SYMBIOTE_PROGRESS_ATTACHMENT);
        long currentTick = player.level().getGameTime();

        // Track first-time category experience
        String conceptKey = "first_" + category.getKey();

        var existingSignal = tracker.getSignal(conceptKey);
        if (existingSignal.isEmpty()) {
            // First time seeing this category
            var signal = net.j40climb.florafauna.common.item.symbiote.progress.ConceptSignal
                    .firstSeen(conceptKey, currentTick);
            tracker = tracker.withSignalUpdated(conceptKey, signal);
        } else {
            // Increment interaction count
            var updated = existingSignal.get().incrementInteraction(currentTick);
            tracker = tracker.withSignalUpdated(conceptKey, updated);
        }

        // Mark progress tick
        tracker = tracker.withProgressTick(currentTick);

        player.setData(ModRegistry.SYMBIOTE_PROGRESS_ATTACHMENT, tracker);
    }

    /**
     * Calculate severity from damage amount.
     * Maps damage to a 0-100 scale.
     *
     * @param damage The damage amount
     * @return Severity score (0-100)
     */
    public static int damageToSeverity(float damage) {
        // 10 damage (5 hearts) = 100 severity
        return Math.min(100, Math.max(0, (int) (damage * 10)));
    }
}
