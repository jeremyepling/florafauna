package net.j40climb.florafauna.common.symbiote.progress;

/**
 * State machine for tracking progress on concepts/features.
 * Represents how familiar the symbiote is with a particular concept.
 */
public enum SignalState {
    /**
     * Never encountered this concept
     */
    UNSEEN(0),

    /**
     * Observed once - knows it exists
     */
    SEEN(1),

    /**
     * Interacted with multiple times
     */
    TOUCHED(2),

    /**
     * Consistent engagement - understands it
     */
    STABILIZED(3),

    /**
     * Fully incorporated into workflow
     */
    INTEGRATED(4),

    /**
     * Stalled too long - needs attention
     */
    NEGLECTED(-1);

    private final int progressLevel;

    SignalState(int progressLevel) {
        this.progressLevel = progressLevel;
    }

    public int getProgressLevel() {
        return progressLevel;
    }

    /**
     * Check if this state can transition to the target state.
     * Generally follows UNSEEN -> SEEN -> TOUCHED -> STABILIZED -> INTEGRATED
     * NEGLECTED can recover back to TOUCHED.
     */
    public boolean canTransitionTo(SignalState target) {
        // Can always stay in same state
        if (this == target) return true;

        // NEGLECTED can recover to TOUCHED
        if (this == NEGLECTED && target == TOUCHED) return true;

        // Any positive state can transition to NEGLECTED
        if (target == NEGLECTED && this.progressLevel > 0 && this != INTEGRATED) return true;

        // Normal forward progression
        return target.progressLevel == this.progressLevel + 1;
    }

    /**
     * Get the next state in normal progression
     */
    public SignalState getNextState() {
        return switch (this) {
            case UNSEEN -> SEEN;
            case SEEN -> TOUCHED;
            case TOUCHED -> STABILIZED;
            case STABILIZED -> INTEGRATED;
            case INTEGRATED -> INTEGRATED; // Terminal state
            case NEGLECTED -> TOUCHED; // Recovery
        };
    }
}
