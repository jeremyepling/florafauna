package net.j40climb.florafauna.common.block.mininganchor;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.common.block.mininganchor.pod.AbstractStoragePodBlockEntity;
import net.j40climb.florafauna.common.block.vacuum.AbstractVacuumBlockEntity;
import net.j40climb.florafauna.common.block.vacuum.BufferTransfer;
import net.j40climb.florafauna.common.block.vacuum.VacuumState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for Mining Anchor block entities.
 * Extends AbstractVacuumBlockEntity with pod management and capacity tracking.
 *
 * Mining Anchors:
 * - Collect block drops from the world (collectBlockDropsOnly = true)
 * - Automatically spawn pods when buffer fills up
 * - Overflow items to pods when anchor buffer is full
 * - Track fill state for waypoint display and symbiote dialogue
 */
public abstract class AbstractMiningAnchorBlockEntity extends AbstractVacuumBlockEntity {
    private static final String KEY_POD_POSITIONS = "pod_positions";

    // Default capacity of anchor buffer (in items) - overridden by config
    protected static final int DEFAULT_BASE_CAPACITY = 256;

    // Default maximum number of pods - overridden by config
    protected static final int DEFAULT_MAX_PODS = 4;

    // Default pod growth threshold - overridden by config
    protected static final float DEFAULT_POD_GROWTH_THRESHOLD = 0.8f;

    // Relative positions where pods can spawn
    protected static final BlockPos[] POD_OFFSETS = {
            new BlockPos(1, 0, 0),
            new BlockPos(-1, 0, 0),
            new BlockPos(0, 0, 1),
            new BlockPos(0, 0, -1)
    };

    // Tracked pod positions
    protected final List<BlockPos> podPositions = new ArrayList<>();

    // Current fill state (for change detection)
    protected AnchorFillState currentFillState = AnchorFillState.NORMAL;

