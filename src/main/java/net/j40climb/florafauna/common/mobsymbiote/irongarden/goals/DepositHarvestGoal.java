package net.j40climb.florafauna.common.mobsymbiote.irongarden.goals;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.common.mobsymbiote.irongarden.IronGardenActivity;
import net.j40climb.florafauna.common.mobsymbiote.irongarden.IronGardenData;
import net.j40climb.florafauna.common.mobsymbiote.irongarden.IronGardenHelper;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.j40climb.florafauna.setup.FloraFaunaTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.EnumSet;

/**
 * Goal for calm Iron Golems to deposit harvested ferric poppies into nearby storage.
 * Higher priority than harvesting when carrying poppies.
 */
public class DepositHarvestGoal extends Goal {
    private final IronGolem golem;
    private BlockPos storagePos;
    private int depositingTicks;
    private int stuckTicks;
    private int totalTicks;
    private BlockPos lastPosition;

    private static final int DEPOSITING_DURATION = 20; // 1 second to deposit
    private static final int STUCK_THRESHOLD = 40; // 2 seconds without progress = give up
    private static final int TOTAL_TIMEOUT = 40; // 2 seconds max total time for depositing attempt

    public DepositHarvestGoal(IronGolem golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        IronGardenData data = IronGardenHelper.getData(golem);

        // Only when calm and carrying poppies
        if (!data.ironGardenState().isCalm()) {
            return false;
        }
        if (!data.isCarryingPoppies()) {
            return false;
        }

        // First check remembered storage
        if (data.hasStorage()) {
            BlockPos remembered = data.getStorage().orElse(null);
            if (remembered != null && isValidStorage(remembered)) {
                storagePos = remembered;
                return true;
            } else {
                // Remembered storage is no longer valid, clear it
                IronGardenHelper.setData(golem, data.clearStorage());
            }
        }

        // Find a new storage container
        storagePos = findStorage();
        if (storagePos == null) {
            return false;
        }

        // Remember this storage for future use
        IronGardenHelper.setData(golem, IronGardenHelper.getData(golem).withStorage(storagePos));

        // Verify we can actually path to the target
        return golem.getNavigation().createPath(storagePos, 1) != null;
    }

    @Override
    public boolean canContinueToUse() {
        IronGardenData data = IronGardenHelper.getData(golem);
        if (!data.ironGardenState().isCalm()) {
            return false;
        }
        if (!data.isCarryingPoppies()) {
            return false;
        }
        // Check if storage is still valid
        if (storagePos == null) {
            return false;
        }
        // Give up if stuck for too long (not moving)
        if (stuckTicks >= STUCK_THRESHOLD) {
            // Clear remembered storage since we couldn't reach it
            IronGardenHelper.setData(golem, data.clearStorage());
            return false;
        }
        // Give up if total time exceeded (moving but never arriving)
        if (totalTicks >= TOTAL_TIMEOUT) {
            IronGardenHelper.setData(golem, data.clearStorage());
            return false;
        }
        return isValidStorage(storagePos);
    }

    @Override
    public void start() {
        depositingTicks = 0;
        stuckTicks = 0;
        totalTicks = 0;
        lastPosition = golem.blockPosition();
        IronGardenHelper.setActivity(golem, IronGardenActivity.DEPOSITING);
    }

