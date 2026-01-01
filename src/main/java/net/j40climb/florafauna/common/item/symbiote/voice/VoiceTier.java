package net.j40climb.florafauna.common.item.symbiote.voice;

/**
 * Defines the two tiers of symbiote voice output with their cooldown configurations.
 *
 * Tier 1 (Ambient): Rare observational comments, rate-limited
 * Tier 2 (Breakthrough): Memorable milestone moments with long lockouts
 */
public enum VoiceTier {
    /**
     * Ambient observations - deadpan, short, rate-limited.
     * Global cooldown: 5 minutes
     * Category dampening: 1 minute
     */
    TIER_1_AMBIENT(
        6000,     // 5 minutes global cooldown (in ticks, 20 ticks/sec)
        1200,     // 1 minute category dampening
        false     // no post-lockout
    ),

    /**
     * Breakthrough moments - memorable, longer messages.
     * Global cooldown: 30 minutes
     * Category lockout: 1 hour
     * Triggers post-tier-2 silence window for Tier 1
     */
    TIER_2_BREAKTHROUGH(
        36000,    // 30 minutes global cooldown
        72000,    // 1 hour category lockout
        true      // triggers post-tier-2 silence window
    );

    private final int globalCooldownTicks;
    private final int categoryDampeningTicks;
    private final boolean triggersLockout;

    VoiceTier(int globalCooldownTicks, int categoryDampeningTicks, boolean triggersLockout) {
        this.globalCooldownTicks = globalCooldownTicks;
        this.categoryDampeningTicks = categoryDampeningTicks;
        this.triggersLockout = triggersLockout;
    }

    public int getGlobalCooldownTicks() {
        return globalCooldownTicks;
    }

    public int getCategoryDampeningTicks() {
        return categoryDampeningTicks;
    }

    public boolean triggersLockout() {
        return triggersLockout;
    }

    /**
     * Duration of post-tier-2 lockout for Tier 1 messages (15 minutes)
     */
    public static final int POST_TIER2_LOCKOUT_TICKS = 18000;
}
