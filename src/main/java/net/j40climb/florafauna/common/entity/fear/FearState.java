package net.j40climb.florafauna.common.entity.fear;

/**
 * Fear states for the stress/fear ecosystem.
 * States are binary - a mob is either in a state or not.
 * No gradual ramping or decay.
 */
public enum FearState {
    /**
     * Normal AI, no output.
     * Exit: Fear source detected → PANICKED
     */
    CALM,

    /**
     * Mob shakes visibly, hissing/breathing sounds.
     * Particles appear (gunpowder puffs for creepers).
     * Fuse is suppressed (for creepers).
     * Exit (up): Duration threshold → LEAK
     * Exit (down): Fear source removed → CALM
     */
    PANICKED,

    /**
     * Output event - drops items (gunpowder for creepers).
     * Mob remains alive.
     * Exit: Immediate → EXHAUSTED
     */
    LEAK,

    /**
     * Cooldown state - temporarily immune to fear.
     * Normal AI, sluggish movement.
     * Exit: Cooldown expires → CALM
     */
    EXHAUSTED,

    /**
     * Failure state - mob explodes (for creepers).
     * Entry: Multiple consecutive LEAKs without CALM reset.
     * Terminal state.
     */
    OVERSTRESS;

    /**
     * @return true if this state suppresses creeper fuse
     */
    public boolean suppressesFuse() {
        return this == PANICKED;
    }

    /**
     * @return true if this state is immune to new fear triggers
     */
    public boolean isImmuneToFear() {
        return this == EXHAUSTED || this == OVERSTRESS;
    }

    /**
     * @return true if this is a scared state (backing away, particles, etc.)
     */
    public boolean isScared() {
        return this == PANICKED;
    }
}
