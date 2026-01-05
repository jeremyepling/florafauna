package net.j40climb.florafauna.common.entity.mobsymbiote;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.common.block.mobtransport.LuredToBlockGoal;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.j40climb.florafauna.setup.FloraFaunaTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.jetbrains.annotations.Nullable;

/**
 * Utility methods for working with MobSymbiote data.
 */
public final class MobSymbioteHelper {

    private MobSymbioteHelper() {
        // Utility class
    }

    // ==================== MOB SYMBIOTE STATE ====================

    /**
     * Checks if a mob has a MobSymbiote (level >= 1).
     *
     * @param entity The entity to check
     * @return true if the entity has a MobSymbiote attached
     */
    public static boolean hasMobSymbiote(Entity entity) {
        if (!(entity instanceof Mob)) {
            return false;
        }
        MobSymbioteData data = entity.getData(FloraFaunaRegistry.MOB_SYMBIOTE_DATA);
        return data.hasMobSymbiote();
    }

    /**
     * Gets the MobSymbiote level of a mob.
     *
     * @param entity The entity to check
     * @return The MobSymbiote level (0 if no symbiote or not a mob)
     */
    public static int getMobSymbioteLevel(Entity entity) {
        if (!(entity instanceof Mob)) {
            return MobSymbioteData.LEVEL_NONE;
        }
        MobSymbioteData data = entity.getData(FloraFaunaRegistry.MOB_SYMBIOTE_DATA);
        return data.mobSymbioteLevel();
    }

    /**
     * Applies a Level 1 MobSymbiote to a mob.
     * Level 1 enables luring and transport behavior.
     *
     * @param mob The mob to apply the symbiote to
     * @param currentTick The current game tick
     */
    public static void applyMobSymbioteLevel1(Mob mob, long currentTick) {
        MobSymbioteData current = mob.getData(FloraFaunaRegistry.MOB_SYMBIOTE_DATA);
        mob.setData(FloraFaunaRegistry.MOB_SYMBIOTE_DATA,
                current.withMobSymbioteLevel(MobSymbioteData.LEVEL_TRANSPORT, currentTick));
    }

    /**
     * Applies a Level 2 MobSymbiote to a mob.
     * Level 2 enables enhanced behaviors (future feature).
     * Only mobs in the MOB_SYMBIOTE_LEVEL2_ELIGIBLE tag can receive Level 2.
     *
     * @param mob The mob to upgrade
     * @param currentTick The current game tick
     * @return true if upgrade succeeded, false if mob is not eligible
     */
    public static boolean applyMobSymbioteLevel2(Mob mob, long currentTick) {
        if (!mob.getType().is(FloraFaunaTags.EntityTypes.MOB_SYMBIOTE_LEVEL2_ELIGIBLE)) {
            return false;
        }
        MobSymbioteData current = mob.getData(FloraFaunaRegistry.MOB_SYMBIOTE_DATA);
        mob.setData(FloraFaunaRegistry.MOB_SYMBIOTE_DATA,
                current.withMobSymbioteLevel(MobSymbioteData.LEVEL_ENHANCED, currentTick));
        return true;
    }

    /**
     * Removes the MobSymbiote from a mob (sets level to 0).
     *
     * @param mob The mob to remove the symbiote from
     */
    public static void removeMobSymbiote(Mob mob) {
        MobSymbioteData current = mob.getData(FloraFaunaRegistry.MOB_SYMBIOTE_DATA);
        mob.setData(FloraFaunaRegistry.MOB_SYMBIOTE_DATA,
                current.withMobSymbioteLevel(MobSymbioteData.LEVEL_NONE, 0L));
    }

    /**
     * Checks if a mob is on the exclusion list (cannot receive any MobSymbiote).
     *
     * @param mob The mob to check
     * @return true if the mob cannot receive a MobSymbiote
     */
    public static boolean isMobSymbioteExcluded(Mob mob) {
        return mob.getType().is(FloraFaunaTags.EntityTypes.MOB_SYMBIOTE_EXCLUDED);
    }

    // ==================== RELEASE IMMUNITY ====================

