package net.j40climb.florafauna.common.symbiote.dialogue;

import net.j40climb.florafauna.common.symbiote.observation.ObservationCategory;
import net.j40climb.florafauna.common.symbiote.progress.ProgressSignalTracker;
import net.j40climb.florafauna.common.symbiote.voice.VoiceTier;

import java.util.Map;

/**
 * Context for selecting appropriate dialogue lines.
 * Contains all the information needed to filter and select lines from the repository.
 */
public record DialogueSelectionContext(
    VoiceTier tier,
    ObservationCategory category,
    int severity,
    Map<String, Object> additionalContext,
    ProgressSignalTracker progressTracker
) {
    /**
     * Create a simple context without additional data
     */
    public static DialogueSelectionContext simple(
            VoiceTier tier,
            ObservationCategory category,
            int severity,
            ProgressSignalTracker progressTracker
    ) {
        return new DialogueSelectionContext(tier, category, severity, Map.of(), progressTracker);
    }

    /**
     * Get a context value by key, with type casting
     */
    @SuppressWarnings("unchecked")
    public <T> T getContextValue(String key, T defaultValue) {
        Object value = additionalContext.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * Check if severity is within a range
     */
    public boolean severityInRange(int min, int max) {
        return severity >= min && severity <= max;
    }
}
