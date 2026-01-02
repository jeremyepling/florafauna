package net.j40climb.florafauna.common.block.husk;

import net.minecraft.util.StringRepresentable;

/**
 * Types of symbiotic husks created on player death.
 *
 * RESTORATION: Created when dying while BONDED_ACTIVE.
 *              Stores items and restores symbiote abilities on interaction.
 *              Tracked via locator bar and emits particles.
 *
 * CONTAINER: Created when dying while BONDED_WEAKENED.
 *            Stores items only. No restoration or tracking.
 *
 * BROKEN: Inert visual remnant after all items retrieved.
 *         Persists forever as a marker.
 */
public enum HuskType implements StringRepresentable {
    RESTORATION("restoration"),
    CONTAINER("container"),
    BROKEN("broken");

    private final String name;

    HuskType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /**
     * Returns true if this husk type can hold items.
     */
    public boolean holdsItems() {
        return this != BROKEN;
    }

    /**
     * Returns true if interacting with this husk restores symbiote abilities.
     */
    public boolean restoresAbilities() {
        return this == RESTORATION;
    }
}