    /**
     * Marks a mob as recently released, granting capture immunity.
     *
     * @param mob The mob to mark
     * @param currentTick The current game tick
     */
    public static void markRecentlyReleased(Mob mob, long currentTick) {
        MobSymbioteData current = mob.getData(FloraFaunaRegistry.MOB_SYMBIOTE_DATA);
        long immunityUntil = currentTick + Config.recentlyReleasedImmunityTicks;
        mob.setData(FloraFaunaRegistry.MOB_SYMBIOTE_DATA, current.withRecentlyReleased(immunityUntil));
    }

    /**
     * Checks if a mob has release immunity (cannot be captured).
     *
     * @param entity The entity to check
     * @param currentTick The current game tick
     * @return true if the entity has capture immunity
     */
    public static boolean hasReleaseImmunity(Entity entity, long currentTick) {
        if (!(entity instanceof Mob)) {
            return false;
        }
        MobSymbioteData data = entity.getData(FloraFaunaRegistry.MOB_SYMBIOTE_DATA);
        return data.hasReleaseImmunity(currentTick);
    }

    // ==================== LURE GOAL MANAGEMENT ====================

    /**
     * Priority for the lure goal. Lower = higher priority.
     * Set to 1 to override most other goals but not panic/flee.
     */
    private static final int LURE_GOAL_PRIORITY = 1;

    /**
     * Starts luring a mob with a MobSymbiote toward a MobInput block.
     * Adds a LuredToBlockGoal to the mob's AI.
     *
     * @param mob The mob to lure (must have a MobSymbiote)
     * @param targetPos The MobInput block position
     * @return true if the goal was added, false if mob has no MobSymbiote or already lured
     */
    public static boolean startLuring(Mob mob, BlockPos targetPos) {
        // Only mobs with a MobSymbiote can be lured
        if (!hasMobSymbiote(mob)) {
            return false;
        }

        // Check if already being lured to this block
        LuredToBlockGoal existingGoal = getLureGoal(mob);
        if (existingGoal != null) {
            if (existingGoal.isTargeting(targetPos)) {
                return false; // Already lured to this block
            }
            // Lured to different block - remove old goal first
            stopLuring(mob);
        }

        // Add the lure goal
        LuredToBlockGoal goal = new LuredToBlockGoal(mob, targetPos);
        mob.goalSelector.addGoal(LURE_GOAL_PRIORITY, goal);
        return true;
    }

    /**
     * Stops luring a mob by removing its LuredToBlockGoal.
     *
     * @param mob The mob to stop luring
     * @return true if a goal was removed
     */
    public static boolean stopLuring(Mob mob) {
        LuredToBlockGoal goal = getLureGoal(mob);
        if (goal != null) {
            mob.goalSelector.removeGoal(goal);
            return true;
        }
        return false;
    }

    /**
     * Checks if a mob is currently being lured by any MobInput.
     *
     * @param mob The mob to check
     * @return true if the mob has a LuredToBlockGoal
     */
    public static boolean isBeingLured(Mob mob) {
        return getLureGoal(mob) != null;
    }

    /**
     * Checks if a mob is being lured to a specific block position.
     *
     * @param mob The mob to check
     * @param targetPos The block position to check
     * @return true if the mob is being lured to this specific block
     */
    public static boolean isBeingLuredTo(Mob mob, BlockPos targetPos) {
        LuredToBlockGoal goal = getLureGoal(mob);
        return goal != null && goal.isTargeting(targetPos);
    }

    /**
     * Gets the position the mob is being lured to, if any.
     *
     * @param mob The mob to check
     * @return The target block position, or null if not being lured
     */
    @Nullable
    public static BlockPos getLureTarget(Mob mob) {
        LuredToBlockGoal goal = getLureGoal(mob);
        return goal != null ? goal.getTargetPos() : null;
    }

    /**
     * Finds the LuredToBlockGoal in a mob's goal selector.
     *
     * @param mob The mob to check
     * @return The LuredToBlockGoal, or null if not found
     */
    @Nullable
    private static LuredToBlockGoal getLureGoal(Mob mob) {
        for (WrappedGoal wrappedGoal : mob.goalSelector.getAvailableGoals()) {
            if (wrappedGoal.getGoal() instanceof LuredToBlockGoal lureGoal) {
                return lureGoal;
            }
        }
        return null;
    }
}
