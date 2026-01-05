package net.j40climb.florafauna.common.block.mobtransport;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.common.entity.mobsymbiote.MobSymbioteHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * Determines which mobs are eligible for capture by MobInput.
 * <p>
 * Eligibility rules:
 * - Must be a Mob (not Player, not projectile, etc.)
 * - Mobs on the exclusion list (bosses, warden, etc.) are not eligible
 * - Recently released mobs have capture immunity
 * - Mobs without a MobSymbiote require config flag to be captured
 * - Mobs with a MobSymbiote are always eligible (and prioritized)
 */
public final class MobCaptureEligibility {

    private MobCaptureEligibility() {
        // Utility class
    }

    /**
     * Exclusion reasons for capture eligibility.
     */
    public enum ExclusionReason {
        /** Eligible for capture */
        NONE,
        /** Not a Mob entity (Player, projectile, etc.) */
        NOT_MOB,
        /** Is a player entity */
        PLAYER,
        /** Entity is on the MobSymbiote exclusion list (bosses, warden, etc.) */
        MOB_SYMBIOTE_EXCLUDED,
        /** Has capture immunity from recent release */
        RECENTLY_RELEASED,
        /** Config disallows capture without MobSymbiote and mob has none */
        NO_MOB_SYMBIOTE
    }

    /**
     * Checks if an entity is eligible for capture.
     *
     * @param entity The entity to check
     * @param currentTick The current game tick
     * @return EligibilityResult containing the exclusion reason (NONE if eligible)
     */
    public static EligibilityResult checkEligibility(Entity entity, long currentTick) {
        // Never capture players
        if (entity instanceof Player) {
            return new EligibilityResult(ExclusionReason.PLAYER);
        }

        // Must be a Mob
        if (!(entity instanceof Mob mob)) {
            return new EligibilityResult(ExclusionReason.NOT_MOB);
        }

        // Check exclusion list (bosses, warden, elder guardian, etc.)
        if (MobSymbioteHelper.isMobSymbioteExcluded(mob)) {
            return new EligibilityResult(ExclusionReason.MOB_SYMBIOTE_EXCLUDED);
        }

        // Check release immunity
        if (MobSymbioteHelper.hasReleaseImmunity(entity, currentTick)) {
            return new EligibilityResult(ExclusionReason.RECENTLY_RELEASED);
        }

        // Check MobSymbiote status
        boolean hasMobSymbiote = MobSymbioteHelper.hasMobSymbiote(mob);
        if (!hasMobSymbiote && !Config.allowUnbondedCapture) {
            return new EligibilityResult(ExclusionReason.NO_MOB_SYMBIOTE);
        }

        return new EligibilityResult(ExclusionReason.NONE);
    }

    /**
     * Returns true if the entity is eligible for capture.
     *
     * @param entity The entity to check
     * @param currentTick The current game tick
     * @return true if eligible
     */
    public static boolean isEligible(Entity entity, long currentTick) {
        return checkEligibility(entity, currentTick).isEligible();
    }

    /**
     * Returns true if this mob has a MobSymbiote (higher capture priority).
     *
     * @param entity The entity to check
     * @return true if has a MobSymbiote attached
     */
    public static boolean hasMobSymbiote(Entity entity) {
        return MobSymbioteHelper.hasMobSymbiote(entity);
    }

    /**
     * Result of an eligibility check including whether the entity is eligible
     * and the reason if not.
     *
     * @param reason The exclusion reason (NONE if eligible)
     */
    public record EligibilityResult(ExclusionReason reason) {
        public boolean isEligible() {
            return reason == ExclusionReason.NONE;
        }
    }
}
