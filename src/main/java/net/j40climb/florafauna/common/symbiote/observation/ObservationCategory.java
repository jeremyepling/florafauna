package net.j40climb.florafauna.common.symbiote.observation;

/**
 * Categories of observations for dampening purposes.
 * Each category has independent dampening timers to prevent
 * the same type of observation from triggering repeatedly.
 */
public enum ObservationCategory {
    /**
     * Environmental hazards: cold water, lava, drowning, suffocation
     */
    ENVIRONMENTAL_HAZARD("environmental"),

    /**
     * Combat damage: mob attacks, fall damage from combat
     */
    COMBAT_DAMAGE("combat"),

    /**
     * Fall damage (non-combat related)
     */
    FALL_DAMAGE("fall"),

    /**
     * Player state issues: hunger, low health, negative effects
     */
    PLAYER_STATE("state"),

    /**
     * Sleep-related observations
     */
    SLEEP("sleep"),

    /**
     * Bonding milestones: initial bond, tier up, etc.
     * These are typically Tier 2 events.
     */
    BONDING_MILESTONE("bonding"),

    /**
     * Mining anchor events: fill warnings, capacity full
     */
    MINING_ANCHOR("anchor");

    private final String key;

    ObservationCategory(String key) {
        this.key = key;
    }

    /**
     * Get the key used for cooldown tracking and line selection
     */
    public String getKey() {
        return key;
    }
}