    protected AbstractMiningAnchorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState, calculateMaxStacks(getConfigBaseCapacity()));
    }

    /**
     * Calculate buffer slot count from item capacity.
     * Assumes average stack size of 64.
     */
    private static int calculateMaxStacks(int itemCapacity) {
        return Math.max(1, itemCapacity / 64);
    }

    /**
     * Returns the configured base capacity, with fallback to default.
     */
    protected static int getConfigBaseCapacity() {
        return Config.miningAnchorBaseCapacity > 0 ? Config.miningAnchorBaseCapacity : DEFAULT_BASE_CAPACITY;
    }

    /**
     * Returns the configured max pods, with fallback to default.
     */
    protected static int getConfigMaxPods() {
        return Config.miningAnchorMaxPods > 0 ? Config.miningAnchorMaxPods : DEFAULT_MAX_PODS;
    }

    /**
     * Returns the configured pod growth threshold, with fallback to default.
     */
    protected static float getConfigPodGrowthThreshold() {
        return Config.miningAnchorPodGrowthThreshold > 0 ? (float) Config.miningAnchorPodGrowthThreshold : DEFAULT_POD_GROWTH_THRESHOLD;
    }

    // ==================== BLOCK DROP FILTERING ====================

    @Override
    protected boolean collectBlockDropsOnly() {
        return true; // Mining anchors only collect block drops
    }

    // ==================== ABSTRACT METHODS ====================

    /**
     * Returns the block to spawn as a pod.
     * Subclasses return their tier-specific pod block.
     */
    protected abstract Block getPodBlock();

    // ==================== CAPACITY CALCULATION ====================

    /**
     * Returns the base capacity of this anchor (without pods).
     */
    public int getBaseCapacity() {
        return getConfigBaseCapacity();
    }

    /**
     * Returns the total maximum capacity including all pods.
     */
    public int getMaxCapacity() {
        int podCapacity = 0;
        if (level != null) {
            for (BlockPos podPos : podPositions) {
                BlockEntity be = level.getBlockEntity(podPos);
                if (be instanceof AbstractStoragePodBlockEntity pod) {
                    podCapacity += pod.getCapacity();
                }
            }
        }
        return getConfigBaseCapacity() + podCapacity;
    }

    /**
     * Returns the total number of stored items (anchor + all pods).
     */
    public int getStoredCount() {
        int count = buffer.getTotalItemCount();
        if (level != null) {
            for (BlockPos podPos : podPositions) {
                BlockEntity be = level.getBlockEntity(podPos);
                if (be instanceof AbstractStoragePodBlockEntity pod) {
                    count += pod.getStoredCount();
                }
            }
        }
        return count;
    }

    /**
     * Returns the current fill state based on stored vs max capacity.
     */
    public AnchorFillState getFillState() {
        return AnchorFillState.fromFillRatio(getStoredCount(), getMaxCapacity());
    }

    // ==================== TICK PROCESSING ====================

    @Override
    protected void tickProcessing(Level level, BlockPos pos, BlockState state) {
        // Pod growth: spawn new pods when anchor is filling up
        tickPodGrowth(level);

        // Overflow: move excess items to pods
        tickOverflow(level);

        // Fill state change detection
        checkFillStateChange(level, pos, state);
    }

    /**
     * Spawns new pods when the anchor buffer is getting full.
     */
    protected void tickPodGrowth(Level level) {
        if (podPositions.size() >= getConfigMaxPods()) {
            return;
        }

        // Check if buffer is above growth threshold
        float fillRatio = (float) buffer.getTotalItemCount() / getBaseCapacity();
        if (fillRatio < getConfigPodGrowthThreshold()) {
            return;
        }

        // Find next available spawn position
        for (BlockPos offset : POD_OFFSETS) {
            BlockPos podPos = worldPosition.offset(offset);
            if (canSpawnPodAt(level, podPos) && !podPositions.contains(podPos)) {
                spawnPod(level, podPos);
                break;
            }
        }
    }

    /**
     * Checks if a pod can be spawned at the given position.
     */
    protected boolean canSpawnPodAt(Level level, BlockPos pos) {
        return level.getBlockState(pos).isAir();
    }

    /**
     * Spawns a pod at the given position.
     */
    protected void spawnPod(Level level, BlockPos pos) {
        Block podBlock = getPodBlock();
        if (podBlock != null) {
            level.setBlock(pos, podBlock.defaultBlockState(), Block.UPDATE_ALL);
            podPositions.add(pos);
            setChanged();

            // Link pod to anchor
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AbstractStoragePodBlockEntity pod) {
                pod.setParentAnchor(worldPosition);
            }
        }
    }

    /**
     * Moves excess items from anchor buffer to pods.
     */
    protected void tickOverflow(Level level) {
        // Only overflow if buffer has items and we have pods
        if (buffer.isEmpty() || podPositions.isEmpty()) {
            return;
        }

        // Move items to pods that aren't full
        for (BlockPos podPos : podPositions) {
            BlockEntity be = level.getBlockEntity(podPos);
            if (be instanceof AbstractStoragePodBlockEntity pod && !pod.isFull()) {
                // Calculate remaining capacity for this pod
                int remainingCapacity = pod.getCapacity() - pod.getStoredCount();
                if (remainingCapacity <= 0) {
                    continue; // Pod is full, try next one
                }

                // Transfer up to remaining capacity
                BufferTransfer.TransferResult result = BufferTransfer.transfer(buffer, pod.getBuffer(), remainingCapacity);
                if (result.itemsTransferred() > 0) {
                    setChanged();
                    pod.markChangedAndSync(); // Mark pod as changed and sync to clients
                    break; // One transfer per tick
                }
            }
        }
    }

    /**
     * Checks for fill state changes and triggers events.
     */
    protected void checkFillStateChange(Level level, BlockPos pos, BlockState state) {
        AnchorFillState newFillState = getFillState();

        if (newFillState != currentFillState) {
            AnchorFillState oldState = currentFillState;
            currentFillState = newFillState;

            // Update block state visual
            VacuumState vacuumState = switch (newFillState) {
                case FULL -> VacuumState.BLOCKED;
                case WARNING -> VacuumState.WORKING;
                case NORMAL -> VacuumState.NORMAL;
            };

            if (state.hasProperty(STATE) && state.getValue(STATE) != vacuumState) {
                updateBlockState(level, pos, state, vacuumState);
            }

            // Fire events for symbiote dialogue
            onFillStateChanged(oldState, newFillState);
        }
    }

    /**
     * Called when fill state changes. Triggers dialogue and syncs to bound players.
     */
    protected void onFillStateChanged(AnchorFillState oldState, AnchorFillState newState) {
        if (level instanceof ServerLevel serverLevel) {
            MiningAnchorDialogueEvents.onFillStateChanged(serverLevel, worldPosition, oldState, newState);
        }
    }

    // ==================== POD MANAGEMENT ====================

    /**
     * Forces a pod to spawn, bypassing the growth threshold.
     * Used for testing via commands.
     *
     * @return true if a pod was spawned, false if no space available or max pods reached
     */
    public boolean forceSpawnPod() {
        if (level == null) {
            return false;
        }

        if (podPositions.size() >= getConfigMaxPods()) {
            return false;
        }

        for (BlockPos offset : POD_OFFSETS) {
            BlockPos podPos = worldPosition.offset(offset);
            if (canSpawnPodAt(level, podPos) && !podPositions.contains(podPos)) {
                spawnPod(level, podPos);
                return true;
            }
        }
        return false;
    }

    /**
     * Called when a pod is removed from the world.
     *
     * @param podPos The position of the removed pod
     */
    public void onPodRemoved(BlockPos podPos) {
        podPositions.remove(podPos);
        setChanged();
    }

    /**
     * Returns the current pod positions.
     */
    public List<BlockPos> getPodPositions() {
        return new ArrayList<>(podPositions);
    }

    /**
     * Returns the number of active pods.
     */
    public int getPodCount() {
        return podPositions.size();
    }

    // ==================== BLOCK REMOVAL ====================

    @Override
    public void onRemoved() {
        super.onRemoved(); // Drops anchor buffer contents

        // Don't remove pods - they spill their own contents when broken
        // Just clear the tracking list
        podPositions.clear();
    }

    // ==================== SERIALIZATION ====================

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store(KEY_POD_POSITIONS, BlockPos.CODEC.listOf(), podPositions);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        podPositions.clear();
        podPositions.addAll(input.read(KEY_POD_POSITIONS, BlockPos.CODEC.listOf()).orElse(List.of()));
    }
}
