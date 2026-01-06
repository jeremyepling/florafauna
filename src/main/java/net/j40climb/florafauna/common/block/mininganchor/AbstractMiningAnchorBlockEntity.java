package net.j40climb.florafauna.common.block.mininganchor;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.common.block.mininganchor.pod.AbstractStoragePodBlockEntity;
import net.j40climb.florafauna.common.block.vacuum.AbstractVacuumBlockEntity;
import net.j40climb.florafauna.common.block.vacuum.VacuumState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
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
 * - Store ALL items in pods (anchor itself has no storage)
 * - Automatically spawn pods as needed up to max pods
 * - Track fill state for waypoint display and symbiote dialogue
 */
public abstract class AbstractMiningAnchorBlockEntity extends AbstractVacuumBlockEntity {
    private static final String KEY_POD_POSITIONS = "pod_positions";

    // Default total capacity across all pods (in items) - overridden by config
    protected static final int DEFAULT_BASE_CAPACITY = 256;

    // Default maximum number of pods - overridden by config
    protected static final int DEFAULT_MAX_PODS = 4;

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
        // Anchor has minimal buffer - items go directly to pods
        super(type, pos, blockState, 1);
    }

    /**
     * Returns the configured total capacity (across all pods), with fallback to default.
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
     * Returns the total capacity across all pods.
     * This is the configured base capacity (all storage is in pods).
     */
    public int getMaxCapacity() {
        return getConfigBaseCapacity();
    }

    /**
     * Returns the total number of stored items (sum of all pods).
     * The anchor itself stores nothing - all items are in pods.
     */
    public int getStoredCount() {
        int count = 0;
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
        // Fill state change detection
        checkFillStateChange(level, pos, state);
    }

    // ==================== ITEM COLLECTION ====================

    /**
     * Overrides parent to store items directly in pods instead of anchor buffer.
     * Spawns pods as needed to store incoming items.
     */
    @Override
    protected void absorbItem(ItemEntity itemEntity) {
        if (level == null) {
            return;
        }

        ItemStack stack = itemEntity.getItem().copy();
        int remaining = stack.getCount();

        // Try to add to existing pods first
        for (BlockPos podPos : podPositions) {
            if (remaining <= 0) break;

            BlockEntity be = level.getBlockEntity(podPos);
            if (be instanceof AbstractStoragePodBlockEntity pod && !pod.isFull()) {
                int canAdd = Math.min(remaining, pod.getCapacity() - pod.getStoredCount());
                if (canAdd > 0) {
                    ItemStack toAdd = stack.copyWithCount(canAdd);
                    int added = pod.getBuffer().add(toAdd);
                    if (added > 0) {
                        remaining -= added;
                        pod.markChangedAndSync();
                    }
                }
            }
        }

        // Spawn new pods if needed and we have capacity
        while (remaining > 0 && podPositions.size() < getConfigMaxPods()) {
            if (!trySpawnPod(level)) {
                break; // No space for more pods
            }

            // Add to the newly spawned pod
            BlockPos newPodPos = podPositions.get(podPositions.size() - 1);
            BlockEntity be = level.getBlockEntity(newPodPos);
            if (be instanceof AbstractStoragePodBlockEntity pod) {
                int canAdd = Math.min(remaining, pod.getCapacity());
                if (canAdd > 0) {
                    ItemStack toAdd = stack.copyWithCount(canAdd);
                    int added = pod.getBuffer().add(toAdd);
                    if (added > 0) {
                        remaining -= added;
                        pod.markChangedAndSync();
                    }
                }
            }
        }

        // Remove the item entity (even if we couldn't store everything)
        itemEntity.discard();
    }

    /**
     * Tries to spawn a new pod at an available position.
     * @return true if a pod was spawned, false if no space available
     */
    protected boolean trySpawnPod(Level level) {
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
     * Adds items directly to pods. Spawns new pods as needed.
     * Used by commands and other external sources.
     *
     * @param stack The items to add
     * @return The number of items that were added
     */
    public int addItems(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return 0;
        }

        int remaining = stack.getCount();
        int totalAdded = 0;

        // Try to add to existing pods first
        for (BlockPos podPos : podPositions) {
            if (remaining <= 0) break;

            BlockEntity be = level.getBlockEntity(podPos);
            if (be instanceof AbstractStoragePodBlockEntity pod && !pod.isFull()) {
                int canAdd = Math.min(remaining, pod.getCapacity() - pod.getStoredCount());
                if (canAdd > 0) {
                    ItemStack toAdd = stack.copyWithCount(canAdd);
                    int added = pod.getBuffer().add(toAdd);
                    if (added > 0) {
                        remaining -= added;
                        totalAdded += added;
                        pod.markChangedAndSync();
                    }
                }
            }
        }

        // Spawn new pods if needed
        while (remaining > 0 && podPositions.size() < getConfigMaxPods()) {
            if (!trySpawnPod(level)) {
                break;
            }

            BlockPos newPodPos = podPositions.get(podPositions.size() - 1);
            BlockEntity be = level.getBlockEntity(newPodPos);
            if (be instanceof AbstractStoragePodBlockEntity pod) {
                int canAdd = Math.min(remaining, pod.getCapacity());
                if (canAdd > 0) {
                    ItemStack toAdd = stack.copyWithCount(canAdd);
                    int added = pod.getBuffer().add(toAdd);
                    if (added > 0) {
                        remaining -= added;
                        totalAdded += added;
                        pod.markChangedAndSync();
                    }
                }
            }
        }

        return totalAdded;
    }

    /**
     * Clears all items from all pods.
     */
    public void clearAllPods() {
        if (level == null) return;

        for (BlockPos podPos : podPositions) {
            BlockEntity be = level.getBlockEntity(podPos);
            if (be instanceof AbstractStoragePodBlockEntity pod) {
                pod.getBuffer().clear();
                pod.markChangedAndSync();
            }
        }
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