    @Override
    public void tick() {
        if (storagePos == null) {
            return;
        }

        totalTicks++;
        // Look at storage
        golem.getLookControl().setLookAt(storagePos.getX() + 0.5, storagePos.getY() + 0.5, storagePos.getZ() + 0.5);

        // Move toward storage if not close enough
        double distSq = golem.distanceToSqr(storagePos.getX() + 0.5, storagePos.getY() + 0.5, storagePos.getZ() + 0.5);
        if (distSq > 4.0) {
            // Check if navigation failed (finished but we're still far away)
            if (golem.getNavigation().isDone()) {
                // Try to repath
                boolean success = golem.getNavigation().moveTo(storagePos.getX() + 0.5, storagePos.getY(), storagePos.getZ() + 0.5, 0.6);
                if (!success) {
                    // Can't path to target, give up
                    storagePos = null;
                    return;
                }
            } else {
                golem.getNavigation().moveTo(storagePos.getX() + 0.5, storagePos.getY(), storagePos.getZ() + 0.5, 0.6);
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

        // Stop moving when close
        golem.getNavigation().stop();

        // Increment depositing timer
        depositingTicks++;

        // Complete depositing
        if (depositingTicks >= DEPOSITING_DURATION) {
            depositPoppies();
        }
    }

    @Override
    public void stop() {
        storagePos = null;
        depositingTicks = 0;
        stuckTicks = 0;
        totalTicks = 0;
        lastPosition = null;
        IronGardenHelper.setActivity(golem, IronGardenActivity.IDLE);
    }

    /**
     * Checks if a position is a valid storage container with room.
     */
    private boolean isValidStorage(BlockPos pos) {
        Level level = golem.level();
        BlockState state = level.getBlockState(pos);
        if (!state.is(FloraFaunaTags.Blocks.IRON_GARDEN_STORAGE)) {
            return false;
        }
        // Verify it has inventory capability with room
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, null);
        if (handler == null) {
            return false;
        }
        ItemResource resource = ItemResource.of(new ItemStack(FloraFaunaRegistry.FERRIC_POPPY_ITEM.get(), 1));
        try (Transaction tx = Transaction.openRoot()) {
            long inserted = handler.insert(resource, 1, tx);
            return inserted > 0;
        }
    }

    /**
     * Finds a storage container within range.
     */
    private BlockPos findStorage() {
        Level level = golem.level();
        BlockPos golemPos = golem.blockPosition();
        int radius = Config.ironGardenStorageSearchRadius;

        // Search nearby for storage containers
        for (int y = -2; y <= 2; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = golemPos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);

                    if (state.is(FloraFaunaTags.Blocks.IRON_GARDEN_STORAGE)) {
                        // Verify it has inventory capability
                        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, checkPos, null);
                        if (handler != null) {
                            // Check if there's room using the Transfer API
                            ItemResource resource = ItemResource.of(new ItemStack(FloraFaunaRegistry.FERRIC_POPPY_ITEM.get(), 1));
                            try (Transaction tx = Transaction.openRoot()) {
                                long inserted = handler.insert(resource, 1, tx);
                                if (inserted > 0) {
                                    // Transaction auto-aborts since we don't commit
                                    return checkPos;
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Deposits ferric poppies into the storage container.
     */
    private void depositPoppies() {
        if (storagePos == null) {
            return;
        }

        Level level = golem.level();
        IronGardenData data = IronGardenHelper.getData(golem);
        int carried = data.carriedPoppies();

        if (carried <= 0) {
            storagePos = null;
            return;
        }

        // Get the resource handler using new Transfer API
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, storagePos, null);

        if (handler != null) {
            // Try to insert poppies using the Transfer API
            ItemResource resource = ItemResource.of(new ItemStack(FloraFaunaRegistry.FERRIC_POPPY_ITEM.get(), 1));

            int totalInserted = 0;
            try (Transaction tx = Transaction.openRoot()) {
                long inserted = handler.insert(resource, carried, tx);
                if (inserted > 0) {
                    tx.commit();
                    totalInserted = (int) inserted;
                }
            }

            // Update carried count
            int remaining = carried - totalInserted;
            IronGardenHelper.setData(golem, data.withCarriedPoppies(remaining));

            // If we still have poppies and config allows, drop them
            if (remaining > 0 && Config.ironGardenDropIfNoStorage) {
                dropPoppies(remaining);
                IronGardenHelper.setData(golem, data.clearCarriedPoppies());
            }
        } else {
            // No handler - drop if configured
            if (Config.ironGardenDropIfNoStorage) {
                dropPoppies(carried);
                IronGardenHelper.setData(golem, data.clearCarriedPoppies());
            }
        }

        // Clear storage target
        storagePos = null;
    }

    /**
     * Drops ferric poppies as item entities.
     */
    private void dropPoppies(int count) {
        Level level = golem.level();
        ItemStack stack = new ItemStack(FloraFaunaRegistry.FERRIC_POPPY_ITEM.get(), count);
        ItemEntity itemEntity = new ItemEntity(level, golem.getX(), golem.getY() + 0.5, golem.getZ(), stack);
        level.addFreshEntity(itemEntity);
    }
}
