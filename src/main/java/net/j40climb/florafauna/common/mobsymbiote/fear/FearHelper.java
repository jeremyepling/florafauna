package net.j40climb.florafauna.common.entity.fear;

import net.j40climb.florafauna.common.entity.mobsymbiote.MobSymbioteHelper;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.j40climb.florafauna.setup.FloraFaunaTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * Utility methods for working with the fear/stress system.
 */
public final class FearHelper {

    private FearHelper() {
        // Utility class
    }

    // ==================== FEAR STATE ACCESS ====================

    /**
     * Gets the current fear state for a mob.
     *
     * @param entity The entity to check
     * @return The current fear state, or CALM if not a mob
     */
    public static FearState getFearState(Entity entity) {
        if (!(entity instanceof Mob)) {
            return FearState.CALM;
        }
        FearData data = entity.getData(FloraFaunaRegistry.FEAR_DATA);
        return data.fearState();
    }

    /**
     * Gets the full FearData for a mob.
     *
     * @param entity The entity to check
     * @return The FearData, or DEFAULT if not a mob
     */
    public static FearData getFearData(Entity entity) {
        if (!(entity instanceof Mob)) {
            return FearData.DEFAULT;
        }
        return entity.getData(FloraFaunaRegistry.FEAR_DATA);
    }

    /**
     * Transitions a mob to a new fear state.
     *
     * @param mob       The mob to update
     * @param newState  The new fear state
     * @param currentTick The current game tick
     */
    public static void setFearState(Mob mob, FearState newState, long currentTick) {
        FearData current = mob.getData(FloraFaunaRegistry.FEAR_DATA);
        mob.setData(FloraFaunaRegistry.FEAR_DATA, current.withState(newState, currentTick));
    }

    /**
     * Gets how many ticks the mob has been in its current state.
     *
     * @param entity      The entity to check
     * @param currentTick The current game tick
     * @return Ticks in current state
     */
    public static long getTicksInState(Entity entity, long currentTick) {
        if (!(entity instanceof Mob)) {
            return 0;
        }
        FearData data = entity.getData(FloraFaunaRegistry.FEAR_DATA);
        return data.getTicksInState(currentTick);
    }

    // ==================== ELIGIBILITY ====================

    /**
     * Checks if a mob can experience fear.
     * Requirements:
     * 1. Must have MobSymbiote Level 1+
     * 2. Must be in the FEARFUL_MOBS tag
     *
     * @param entity The entity to check
     * @return true if the entity can experience fear
     */
    public static boolean canExperienceFear(Entity entity) {
        if (!(entity instanceof Mob mob)) {
            return false;
        }

        // Must have MobSymbiote
        if (!MobSymbioteHelper.hasMobSymbiote(mob)) {
            return false;
        }

        // Must be in FEARFUL_MOBS tag
        return mob.getType().is(FloraFaunaTags.EntityTypes.FEARFUL_MOBS);
    }

    // ==================== LEAK COUNT MANAGEMENT ====================

    /**
     * Increments the consecutive leak count.
     * Used to track progress toward OVERSTRESS.
     *
     * @param mob The mob that leaked
     */
    public static void incrementLeakCount(Mob mob) {
        FearData current = mob.getData(FloraFaunaRegistry.FEAR_DATA);
        mob.setData(FloraFaunaRegistry.FEAR_DATA, current.incrementLeakCount());
    }

    /**
     * Resets the consecutive leak count.
     * Called when a mob returns to CALM naturally (fear source removed).
     *
     * @param mob The mob to reset
     */
    public static void resetLeakCount(Mob mob) {
        FearData current = mob.getData(FloraFaunaRegistry.FEAR_DATA);
        mob.setData(FloraFaunaRegistry.FEAR_DATA, current.resetLeakCount());
    }

    /**
     * Gets the current consecutive leak count.
     *
     * @param entity The entity to check
     * @return The leak count
     */
    public static int getLeakCount(Entity entity) {
        if (!(entity instanceof Mob)) {
            return 0;
        }
        FearData data = entity.getData(FloraFaunaRegistry.FEAR_DATA);
        return data.leakCountSinceCooldown();
    }

    // ==================== FEAR SOURCE POSITION ====================

    /**
     * Sets the fear source position for avoidance direction.
     *
     * @param mob The mob experiencing fear
     * @param pos The position of the fear source
     */
    public static void setFearSourcePos(Mob mob, BlockPos pos) {
        FearData current = mob.getData(FloraFaunaRegistry.FEAR_DATA);
        mob.setData(FloraFaunaRegistry.FEAR_DATA, current.withFearSourcePos(pos));
    }

    /**
     * Clears the fear source position.
     *
     * @param mob The mob to update
     */
    public static void clearFearSourcePos(Mob mob) {
        FearData current = mob.getData(FloraFaunaRegistry.FEAR_DATA);
        mob.setData(FloraFaunaRegistry.FEAR_DATA, current.withoutFearSourcePos());
    }

    /**
     * Resets a mob to the CALM state with all fear data cleared.
     *
     * @param mob         The mob to reset
     * @param currentTick The current game tick
     * @param resetLeaks  If true, also reset the leak count
     */
    public static void resetToCalm(Mob mob, long currentTick, boolean resetLeaks) {
        FearData current = mob.getData(FloraFaunaRegistry.FEAR_DATA);
        FearData updated = current.withState(FearState.CALM, currentTick)
                .withoutFearSourcePos();
        if (resetLeaks) {
            updated = updated.resetLeakCount();
        }
        mob.setData(FloraFaunaRegistry.FEAR_DATA, updated);
    }
}
