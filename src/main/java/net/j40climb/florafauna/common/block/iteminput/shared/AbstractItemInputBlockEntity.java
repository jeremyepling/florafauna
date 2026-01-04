package net.j40climb.florafauna.common.block.iteminput.shared;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.common.block.iteminput.storageanchor.StorageAnchorBlockEntity;
import net.j40climb.florafauna.common.block.iteminput.storageanchor.StorageDestination;
import net.j40climb.florafauna.common.block.vacuum.AbstractVacuumBlockEntity;
import net.j40climb.florafauna.common.block.vacuum.VacuumState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Abstract base class for item input block entities.
 * Extends AbstractVacuumBlockEntity and adds transfer-to-storage functionality.
 *
 * Item Lifecycle: FREE -> CLAIMED -> ABSORBED -> BUFFERED -> STORED
 */
public abstract class AbstractItemInputBlockEntity extends AbstractVacuumBlockEntity {
    private static final String KEY_PAIRED_ANCHOR = "paired_anchor";
    private static final String KEY_BACKOFF_TICKS = "backoff_ticks";

    // Position of paired StorageAnchor (null if not paired)
    @Nullable
    protected BlockPos pairedAnchorPos;

    // Tick cooldowns for transfer
    private int transferCooldown = 0;

    // Exponential backoff for blocked state
    private int backoffTicks = 0;
    private int currentBackoff = 0;

