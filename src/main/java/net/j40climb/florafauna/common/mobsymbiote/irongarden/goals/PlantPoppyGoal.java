package net.j40climb.florafauna.common.mobsymbiote.irongarden.goals;

import net.j40climb.florafauna.common.mobsymbiote.irongarden.IronGardenActivity;
import net.j40climb.florafauna.common.mobsymbiote.irongarden.IronGardenData;
import net.j40climb.florafauna.common.mobsymbiote.irongarden.IronGardenHelper;
import net.j40climb.florafauna.setup.FloraFaunaTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/**
 * Goal for calm Iron Golems to plant vanilla poppies on valid ground.
 * Vanilla poppies are later converted to ferric poppies by the golem's presence.
 */
public class PlantPoppyGoal extends Goal {
    private final IronGolem golem;
    private BlockPos targetPos;
    private int plantingTicks;
    private int cooldown;
    private boolean isPlanting;
    private int stuckTicks;
    private int totalTicks;
    private BlockPos lastPosition;

    private static final int PLANTING_DURATION = 20; // 1 second to plant
    private static final int POST_PLANT_COOLDOWN = 60; // 3 seconds before next plant
    private static final int STUCK_THRESHOLD = 40; // 2 seconds without progress = give up
    private static final int TOTAL_TIMEOUT = 40; // 2 seconds max total time for planting attempt

    public PlantPoppyGoal(IronGolem golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        IronGardenData data = IronGardenHelper.getData(golem);
        if (!data.ironGardenState().isCalm()) {
            return false;
        }

        // Yield to harvesting if enough ferric poppies exist
        if (IronGardenHelper.countFerricPoppies(golem) >= 3) {
            return false;
        }

        targetPos = IronGardenHelper.findBlockInGarden(golem, this::isValidPlantingSpot);
        if (targetPos == null) {
            return false;
        }

        // Verify we can actually path to the target
        return golem.getNavigation().createPath(targetPos, 1) != null;
    }

    @Override
    public boolean canContinueToUse() {
        IronGardenData data = IronGardenHelper.getData(golem);
        if (!data.ironGardenState().isCalm()) {
            return false;
        }
        if (targetPos == null) {
            return false;
        }
        // Give up if stuck for too long (not moving)
        if (stuckTicks >= STUCK_THRESHOLD) {
            return false;
        }
        // Give up if total time exceeded (moving but never arriving)
        if (totalTicks >= TOTAL_TIMEOUT) {
            return false;
        }
        return isValidPlantingSpot(golem.level(), targetPos);
    }

    @Override
    public void start() {
        plantingTicks = 0;
        isPlanting = false;
        stuckTicks = 0;
        totalTicks = 0;
        lastPosition = golem.blockPosition();
        IronGardenHelper.setActivity(golem, IronGardenActivity.PLANTING);
    }

    @Override
    public void tick() {
        if (targetPos == null) {
            return;
        }

        totalTicks++;
        golem.getLookControl().setLookAt(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);

        double distSq = golem.distanceToSqr(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
        if (distSq > 4.0) {
            // Check if navigation failed (finished but we're still far away)
            if (golem.getNavigation().isDone()) {
                // Try to repath
                boolean success = golem.getNavigation().moveTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, 0.6);
                if (!success) {
                    // Can't path to target, give up
                    targetPos = null;
                    return;
                }
            } else {
                golem.getNavigation().moveTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, 0.6);
            }

            // Track stuck detection
            BlockPos currentPos = golem.blockPosition();
            if (currentPos.equals(lastPosition)) {
                stuckTicks++;
            } else {
                stuckTicks = 0;
                lastPosition = currentPos;
            }
            return;
        }

        golem.getNavigation().stop();

        if (!isPlanting) {
            isPlanting = true;
            golem.offerFlower(true);
        }

        plantingTicks++;

        if (plantingTicks >= PLANTING_DURATION) {
            plantPoppy();
        }
    }

    @Override
    public void stop() {
        golem.offerFlower(false);
        targetPos = null;
        plantingTicks = 0;
        isPlanting = false;
        stuckTicks = 0;
        totalTicks = 0;
        lastPosition = null;
        IronGardenHelper.setActivity(golem, IronGardenActivity.IDLE);
    }

    /**
     * Checks if a position is valid for planting (plantable ground below, air above).
     */
    private boolean isValidPlantingSpot(Level level, BlockPos pos) {
        BlockState ground = level.getBlockState(pos.below());
        BlockState space = level.getBlockState(pos);
        return ground.is(FloraFaunaTags.Blocks.FERRIC_POPPY_PLANTABLE) && space.isAir();
    }

    /**
     * Plants a vanilla poppy at the target position.
     */
    private void plantPoppy() {
        if (targetPos == null) {
            return;
        }

        Level level = golem.level();
        if (!isValidPlantingSpot(level, targetPos)) {
            targetPos = null;
            return;
        }

        level.setBlock(targetPos, Blocks.POPPY.defaultBlockState(), 3);

        IronGardenData data = IronGardenHelper.getData(golem);
        IronGardenHelper.setData(golem, data.incrementPlants());

        targetPos = null;
        cooldown = POST_PLANT_COOLDOWN;
    }
}
