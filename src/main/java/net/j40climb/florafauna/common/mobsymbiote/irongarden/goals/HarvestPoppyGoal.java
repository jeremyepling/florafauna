package net.j40climb.florafauna.common.mobsymbiote.irongarden.goals;

import net.j40climb.florafauna.common.block.ferricpoppy.FerricPoppyBlock;
import net.j40climb.florafauna.common.mobsymbiote.irongarden.IronGardenActivity;
import net.j40climb.florafauna.common.mobsymbiote.irongarden.IronGardenData;
import net.j40climb.florafauna.common.mobsymbiote.irongarden.IronGardenHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/**
 * Goal for calm Iron Golems to harvest ferric poppies.
 */
public class HarvestPoppyGoal extends Goal {
    private final IronGolem golem;
    private BlockPos targetPos;
    private int harvestingTicks;
    private int cooldown;
    private boolean isHarvesting;
    private int stuckTicks;
    private int totalTicks;
    private BlockPos lastPosition;

    private static final int HARVESTING_DURATION = 30; // 1.5 seconds to harvest
    private static final int POST_HARVEST_COOLDOWN = 20; // 1 second before next harvest
    private static final int STUCK_THRESHOLD = 40; // 2 seconds without progress = give up
    private static final int TOTAL_TIMEOUT = 40; // 2 seconds max total time for harvesting attempt

    public HarvestPoppyGoal(IronGolem golem) {
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
        if (data.isCarryingFull()) {
            return false;
        }

        // Find any ferric poppy to harvest
        targetPos = IronGardenHelper.findBlockInGarden(golem, this::isFerricPoppy);
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
        if (data.isCarryingFull()) {
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
        return isFerricPoppy(golem.level(), targetPos);
    }

    @Override
    public void start() {
        harvestingTicks = 0;
        isHarvesting = false;
        stuckTicks = 0;
        totalTicks = 0;
        lastPosition = golem.blockPosition();
        IronGardenHelper.setActivity(golem, IronGardenActivity.HARVESTING);
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

        if (!isHarvesting) {
            isHarvesting = true;
        }

        harvestingTicks++;

        // Swing arm periodically during harvesting for visual feedback
        if (harvestingTicks % 10 == 0) {
            golem.swing(InteractionHand.MAIN_HAND);
        }

        if (harvestingTicks >= HARVESTING_DURATION) {
            harvestPoppy();
        }
    }

    @Override
    public void stop() {
        targetPos = null;
        harvestingTicks = 0;
        isHarvesting = false;
        stuckTicks = 0;
        totalTicks = 0;
        lastPosition = null;
        IronGardenHelper.setActivity(golem, IronGardenActivity.IDLE);
    }

    /**
     * Checks if a position contains a ferric poppy.
     */
    private boolean isFerricPoppy(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof FerricPoppyBlock;
    }

    /**
     * Harvests the ferric poppy at the target position.
     */
    private void harvestPoppy() {
        if (targetPos == null) {
            return;
        }

        Level level = golem.level();
        if (!isFerricPoppy(level, targetPos)) {
            targetPos = null;
            return;
        }

        level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3);

        IronGardenData data = IronGardenHelper.getData(golem);
        IronGardenHelper.setData(golem, data.incrementHarvests());

        targetPos = null;
        cooldown = POST_HARVEST_COOLDOWN;
    }
}