    protected AbstractItemInputBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState, Config.maxBufferedStacks);
    }

    // ==================== TRANSFER PHASE ====================

    /**
     * Implements the processing phase: transfers buffered items to paired storage.
     */
    @Override
    protected void tickProcessing(Level level, BlockPos pos, BlockState state) {
        tickTransfer(level, pos, state);
    }

    /**
     * Transfers buffered items to paired storage using the Transaction API.
     */
    protected void tickTransfer(Level level, BlockPos pos, BlockState state) {
        // Handle backoff
        if (backoffTicks > 0) {
            backoffTicks--;
            return;
        }

        transferCooldown--;
        if (transferCooldown > 0) {
            return;
        }
        transferCooldown = Config.transferIntervalTicks;

        // Update state based on buffer
        VacuumState currentState = getCurrentState(state);
        if (buffer.isEmpty()) {
            if (currentState != VacuumState.NORMAL) {
                updateBlockState(level, pos, state, VacuumState.NORMAL);
            }
            return;
        }

        // Get storage anchor
        StorageAnchorBlockEntity anchor = getPairedAnchor(level);
        if (anchor == null) {
            if (currentState != VacuumState.BLOCKED) {
                updateBlockState(level, pos, state, VacuumState.BLOCKED);
            }
            applyBackoff();
            return;
        }

        // Get destinations
        List<StorageDestination> destinations = anchor.getAvailableDestinations();
        if (destinations.isEmpty()) {
            if (currentState != VacuumState.BLOCKED) {
                updateBlockState(level, pos, state, VacuumState.BLOCKED);
            }
            applyBackoff();
            return;
        }

        // Try to transfer items
        boolean transferredAny = false;
        int stacksTransferred = 0;
        int itemsTransferred = 0;

        while (stacksTransferred < Config.maxStacksPerTransferTick &&
               itemsTransferred < Config.maxItemsPerTransferTick &&
               !buffer.isEmpty()) {

            int firstSlot = buffer.getFirstNonEmptySlot();
            if (firstSlot < 0) break;

            ItemStack toTransfer = buffer.getStack(firstSlot);
            if (toTransfer.isEmpty()) break;

            int transferred = transferToDestinations(level, toTransfer, destinations);
            if (transferred > 0) {
                toTransfer.shrink(transferred);
                if (toTransfer.isEmpty()) {
                    buffer.setStack(firstSlot, ItemStack.EMPTY);
                }
                transferredAny = true;
                itemsTransferred += transferred;
                stacksTransferred++;
                setChanged();
            } else {
                break; // Couldn't transfer, destinations may be full
            }
        }

        // Update state
        if (transferredAny) {
            currentBackoff = 0; // Reset backoff on success
            if (currentState != VacuumState.WORKING) {
                updateBlockState(level, pos, state, VacuumState.WORKING);
            }
        } else if (!buffer.isEmpty()) {
            if (currentState != VacuumState.BLOCKED) {
                updateBlockState(level, pos, state, VacuumState.BLOCKED);
            }
            applyBackoff();
        }
    }

    /**
     * Transfers items to available destinations using NeoForge Transfer API.
     * Uses Transaction.openRoot() for atomic operations.
     *
     * @param level The level
     * @param stack The stack to transfer
     * @param destinations Available storage destinations
     * @return Number of items transferred
     */
    protected int transferToDestinations(Level level, ItemStack stack, List<StorageDestination> destinations) {
        int totalTransferred = 0;
        int remaining = stack.getCount();

        for (StorageDestination dest : destinations) {
            if (remaining <= 0) break;

            ResourceHandler<ItemResource> handler = dest.getResourceHandler(level, null);
            if (handler == null) continue;

            // Use the Transfer API with Transaction
            ItemResource resource = ItemResource.of(stack);

            try (Transaction tx = Transaction.openRoot()) {
                int inserted = (int) handler.insert(resource, remaining, tx);
                if (inserted > 0) {
                    tx.commit();
                    totalTransferred += inserted;
                    remaining -= inserted;
                }
                // If nothing was inserted, transaction auto-aborts on close
            }
        }

        return totalTransferred;
    }

    /**
     * Gets the paired StorageAnchor block entity.
     */
    @Nullable
    protected StorageAnchorBlockEntity getPairedAnchor(Level level) {
        if (pairedAnchorPos == null) {
            return null;
        }

        BlockEntity be = level.getBlockEntity(pairedAnchorPos);
        if (be instanceof StorageAnchorBlockEntity anchor) {
            return anchor;
        }

        // Anchor was removed, clear the pairing
        pairedAnchorPos = null;
        setChanged();
        return null;
    }

    /**
     * Pairs this item input with a StorageAnchor.
     */
    public void pairWithAnchor(BlockPos anchorPos) {
        // Unpair from old anchor first
        if (pairedAnchorPos != null && level != null) {
            StorageAnchorBlockEntity oldAnchor = getPairedAnchor(level);
            if (oldAnchor != null) {
                oldAnchor.unpairItemInput(worldPosition);
            }
        }

        this.pairedAnchorPos = anchorPos;

        // Register with new anchor
        if (level != null && anchorPos != null) {
            BlockEntity be = level.getBlockEntity(anchorPos);
            if (be instanceof StorageAnchorBlockEntity anchor) {
                anchor.pairItemInput(worldPosition);
            }
        }

        setChanged();
    }

    /**
     * Unpairs this item input from its StorageAnchor.
     */
    public void unpairAnchor() {
        if (pairedAnchorPos != null && level != null) {
            StorageAnchorBlockEntity anchor = getPairedAnchor(level);
            if (anchor != null) {
                anchor.unpairItemInput(worldPosition);
            }
        }
        pairedAnchorPos = null;
        setChanged();
    }

    /**
     * Applies exponential backoff when blocked.
     */
    private void applyBackoff() {
        if (currentBackoff == 0) {
            currentBackoff = Config.blockedRetryBaseTicks;
        } else {
            currentBackoff = Math.min(currentBackoff * 2, Config.blockedRetryMaxTicks);
        }
        backoffTicks = currentBackoff;
    }

    // ==================== BLOCK REMOVAL ====================

    @Override
    public void onRemoved() {
        super.onRemoved(); // Drops buffer contents and releases claimed items
        unpairAnchor();
    }

    // ==================== SERIALIZATION ====================

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        if (pairedAnchorPos != null) {
            output.store(KEY_PAIRED_ANCHOR, BlockPos.CODEC, pairedAnchorPos);
        }

        output.putInt(KEY_BACKOFF_TICKS, currentBackoff);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        pairedAnchorPos = input.read(KEY_PAIRED_ANCHOR, BlockPos.CODEC).orElse(null);
        currentBackoff = input.getIntOr(KEY_BACKOFF_TICKS, 0);
    }

    // ==================== ACCESSORS ====================

    @Nullable
    public BlockPos getPairedAnchorPos() {
        return pairedAnchorPos;
    }

    public boolean isPaired() {
        return pairedAnchorPos != null;
    }
}
