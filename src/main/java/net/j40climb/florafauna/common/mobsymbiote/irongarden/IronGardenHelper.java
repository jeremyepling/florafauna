package net.j40climb.florafauna.common.mobsymbiote.irongarden;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.common.mobsymbiote.MobSymbioteData;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.Level;
import net.j40climb.florafauna.common.block.ferricpoppy.FerricPoppyBlock;

import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * Utility methods for the Iron Garden system.
 * Provides static helpers for reading/writing iron garden data and checking eligibility.
 */
public final class IronGardenHelper {

    private IronGardenHelper() {
        // Utility class
    }

    /**
     * Get the iron garden data for an entity.
     * Returns DEFAULT if the entity is not an Iron Golem.
     *
     * @param entity The entity to get data from
     * @return The iron garden data, or DEFAULT if not applicable
     */
    public static IronGardenData getData(Entity entity) {
        if (!(entity instanceof IronGolem)) {
            return IronGardenData.DEFAULT;
        }
        return entity.getData(FloraFaunaRegistry.IRON_GARDEN_DATA);
    }

    /**
     * Set the iron garden data for an Iron Golem.
     *
     * @param golem The golem to set data on
     * @param data The new iron garden data
     */
    public static void setData(IronGolem golem, IronGardenData data) {
        golem.setData(FloraFaunaRegistry.IRON_GARDEN_DATA, data);
    }

    /**
     * Check if an entity is eligible for the iron garden system.
     * Must be an Iron Golem with MobSymbiote Level 1+.
     *
     * @param entity The entity to check
     * @return true if eligible for iron garden
     */
    public static boolean isEligible(Entity entity) {
        if (!(entity instanceof IronGolem)) {
            return false;
        }
        MobSymbioteData symbioteData = entity.getData(FloraFaunaRegistry.MOB_SYMBIOTE_DATA);
        return symbioteData.hasMobSymbiote();
    }

    /**
     * Check if an Iron Golem has been calm (no combat) for the required duration.
     *
     * @param golem The golem to check
     * @param currentTick Current game tick
     * @return true if calm period has elapsed
     */
    public static boolean hasBeenCalmLongEnough(IronGolem golem, long currentTick) {
        IronGardenData data = getData(golem);
        long ticksSinceCombat = data.getTicksSinceCombat(currentTick);
        return ticksSinceCombat >= Config.ironGardenCalmRequiredTicks;
    }

    /**
     * Record a combat event for the golem.
     * If the golem was in a calm state, it transitions to BONDED_NOT_CALM.
     *
     * @param golem The golem that experienced combat
     * @param currentTick Current game tick
     */
    public static void recordCombat(IronGolem golem, long currentTick) {
        IronGardenData data = getData(golem);
        if (data.ironGardenState().isCalm()) {
            // Break calmness
            setData(golem, data.withCombatBreakingCalm(currentTick));
        } else if (data.ironGardenState() == IronGardenState.BONDED_NOT_CALM) {
            // Just update the combat tick
            setData(golem, data.withCombat(currentTick));
        }
        // UNBONDED golems don't track combat for garden purposes
    }

    /**
     * Transition the golem to a new state.
     *
     * @param golem The golem to transition
     * @param newState The new state
     * @param currentTick Current game tick
     */
    public static void transitionState(IronGolem golem, IronGardenState newState, long currentTick) {
        IronGardenData data = getData(golem);
        setData(golem, data.withState(newState, currentTick));
    }

    /**
     * Get the ticks remaining until the golem becomes calm.
     *
     * @param golem The golem to check
     * @param currentTick Current game tick
     * @return Ticks remaining, or 0 if already calm eligible
     */
    public static long getTicksUntilCalm(IronGolem golem, long currentTick) {
        IronGardenData data = getData(golem);
        long ticksSinceCombat = data.getTicksSinceCombat(currentTick);
        long remaining = Config.ironGardenCalmRequiredTicks - ticksSinceCombat;
        return Math.max(0, remaining);
    }

    /**
     * Format ticks as a human-readable time string.
     *
     * @param ticks The number of ticks
     * @return Formatted string like "5m 30s" or "2m 0s"
     */
    public static String formatTicks(long ticks) {
        long seconds = ticks / 20;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        if (minutes > 0) {
            return String.format("%dm %ds", minutes, remainingSeconds);
        }
        return String.format("%ds", remainingSeconds);
    }

    /**
     * Searches for a block position within the golem's garden that matches the given predicate.
     * Searches in an expanding spiral pattern from the golem's position.
     *
     * @param golem The golem to search around
     * @param matcher Predicate that takes (Level, BlockPos) and returns true if the position is a match
     * @return The first matching position, or null if none found
     */
    public static BlockPos findBlockInGarden(IronGolem golem, BiPredicate<Level, BlockPos> matcher) {
        IronGardenData data = getData(golem);
        Optional<BlockPos> centerOpt = data.getGardenCenter();
        if (centerOpt.isEmpty()) {
            return null;
        }

        BlockPos center = centerOpt.get();
        Level level = golem.level();
        int radius = Config.ironGardenWanderRadius;
        BlockPos golemPos = golem.blockPosition();

        // Search in expanding perimeter from golem's position
        // Check multiple Y levels to handle uneven terrain
        for (int r = 1; r <= radius; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    // Only check perimeter of each ring
                    if (Math.abs(x) != r && Math.abs(z) != r) continue;

                    // Check Y levels from -2 to +2 to handle terrain variation
                    for (int y = -2; y <= 2; y++) {
                        BlockPos checkPos = golemPos.offset(x, y, z);

                        // Check if within garden bounds (horizontal distance only)
                        double horizontalDistSq = Math.pow(checkPos.getX() - center.getX(), 2)
                                + Math.pow(checkPos.getZ() - center.getZ(), 2);
                        if (horizontalDistSq > (long) radius * radius) {
                            continue;
                        }

                        if (matcher.test(level, checkPos)) {
                            return checkPos;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Set the current activity for debug display.
     *
     * @param golem The golem to update
     * @param activity The new activity
     */
    public static void setActivity(IronGolem golem, IronGardenActivity activity) {
        IronGardenData data = getData(golem);
        setData(golem, data.withActivity(activity));
    }

    /**
     * Counts the number of ferric poppies within the golem's garden radius.
     *
     * @param golem The golem to search around
     * @return The count of ferric poppies
     */
    public static int countFerricPoppies(IronGolem golem) {
        Level level = golem.level();
        BlockPos golemPos = golem.blockPosition();
        int radius = Config.ironGardenWanderRadius;
        int count = 0;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -2; y <= 2; y++) {
                    BlockPos checkPos = golemPos.offset(x, y, z);
                    if (level.getBlockState(checkPos).getBlock() instanceof FerricPoppyBlock) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
