package net.j40climb.florafauna.common.symbiote.progress;

import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.server.level.ServerPlayer;

/**
 * Utility for updating progress signals based on game events.
 * Handles state transitions and stall detection.
 */
public class ProgressSignalUpdater {

    /**
     * Thresholds for state transitions based on interaction count
     */
    private static final int TOUCHED_THRESHOLD = 3;
    private static final int STABILIZED_THRESHOLD = 10;
    private static final int INTEGRATED_THRESHOLD = 25;

    /**
     * Stall threshold in ticks (5 hours of game time)
     */
    private static final long STALL_THRESHOLD_TICKS = 360000L;

    /**
     * Record an observation of a concept.
     * Creates the signal if it doesn't exist, or advances its state.
     *
     * @param player The player
     * @param conceptId The concept identifier
     */
    public static void recordObservation(ServerPlayer player, String conceptId) {
        ProgressSignalTracker tracker = player.getData(FloraFaunaRegistry.SYMBIOTE_PROGRESS_ATTACHMENT);
        long currentTick = player.level().getGameTime();

        var existingSignal = tracker.getSignal(conceptId);

        ConceptSignal updated;
        if (existingSignal.isEmpty()) {
            // First time seeing this concept
            updated = ConceptSignal.firstSeen(conceptId, currentTick);
        } else {
            ConceptSignal signal = existingSignal.get();
            // Increment interaction and potentially advance state
            updated = advanceSignal(signal, currentTick);
        }

        ProgressSignalTracker newTracker = tracker
                .withSignalUpdated(conceptId, updated)
                .withProgressTick(currentTick);

        player.setData(FloraFaunaRegistry.SYMBIOTE_PROGRESS_ATTACHMENT, newTracker);
    }

    /**
     * Advance a signal based on interaction count.
     */
    private static ConceptSignal advanceSignal(ConceptSignal signal, long currentTick) {
        ConceptSignal incremented = signal.incrementInteraction(currentTick);
        int count = incremented.interactionCount();

        SignalState newState = switch (signal.state()) {
            case UNSEEN -> SignalState.SEEN;
            case SEEN -> count >= TOUCHED_THRESHOLD ? SignalState.TOUCHED : SignalState.SEEN;
            case TOUCHED -> count >= STABILIZED_THRESHOLD ? SignalState.STABILIZED : SignalState.TOUCHED;
            case STABILIZED -> count >= INTEGRATED_THRESHOLD ? SignalState.INTEGRATED : SignalState.STABILIZED;
            case INTEGRATED -> SignalState.INTEGRATED; // Terminal state
            case NEGLECTED -> SignalState.TOUCHED; // Recovery from neglect
        };

        if (newState != signal.state()) {
            return incremented.withState(newState, currentTick);
        }
        return incremented;
    }

    /**
     * Check all signals for stalls and mark neglected ones.
     * Should be called periodically (e.g., every minute).
     *
     * @param player The player
     */
    public static void checkForStalls(ServerPlayer player) {
        ProgressSignalTracker tracker = player.getData(FloraFaunaRegistry.SYMBIOTE_PROGRESS_ATTACHMENT);
        long currentTick = player.level().getGameTime();

        boolean anyChanges = false;
        ProgressSignalTracker updated = tracker;

        for (ConceptSignal signal : tracker.getPartiallyCompleteSignals()) {
            long ticksSince = currentTick - signal.lastTransitionTick();

            if (ticksSince >= STALL_THRESHOLD_TICKS && signal.state() != SignalState.NEGLECTED) {
                ConceptSignal neglected = signal.withState(SignalState.NEGLECTED, currentTick);
                updated = updated.withSignalUpdated(signal.conceptId(), neglected);
                anyChanges = true;
            }
        }

        if (anyChanges) {
            player.setData(FloraFaunaRegistry.SYMBIOTE_PROGRESS_ATTACHMENT, updated);
        }
    }

    /**
     * Force a concept to a specific state (for debugging/commands).
     *
     * @param player The player
     * @param conceptId The concept identifier
     * @param state The target state
     */
    public static void forceState(ServerPlayer player, String conceptId, SignalState state) {
        ProgressSignalTracker tracker = player.getData(FloraFaunaRegistry.SYMBIOTE_PROGRESS_ATTACHMENT);
        long currentTick = player.level().getGameTime();

        ConceptSignal signal = tracker.getSignal(conceptId)
                .orElse(ConceptSignal.firstSeen(conceptId, currentTick));

        ConceptSignal updated = signal.withState(state, currentTick);

        ProgressSignalTracker newTracker = tracker.withSignalUpdated(conceptId, updated);
        player.setData(FloraFaunaRegistry.SYMBIOTE_PROGRESS_ATTACHMENT, newTracker);
    }

    /**
     * Get a summary of progress for debugging.
     */
    public static String getProgressSummary(ServerPlayer player) {
        ProgressSignalTracker tracker = player.getData(FloraFaunaRegistry.SYMBIOTE_PROGRESS_ATTACHMENT);
        long currentTick = player.level().getGameTime();

        StringBuilder sb = new StringBuilder();
        sb.append("Progress Signals:\n");

        if (tracker.signals().isEmpty()) {
            sb.append("  (none)\n");
        } else {
            for (ConceptSignal signal : tracker.signals().values()) {
                int stallScore = signal.calculateStallScore(currentTick);
                sb.append(String.format("  %s: %s (x%d, stall: %d%%)\n",
                        signal.conceptId(),
                        signal.state().name(),
                        signal.interactionCount(),
                        stallScore));
            }
        }

        sb.append(String.format("Dream level: %d, Last dream: %d ticks ago\n",
                tracker.dreamLevel(),
                tracker.lastDreamTick() > 0 ? currentTick - tracker.lastDreamTick() : -1));

        return sb.toString();
    }
}
