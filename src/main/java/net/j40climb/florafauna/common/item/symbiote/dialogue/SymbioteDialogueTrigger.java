package net.j40climb.florafauna.common.item.symbiote.dialogue;

/**
 * Enum defining all dialogue triggers for the symbiote.
 * Each trigger has:
 * - A unique key for tracking first-time events
 * - A localization key for the message
 * - A flag indicating if it should only trigger once
 */
public enum SymbioteDialogueTrigger {
    // Bonding events (always trigger, not tracked)
    BONDED("bonded", "symbiote.dialogue.bonded", false),
    UNBONDED("unbonded", "symbiote.dialogue.unbonded", false),

    // Environmental/damage events (first-time only)
    COLD_WATER("cold_water", "symbiote.dialogue.cold_water", true),
    FALL_DAMAGE("fall_damage", "symbiote.dialogue.fall_damage", true),
    DROWNING("drowning", "symbiote.dialogue.drowning", true),
    MOB_ATTACK("mob_attack", "symbiote.dialogue.mob_attack", true),
    SLEEPING("sleeping", "symbiote.dialogue.sleeping", true);

    private final String key;
    private final String localizationKey;
    private final boolean firstTimeOnly;

    SymbioteDialogueTrigger(String key, String localizationKey, boolean firstTimeOnly) {
        this.key = key;
        this.localizationKey = localizationKey;
        this.firstTimeOnly = firstTimeOnly;
    }

    public String getKey() {
        return key;
    }

    public String getLocalizationKey() {
        return localizationKey;
    }

    public boolean isFirstTimeOnly() {
        return firstTimeOnly;
    }
}
