package net.j40climb.florafauna.common.entity.fear.goals;

import net.j40climb.florafauna.common.entity.fear.FearData;
import net.j40climb.florafauna.common.entity.fear.FearHelper;
import net.j40climb.florafauna.common.entity.fear.FearState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Random;

/**
 * AI Goal that makes a fearful mob back away from the fear source.
 * Active when mob is in PANICKED state.
 * Includes jittery movement for visual effect.
 */
public class FearAvoidanceGoal extends Goal {

    private static final Random RANDOM = new Random();

    private final Mob mob;
    private final double speedModifier;
    private final double retreatDistance;

    // Current retreat target
    private Vec3 retreatTarget;

    // How often to recalculate path (in ticks)
    private int pathRecalcCooldown = 0;
    private static final int PATH_RECALC_INTERVAL = 10;

    // Jitter parameters
    private static final double JITTER_STRENGTH = 0.3;

    /**
     * Creates a new fear avoidance goal.
     *
     * @param mob             The mob experiencing fear
     * @param speedModifier   Speed multiplier (1.0 = normal speed)
     * @param retreatDistance How far to try to get from fear source
     */
    public FearAvoidanceGoal(Mob mob, double speedModifier, double retreatDistance) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.retreatDistance = retreatDistance;

        // This goal controls movement
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    public FearAvoidanceGoal(Mob mob) {
        this(mob, 1.2, 10.0);  // Slightly faster than normal, retreat 10 blocks
    }

    @Override
    public boolean canUse() {
        // Only active when scared (PANICKED state)
        FearState state = FearHelper.getFearState(mob);
        if (!state.isScared()) {
            return false;
        }

        // Need a fear source position to retreat from
        FearData data = FearHelper.getFearData(mob);
        return data.hasFearSourcePos();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        calculateRetreatTarget();
        moveToTarget();
    }

    @Override
    public void tick() {
        // Recalculate retreat target periodically
        pathRecalcCooldown--;
        if (pathRecalcCooldown <= 0) {
            pathRecalcCooldown = PATH_RECALC_INTERVAL;
            calculateRetreatTarget();
            moveToTarget();
        }

        // Apply jitter effect (small random velocity nudges)
        if (RANDOM.nextFloat() < 0.2f) {  // 20% chance per tick
            applyJitter();
        }
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        retreatTarget = null;
    }

    /**
     * Calculates a position away from the fear source.
     * Adds slight randomness for more natural movement.
     */
    private void calculateRetreatTarget() {
        Optional<BlockPos> fearSourceOpt = FearHelper.getFearData(mob).getFearSourcePos();
        if (fearSourceOpt.isEmpty()) {
            retreatTarget = null;
            return;
        }

        BlockPos fearSource = fearSourceOpt.get();
        Vec3 mobPos = mob.position();
        Vec3 sourcePos = Vec3.atCenterOf(fearSource);

        // Calculate direction away from fear source
        Vec3 awayDir = mobPos.subtract(sourcePos).normalize();

        // If mob is too close to source, just pick opposite direction
        if (awayDir.lengthSqr() < 0.1) {
            // Random escape direction
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            awayDir = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        }

        // Add randomness to direction (jittery retreat)
        double randomAngle = (RANDOM.nextDouble() - 0.5) * Math.PI * 0.5;  // +/- 45 degrees
        awayDir = rotateY(awayDir, randomAngle);

        // Calculate target position
        retreatTarget = mobPos.add(awayDir.scale(retreatDistance));
    }

    /**
     * Moves the mob toward the retreat target.
     */
    private void moveToTarget() {
        if (retreatTarget == null) {
            return;
        }

        mob.getNavigation().moveTo(
                retreatTarget.x,
                retreatTarget.y,
                retreatTarget.z,
                speedModifier
        );
    }

    /**
     * Applies a small random velocity nudge for jittery movement.
     */
    private void applyJitter() {
        Vec3 currentVel = mob.getDeltaMovement();
        double jitterX = (RANDOM.nextDouble() - 0.5) * JITTER_STRENGTH;
        double jitterZ = (RANDOM.nextDouble() - 0.5) * JITTER_STRENGTH;

        mob.setDeltaMovement(
                currentVel.x + jitterX,
                currentVel.y,
                currentVel.z + jitterZ
        );
    }

    /**
     * Rotates a vector around the Y axis.
     *
     * @param vec   The vector to rotate
     * @param angle Angle in radians
     * @return Rotated vector
     */
    private Vec3 rotateY(Vec3 vec, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(
                vec.x * cos - vec.z * sin,
                vec.y,
                vec.x * sin + vec.z * cos
        );
    }
}
