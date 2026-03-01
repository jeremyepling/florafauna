package net.j40climb.florafauna.common.mobsymbiote.irongarden;

/**
 * State machine states for the Iron Garden system.
 * <p>
 * State transitions:
 * <pre>
 * UNBONDED
 *   | (bonded via MobSymbiote)
 *   v
 * CALM (gardening)
 *   ^
 *   | (taking damage)
 *   v
 * BONDED_NOT_CALM
 *   | (no combat for configured duration)
 *   v
 * CALM
 * </pre>
 * Taking damage breaks calmness -> returns to BONDED_NOT_CALM.
 */
public enum IronGardenState {
    /**
     * Iron Golem has no MobSymbiote attached.
     * No gardening behaviors are active.
     */
    UNBONDED,

    /**
     * Iron Golem has MobSymbiote Level 1+ but has recently been in combat.
     * Waiting for the calm period to elapse before gardening can begin.
     */
    BONDED_NOT_CALM,

    /**
     * Calm golem is actively gardening - planting and harvesting ferric poppies.
     */
    CALM,

    // Legacy states for backwards compatibility with saved data
    /** @deprecated Use CALM instead */
    @Deprecated CALM_PLANTING,
    /** @deprecated Use CALM instead */
    @Deprecated CALM_HARVESTING;

    /**
     * @return true if this state represents an active gardening state
     */
    public boolean isGardening() {
        return isCalm();
    }

    /**
     * @return true if this state is a calm state where the golem can garden
     */
    public boolean isCalm() {
        return this == CALM || this == CALM_PLANTING || this == CALM_HARVESTING;
    }

    /**
     * @return true if the golem is bonded (has MobSymbiote)
     */
    public boolean isBonded() {
        return this != UNBONDED;
    }
}
