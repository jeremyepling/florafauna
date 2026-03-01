package net.j40climb.florafauna.common.mobsymbiote.irongarden;

/**
 * Current activity of a gardening Iron Golem.
 * This syncs to the client via IronGardenData for debug display.
 */
public enum IronGardenActivity {
    IDLE("Idle"),
    WANDERING("Wandering"),
    PLANTING("Planting"),
    HARVESTING("Harvesting"),
    DEPOSITING("Depositing");

    private final String displayName;

    IronGardenActivity(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
