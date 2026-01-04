package net.j40climb.florafauna.common.block.vacuum;

import net.minecraft.util.StringRepresentable;

/**
 * Visual states for vacuum-type blocks (ItemInput, MiningAnchor, etc.).
 * Used as a block state property for model variants.
 *
 * NORMAL: Idle, waiting for items to collect.
 * WORKING: Actively collecting or processing items.
 * BLOCKED: Buffer is full or unable to process.
 */
public enum VacuumState implements StringRepresentable {
    NORMAL("normal"),
    WORKING("working"),
    BLOCKED("blocked");

    private final String name;

    VacuumState(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /**
     * Returns true if this state indicates the block is actively processing items.
     */
    public boolean isActive() {
        return this == WORKING;
    }

    /**
     * Returns true if this state indicates a problem (buffer full, unable to process).
     */
    public boolean hasError() {
        return this == BLOCKED;
    }
}
