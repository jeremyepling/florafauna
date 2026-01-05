package net.j40climb.florafauna.common.block.mobtransport;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * AI Goal that makes a mob pathfind toward a MobInput block.
 * The mob will walk naturally toward the block, facing the direction it's moving.
 * <p>
 * This goal is added dynamically when a bonded mob enters the lure radius
 * and removed when captured, leaves the area, or the block is removed.
 */
public class LuredToBlockGoal extends Goal {
    private final Mob mob;
    private final BlockPos targetPos;
    private final double speedModifier;

    // How close the mob needs to get before we consider it "arrived"
    private static final double ARRIVAL_DISTANCE_SQ = 2.0 * 2.0;

    // How often to recalculate path (in ticks)
    private int pathRecalcCooldown = 0;
    private static final int PATH_RECALC_INTERVAL = 20;

    /**
     * Creates a new lure goal.
     *
     * @param mob The mob being lured
     * @param targetPos The MobInput block position
     * @param speedModifier Speed multiplier (1.0 = normal speed)
     */
    public LuredToBlockGoal(Mob mob, BlockPos targetPos, double speedModifier) {
        this.mob = mob;
        this.targetPos = targetPos;
        this.speedModifier = speedModifier;

        // This goal controls movement and looking
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public LuredToBlockGoal(Mob mob, BlockPos targetPos) {
        this(mob, targetPos, 1.0);
    }

    @Override
    public boolean canUse() {
        // Goal can be used as long as we're not at the target yet
        double distSq = mob.blockPosition().distSqr(targetPos);
        return distSq > ARRIVAL_DISTANCE_SQ;
    }

    @Override
    public boolean canContinueToUse() {
        // Continue until we reach the target
        double distSq = mob.blockPosition().distSqr(targetPos);
        return distSq > ARRIVAL_DISTANCE_SQ && !mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        // Start pathfinding to the block
        moveTo();
    }

    @Override
    public void tick() {
        // Look at the target
        mob.getLookControl().setLookAt(
                targetPos.getX() + 0.5,
                targetPos.getY() + 0.5,
                targetPos.getZ() + 0.5
        );

        // Recalculate path periodically in case of obstacles
        pathRecalcCooldown--;
        if (pathRecalcCooldown <= 0) {
            pathRecalcCooldown = PATH_RECALC_INTERVAL;
            moveTo();
        }
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }

    private void moveTo() {
        mob.getNavigation().moveTo(
                targetPos.getX() + 0.5,
                targetPos.getY(),
                targetPos.getZ() + 0.5,
                speedModifier
        );
    }

    /**
     * Gets the target block position for this goal.
     */
    public BlockPos getTargetPos() {
        return targetPos;
    }

    /**
     * Checks if this goal is targeting the specified position.
     */
    public boolean isTargeting(BlockPos pos) {
        return targetPos.equals(pos);
    }
}
