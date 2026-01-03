package net.j40climb.florafauna.common.block.iteminput.storageanchor;

import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Block entity for the Storage Anchor.
 * Manages storage destinations and paired item input blocks.
 *
 * Container detection:
 * - Scans for blocks with IItemHandler capability in radius
 * - Explicitly linked containers take priority
 * - Periodically refreshes auto-detected containers
 */
public class StorageAnchorBlockEntity extends BlockEntity {
    private static final int SCAN_RADIUS = 8;
    private static final int SCAN_INTERVAL_TICKS = 100; // 5 seconds
    private static final String TAG_LINKED = "LinkedContainers";
    private static final String TAG_ITEM_INPUTS = "PairedItemInputs";

    // Auto-detected containers (refreshed periodically)
    private List<StorageDestination> nearbyContainers = new ArrayList<>();

    // Explicitly linked distant containers (persistent)
    private List<StorageDestination> linkedContainers = new ArrayList<>();

    // Paired ItemInput block positions
    private Set<BlockPos> pairedItemInputs = new HashSet<>();

    // Tick counter for periodic scanning
    private int scanCooldown = 0;

    public StorageAnchorBlockEntity(BlockPos pos, BlockState blockState) {
        super(FloraFaunaRegistry.STORAGE_ANCHOR_BE.get(), pos, blockState);
    }

    /**
     * Server tick handler.
     */
    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
        if (level.isClientSide()) {
            return;
        }

        // Periodically scan for nearby containers
        scanCooldown--;
        if (scanCooldown <= 0) {
            scanCooldown = SCAN_INTERVAL_TICKS;
            scanForContainers(level, blockPos);
        }
    }

    /**
     * Scans for containers with IItemHandler capability in radius.
     */
    private void scanForContainers(Level level, BlockPos center) {
        nearbyContainers.clear();

        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int y = -SCAN_RADIUS; y <= SCAN_RADIUS; y++) {
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (pos.equals(center)) {
                        continue; // Skip self
                    }

                    // Check if block has item resource handler capability (NeoForge 21.9+ Transfer API)
                    if (level.getCapability(Capabilities.Item.BLOCK, pos, null) != null) {
                        // Don't add if already in linked list
                        boolean isLinked = linkedContainers.stream()
                                .anyMatch(dest -> dest.pos().equals(pos));
                        if (!isLinked) {
                            nearbyContainers.add(StorageDestination.autoDetected(pos));
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets all available storage destinations, sorted by priority.
     * Linked containers come first, then nearby auto-detected ones.
     *
     * @return Sorted list of valid storage destinations
     */
    public List<StorageDestination> getAvailableDestinations() {
        List<StorageDestination> all = new ArrayList<>();

        // Add linked containers (validate they still exist)
        for (StorageDestination dest : linkedContainers) {
            if (level != null && dest.isValid(level)) {
                all.add(dest);
            }
        }

        // Add nearby containers
        for (StorageDestination dest : nearbyContainers) {
            if (level != null && dest.isValid(level)) {
                all.add(dest);
            }
        }

        // Sort by priority (higher first)
        all.sort(Comparator.comparingInt(StorageDestination::priority).reversed());

        return all;
    }

    /**
     * Links a container at the specified position.
     *
     * @param pos Position of the container to link
     * @return true if linking succeeded
     */
    public boolean linkContainer(BlockPos pos) {
        if (level == null) {
            return false;
        }

        // Verify it has item resource handler capability (NeoForge 21.9+ Transfer API)
        if (level.getCapability(Capabilities.Item.BLOCK, pos, null) == null) {
            return false;
        }

        // Don't add duplicates
        boolean alreadyLinked = linkedContainers.stream()
                .anyMatch(dest -> dest.pos().equals(pos));
        if (alreadyLinked) {
            return false;
        }

        linkedContainers.add(StorageDestination.linked(pos));

        // Remove from auto-detected if present
        nearbyContainers.removeIf(dest -> dest.pos().equals(pos));

        setChanged();
        return true;
    }

    /**
     * Unlinks a container at the specified position.
     *
     * @param pos Position of the container to unlink
     * @return true if unlinking succeeded
     */
    public boolean unlinkContainer(BlockPos pos) {
        boolean removed = linkedContainers.removeIf(dest -> dest.pos().equals(pos));
        if (removed) {
            setChanged();
        }
        return removed;
    }

    /**
     * Registers an ItemInput block as paired to this anchor.
     */
    public void pairItemInput(BlockPos pos) {
        if (pairedItemInputs.add(pos)) {
            setChanged();
        }
    }

    /**
     * Unregisters an ItemInput block from this anchor.
     */
    public void unpairItemInput(BlockPos pos) {
        if (pairedItemInputs.remove(pos)) {
            setChanged();
        }
    }

    /**
     * Gets all paired ItemInput positions.
     */
    public Set<BlockPos> getPairedItemInputs() {
        return new HashSet<>(pairedItemInputs);
    }

    /**
     * Gets the count of linked containers.
     */
    public int getLinkedContainerCount() {
        return linkedContainers.size();
    }

    /**
     * Gets the count of auto-detected containers.
     */
    public int getNearbyContainerCount() {
        return nearbyContainers.size();
    }

    // ==================== SERIALIZATION ====================

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        // Save linked containers using Codec-based serialization
        output.store(TAG_LINKED, StorageDestination.CODEC.listOf(), linkedContainers);

        // Save paired item inputs
        output.store(TAG_ITEM_INPUTS, BlockPos.CODEC.listOf(), pairedItemInputs.stream().toList());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        // Load linked containers
        linkedContainers.clear();
        input.read(TAG_LINKED, StorageDestination.CODEC.listOf())
                .ifPresent(linkedContainers::addAll);

        // Load paired item inputs
        pairedItemInputs.clear();
        input.read(TAG_ITEM_INPUTS, BlockPos.CODEC.listOf())
                .ifPresent(pairedItemInputs::addAll);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
