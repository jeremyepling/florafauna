package net.j40climb.florafauna.common.symbiote.dream;

/**
 * Escalation levels for dream messages.
 * Dreams become clearer with repeated requests when no progress is made.
 */
public enum DreamLevel {
    /**
     * First dream or after long gap - reflective, vague, poetic
     */
    L1_REFLECTIVE(0x9E9E9E, "reflective"),  // Gray, vague

    /**
     * Second dream soon after first with no progress - directional hints
     */
    L2_DIRECTIONAL(0xB39DDB, "directional"),  // Purple, clearer

    /**
     * Third dream, still stuck - anchored, specific but non-spoilery
     */
    L3_ANCHORED(0xE1BEE7, "anchored");  // Light purple, vivid

    private final int color;
    private final String lineCategory;

    DreamLevel(int color, String lineCategory) {
        this.color = color;
        this.lineCategory = lineCategory;
    }

    /**
     * Get the text color for this dream level
     */
    public int getColor() {
        return color;
    }

    /**
     * Get the line category suffix for selecting dream lines
     * e.g., "dream_reflective", "dream_directional", "dream_anchored"
     */
    public String getLineCategory() {
        return "dream_" + lineCategory;
    }

    /**
     * Get dream level from ordinal (0-2), clamped to valid range
     */
    public static DreamLevel fromOrdinal(int ordinal) {
        if (ordinal <= 0) return L1_REFLECTIVE;
        if (ordinal >= 2) return L3_ANCHORED;
        return L2_DIRECTIONAL;
    }
}
