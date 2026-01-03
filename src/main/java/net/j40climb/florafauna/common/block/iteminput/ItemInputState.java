package net.j40climb.florafauna.common.block.iteminput;

import net.minecraft.util.StringRepresentable;

/**
 * Visual states for item input blocks.
 * Used as a block state property for model variants.
 *
 * NORMAL: Idle, waiting for items to collect.
 * WORKING: Actively collecting or transferring items.
 * BLOCKED: Buffer is full or storage is unavailable.
 */
public enum ItemInputState implements StringRepresentable {
    NORMAL("normal"),
    WORKING("working"),
    BLOCKED("blocked");

    private final String name;

    ItemInputState(String name) {
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
     * Returns true if this state indicates a problem (buffer full, no storage).
     */
    public boolean hasError() {
        return this == BLOCKED;
    }
}
