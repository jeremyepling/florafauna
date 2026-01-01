package net.j40climb.florafauna.common.item.symbiote.dream;

import net.j40climb.florafauna.common.item.symbiote.progress.ConceptSignal;
import net.j40climb.florafauna.common.item.symbiote.progress.ProgressSignalTracker;

import java.util.List;

/**
 * Context for dream generation.
 * Contains all the information needed to select appropriate dream content.
 */
public record DreamContext(
    List<ConceptSignal> stalledSignals,
    List<ConceptSignal> partiallyComplete,
    DreamLevel level,
    boolean hasProgressSinceLastDream,
    long ticksSinceLastDream
) {
    /**
     * Create a dream context from player progress state
     *
     * @param tracker The player's progress signal tracker
     * @param currentTick Current game time
     * @param level The calculated dream level
     * @return A context for dream generation
     */
    public static DreamContext fromTracker(ProgressSignalTracker tracker, long currentTick, DreamLevel level) {
        // Stall threshold: 50% stalled (about 2.5 hours of game time)
        List<ConceptSignal> stalled = tracker.getStalledSignals(currentTick, 50);
        List<ConceptSignal> partial = tracker.getPartiallyCompleteSignals();
        boolean hasProgress = tracker.hasProgressSinceLastDream();
        long ticksSince = tracker.lastDreamTick() > 0
            ? currentTick - tracker.lastDreamTick()
            : Long.MAX_VALUE;

        return new DreamContext(stalled, partial, level, hasProgress, ticksSince);
    }

    /**
     * Get the primary concept to focus on in the dream.
     * Prioritizes stalled signals, then partially complete.
     */
    public String getPrimaryConcept() {
        if (!stalledSignals.isEmpty()) {
            return stalledSignals.get(0).conceptId();
        }
        if (!partiallyComplete.isEmpty()) {
            return partiallyComplete.get(0).conceptId();
        }
        return null;
    }

    /**
     * Get a secondary concept for additional dream content
     */
    public String getSecondaryConcept() {
        if (stalledSignals.size() > 1) {
            return stalledSignals.get(1).conceptId();
        }
        if (!stalledSignals.isEmpty() && !partiallyComplete.isEmpty()) {
            return partiallyComplete.get(0).conceptId();
        }
        if (partiallyComplete.size() > 1) {
            return partiallyComplete.get(1).conceptId();
        }
        return null;
    }

    /**
     * Check if there's anything meaningful to dream about
     */
    public boolean hasDreamContent() {
        return !stalledSignals.isEmpty() || !partiallyComplete.isEmpty();
    }
}
