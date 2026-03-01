package net.j40climb.florafauna.common.block.mobtransport;

import net.minecraft.util.StringRepresentable;

/**
 * Visual states for the MobInput block.
 * Used as a block state property for model variants.
 * <p>
 * IDLE: Default, waiting for mobs to lure.
 * OPEN: Mouth open, actively luring nearby mobs.
 * CHOMPING: Capture animation in progress.
 */
public enum MobInputState implements StringRepresentable {
    IDLE("idle"),
    OPEN("open"),
    CHOMPING("chomping");

    private final String name;

    MobInputState(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /**
     * Returns true if the block is actively engaging with mobs.
     */
    public boolean isActive() {
        return this == OPEN || this == CHOMPING;
    }

    /**
     * Returns true if the block is in the capture animation.
     */
    public boolean isCapturing() {
        return this == CHOMPING;
    }
}
