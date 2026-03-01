package net.j40climb.florafauna.common.block.mininganchor;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.common.block.mininganchor.pod.AbstractStoragePodBlockEntity;
import net.j40climb.florafauna.common.block.vacuum.AbstractVacuumBlockEntity;
import net.j40climb.florafauna.common.block.vacuum.VacuumState;
import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.common.symbiote.observation.ObservationCategory;
import net.j40climb.florafauna.common.symbiote.voice.SymbioteVoiceService;
import net.j40climb.florafauna.common.symbiote.voice.VoiceTier;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.j40climb.florafauna.setup.FloraFaunaTags;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import java.util.Comparator;
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

    // Tracked pod positions
    protected final List<BlockPos> podPositions = new ArrayList<>();

    // Current fill state (for change detection)
    protected AnchorFillState currentFillState = AnchorFillState.NORMAL;

    /** Transient flag - true during teardown to prevent pods from spilling independently */
    private boolean isTearingDown = false;

    protected AbstractMiningAnchorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        // Anchor has minimal buffer - items go directly to pods
        super(type, pos, blockState, 1);
    }

    /**
     * Returns the configured maximum spawn radius for pods.
     */
    protected static int getConfigSpawnRadius() {
        return Config.miningAnchorPodSpawnRadius > 0 ? Config.miningAnchorPodSpawnRadius : 5;
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

    /**
     * Returns the maximum number of pods this anchor tier can spawn.
     * Subclasses return their tier-specific config value.
     */
    protected abstract int getMaxPods();

    /**
     * Returns the capacity of each pod this anchor spawns.
     * Subclasses return their tier-specific pod capacity.
     */
    protected abstract int getPodCapacity();

    /**
     * Resolves a pod's contents during anchor teardown.
     * Tier 1: Spills items in tight radius around pod position
     * Tier 2: Converts pod to item with contents preserved
     *
     * @param podPos The position of the pod to resolve
     */
    protected abstract void resolvePodContents(BlockPos podPos);

    // ==================== TEARDOWN ====================

    /**
     * Returns true if this anchor is currently tearing down.
     * Pods should check this to prevent double item drops.
     */
    public boolean isTearingDown() {
        return isTearingDown;
    }

    // ==================== CAPACITY CALCULATION ====================

    /**
     * Returns the total potential capacity across all possible pods.
     * Based on max pods × pod capacity, not just existing pods.
     */
    public int getMaxCapacity() {
        return getMaxPods() * getPodCapacity();
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

        // Try to add to existing pods first (closest to anchor first)
        for (BlockPos podPos : getPodPositionsByDistance()) {
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
        while (remaining > 0 && podPositions.size() < getMaxPods()) {
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

        // Update fill state after adding items
        checkFillStateChange(level, worldPosition, getBlockState());
    }

    /**
     * Tries to spawn a new pod at the next available position.
     * Uses expanding ring algorithm - searches outward from anchor.
     * @return true if a pod was spawned, false if no space available
     */
    protected boolean trySpawnPod(Level level) {
        BlockPos nextPos = findNextPodPosition(level);
        if (nextPos != null) {
            spawnPod(level, nextPos);
            return true;
        }
        return false;
    }

    /**
     * Finds the next available position for a pod using expanding ring algorithm.
     * Priority: same Y-level first, then Y+1 if blocked.
     * @return next valid position, or null if no space within spawn radius
     */
    protected BlockPos findNextPodPosition(Level level) {
        int maxRadius = getConfigSpawnRadius();
        List<BlockPos> candidates = new ArrayList<>();

        // Generate all candidate positions within radius, sorted by distance
        for (int dy = 0; dy <= 1; dy++) {
            for (int r = 1; r <= maxRadius; r++) {
                // Cardinal directions at this ring
                addCandidate(candidates, worldPosition.offset(r, dy, 0));
                addCandidate(candidates, worldPosition.offset(-r, dy, 0));
                addCandidate(candidates, worldPosition.offset(0, dy, r));
                addCandidate(candidates, worldPosition.offset(0, dy, -r));

                // Fill in the ring (diagonal and intermediate positions)
                for (int i = 1; i < r; i++) {
                    // All four quadrants
                    addCandidate(candidates, worldPosition.offset(r, dy, i));
                    addCandidate(candidates, worldPosition.offset(r, dy, -i));
                    addCandidate(candidates, worldPosition.offset(-r, dy, i));
                    addCandidate(candidates, worldPosition.offset(-r, dy, -i));
                    addCandidate(candidates, worldPosition.offset(i, dy, r));
                    addCandidate(candidates, worldPosition.offset(-i, dy, r));
                    addCandidate(candidates, worldPosition.offset(i, dy, -r));
                    addCandidate(candidates, worldPosition.offset(-i, dy, -r));
                }

                // Diagonal corners
                addCandidate(candidates, worldPosition.offset(r, dy, r));
                addCandidate(candidates, worldPosition.offset(r, dy, -r));
                addCandidate(candidates, worldPosition.offset(-r, dy, r));
                addCandidate(candidates, worldPosition.offset(-r, dy, -r));
            }
        }

        // Sort by: Y-level first (prefer same level), then distance from anchor
        candidates.sort(Comparator
                .comparingInt((BlockPos p) -> p.getY() - worldPosition.getY())
                .thenComparingDouble(p -> worldPosition.distSqr(p)));

        // Find first valid position
        for (BlockPos candidate : candidates) {
            if (!podPositions.contains(candidate) && canSpawnPodAt(level, candidate)) {
                return candidate;
            }
        }

        return null;
    }

    /**
     * Adds a position to the candidate list if not already present.
     */
    private void addCandidate(List<BlockPos> candidates, BlockPos pos) {
        if (!candidates.contains(pos)) {
            candidates.add(pos);
        }
    }

    /**
     * Checks if a pod can be spawned at the given position.
     * Valid positions are air or blocks tagged as pod_replaceable.
     */
    protected boolean canSpawnPodAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(FloraFaunaTags.Blocks.POD_REPLACEABLE);
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

        if (podPositions.size() >= getMaxPods()) {
            return false;
        }

        return trySpawnPod(level);
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

        // Try to add to existing pods first (closest to anchor first)
        for (BlockPos podPos : getPodPositionsByDistance()) {
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
        while (remaining > 0 && podPositions.size() < getMaxPods()) {
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

        // Update fill state after adding items
        if (totalAdded > 0) {
            checkFillStateChange(level, worldPosition, getBlockState());
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

        // Update fill state after clearing
        if (!level.isClientSide()) {
            checkFillStateChange(level, worldPosition, getBlockState());
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

        // Update fill state immediately when a pod is removed
        if (level != null && !level.isClientSide()) {
            checkFillStateChange(level, worldPosition, getBlockState());
        }
    }

    /**
     * Returns the current pod positions.
     */
    public List<BlockPos> getPodPositions() {
        return new ArrayList<>(podPositions);
    }

    /**
     * Returns pod positions sorted by distance from anchor (closest first).
     * Used for consistent fill order regardless of spawn order.
     */
    protected List<BlockPos> getPodPositionsByDistance() {
        List<BlockPos> sorted = new ArrayList<>(podPositions);
        sorted.sort(Comparator.comparingDouble(pos -> worldPosition.distSqr(pos)));
        return sorted;
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

        if (level == null || level.isClientSide()) {
            podPositions.clear();
            return;
        }

        // Set flag FIRST - pods will check this to prevent double drops
        isTearingDown = true;

        // 1. Clear waypoints for all players with this anchor
        clearWaypointsForAllPlayers();

        // 2. Send teardown dialogue
        sendTeardownDialogue();

        // 3. Resolve all pods (tier-specific behavior)
        for (BlockPos podPos : new ArrayList<>(podPositions)) {
            resolvePod(podPos);
        }

        podPositions.clear();
    }

    /**
     * Removes a pod and resolves its contents during teardown.
     * The anchor is responsible for item resolution - the pod block
     * will NOT spill items because isTearingDown is true.
     */
    protected void resolvePod(BlockPos podPos) {
        if (level == null) return;

        BlockEntity be = level.getBlockEntity(podPos);
        if (be instanceof AbstractStoragePodBlockEntity pod) {
            // Resolve contents BEFORE removing block (tier-specific)
            resolvePodContents(podPos);

            // Clear pod buffer (items already resolved)
            pod.getBuffer().clear();
        }

        // Remove the pod block - triggers playerWillDestroy on pod,
        // but pod will check isTearingDown and skip its own item handling
        level.removeBlock(podPos, false);
    }

    /**
     * Clears waypoints for all players who have this anchor as their waypoint.
     * Called during teardown before removing pods.
     */
    protected void clearWaypointsForAllPlayers() {
        if (!(level instanceof ServerLevel serverLevel)) return;

        ResourceKey<Level> thisDim = serverLevel.dimension();

        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            PlayerSymbioteData data = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);

            boolean changed = false;
            PlayerSymbioteData newData = data;

            // Clear active waypoint if it matches this anchor
            if (data.hasActiveWaypointAnchor() &&
                worldPosition.equals(data.activeWaypointAnchorPos()) &&
                thisDim.equals(data.activeWaypointAnchorDim())) {

                newData = newData.clearActiveWaypointAnchor();
                changed = true;
            }

            // Also clear bound anchor if it matches
            if (data.hasAnchorBound() &&
                worldPosition.equals(data.boundAnchorPos()) &&
                thisDim.equals(data.boundAnchorDim())) {

                newData = newData.clearBoundAnchor();
                changed = true;
            }

            if (changed) {
                player.setData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA, newData);
            }
        }
    }

    /**
     * Sends teardown confirmation dialogue to all players who had this anchor
     * as their waypoint or bound anchor.
     */
    protected void sendTeardownDialogue() {
        if (!(level instanceof ServerLevel serverLevel)) return;

        ResourceKey<Level> thisDim = serverLevel.dimension();

        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            PlayerSymbioteData data = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);

            // Send dialogue to players who had this as waypoint or bound anchor
            boolean wasWaypoint = data.hasActiveWaypointAnchor() &&
                worldPosition.equals(data.activeWaypointAnchorPos()) &&
                thisDim.equals(data.activeWaypointAnchorDim());

            boolean wasBound = data.hasAnchorBound() &&
                worldPosition.equals(data.boundAnchorPos()) &&
                thisDim.equals(data.boundAnchorDim());

            if (wasWaypoint || wasBound) {
                SymbioteVoiceService.trySpeak(
                    player,
                    VoiceTier.TIER_1_AMBIENT,
                    ObservationCategory.MINING_ANCHOR,
                    "symbiote.dialogue.anchor_released"
                );
            }
        }
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
