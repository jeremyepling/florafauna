package net.j40climb.florafauna.common.block.iteminput.shared;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.common.block.iteminput.ClaimedItemData;
import net.j40climb.florafauna.common.block.iteminput.ItemInputBuffer;
import net.j40climb.florafauna.common.block.iteminput.ItemInputState;
import net.j40climb.florafauna.common.block.iteminput.rootiteminput.networking.ItemInputAnimationPayload;
import net.j40climb.florafauna.common.block.iteminput.storageanchor.StorageAnchorBlockEntity;
import net.j40climb.florafauna.common.block.iteminput.storageanchor.StorageDestination;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Abstract base class for item input block entities.
 * Handles the core tick logic for collecting, absorbing, and transferring items.
 *
 * Item Lifecycle: FREE → CLAIMED → ABSORBED → BUFFERED → STORED
 */
public abstract class AbstractItemInputBlockEntity extends BlockEntity {
    public static final EnumProperty<ItemInputState> STATE = EnumProperty.create("state", ItemInputState.class);

    private static final String KEY_PAIRED_ANCHOR = "paired_anchor";
    private static final String KEY_BACKOFF_TICKS = "backoff_ticks";

    // Buffer for items waiting to be transferred
    protected final ItemInputBuffer buffer;

    // Position of paired StorageAnchor (null if not paired)
    @Nullable
    protected BlockPos pairedAnchorPos;

    // Tick cooldowns
    private int collectCooldown = 0;
    private int transferCooldown = 0;

    // Exponential backoff for blocked state
    private int backoffTicks = 0;
    private int currentBackoff = 0;

    protected AbstractItemInputBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.buffer = new ItemInputBuffer(Config.maxBufferedStacks);
    }

    /**
     * Main tick handler called by the block's ticker.
     */
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }

        // Run collection phase
        tickCollection(level, pos);

        // Run absorption phase
        tickAbsorption(level);

        // Run transfer phase
        tickTransfer(level, pos, state);
    }

    // ==================== COLLECTION PHASE ====================

    /**
     * Scans for nearby item entities and claims them.
     */
    protected void tickCollection(Level level, BlockPos pos) {
        collectCooldown--;
        if (collectCooldown > 0) {
            return;
        }
        collectCooldown = Config.collectIntervalTicks;

        // Don't collect if buffer is full
        if (buffer.isFull()) {
            return;
        }

        int radius = Config.collectRadius;
        AABB scanArea = new AABB(
                pos.getX() - radius, pos.getY() - radius, pos.getZ() - radius,
                pos.getX() + radius + 1, pos.getY() + radius + 1, pos.getZ() + radius + 1
        );

        List<ItemEntity> nearbyItems = level.getEntitiesOfClass(ItemEntity.class, scanArea);

        int claimedEntities = 0;
        int claimedItems = 0;

        for (ItemEntity itemEntity : nearbyItems) {
            if (claimedEntities >= Config.maxItemEntitiesPerCollect) {
                break;
            }
            if (claimedItems >= Config.maxItemsPerCollect) {
                break;
            }

            // Skip already claimed items
            ClaimedItemData existingData = itemEntity.getData(FloraFaunaRegistry.CLAIMED_ITEM_DATA);
            if (existingData.claimed()) {
                continue;
            }

            // Skip items that can't fit in buffer
            if (!buffer.canAccept(itemEntity.getItem())) {
                continue;
            }

            // Claim the item
            claimItem(level, itemEntity);
            claimedEntities++;
            claimedItems += itemEntity.getItem().getCount();
        }
    }

    /**
     * Claims an item entity, preventing player pickup and starting animation.
     */
    protected void claimItem(Level level, ItemEntity itemEntity) {
        // Prevent player pickup
        itemEntity.setPickUpDelay(Integer.MAX_VALUE);

        // Attach claim data
        long currentTick = level.getGameTime();
        ClaimedItemData claimData = ClaimedItemData.create(
                worldPosition,
                currentTick,
                Config.animationDurationTicks
        );
        itemEntity.setData(FloraFaunaRegistry.CLAIMED_ITEM_DATA, claimData);

        // Send animation payload to nearby clients
        if (level instanceof ServerLevel serverLevel) {
            ItemInputAnimationPayload payload = new ItemInputAnimationPayload(
                    itemEntity.getId(),
                    worldPosition,
                    Config.animationDurationTicks
            );
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, level.getChunk(worldPosition).getPos(), payload);
        }
    }

    // ==================== ABSORPTION PHASE ====================

    /**
     * Checks claimed items and absorbs those whose animation has completed.
     */
    protected void tickAbsorption(Level level) {
        int radius = Config.collectRadius;
        AABB scanArea = new AABB(
                worldPosition.getX() - radius, worldPosition.getY() - radius, worldPosition.getZ() - radius,
                worldPosition.getX() + radius + 1, worldPosition.getY() + radius + 1, worldPosition.getZ() + radius + 1
        );

        List<ItemEntity> nearbyItems = level.getEntitiesOfClass(ItemEntity.class, scanArea);
        long currentTick = level.getGameTime();

        for (ItemEntity itemEntity : nearbyItems) {
            ClaimedItemData claimData = itemEntity.getData(FloraFaunaRegistry.CLAIMED_ITEM_DATA);

            // Only process items claimed by this block
            if (!claimData.claimed() || !claimData.itemInputPos().equals(worldPosition)) {
                continue;
            }

            // Check if animation is complete
            if (claimData.isAnimationComplete(currentTick)) {
                absorbItem(itemEntity);
            }
        }
    }

    /**
     * Absorbs an item entity into the buffer and removes it from the world.
     */
    protected void absorbItem(ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getItem().copy();

        // Add to buffer
        int added = buffer.add(stack);

        if (added > 0) {
            // Play absorption sound
            // TODO: Add custom sound (Phase 7)

            // Mark as changed for saving
            setChanged();
        }

        // Remove the entity (even if we couldn't add everything - safety rule)
        itemEntity.discard();
    }

    // ==================== TRANSFER PHASE ====================

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
        ItemInputState currentState = state.getValue(STATE);
        if (buffer.isEmpty()) {
            if (currentState != ItemInputState.NORMAL) {
                updateBlockState(level, pos, state, ItemInputState.NORMAL);
            }
            return;
        }

        // Get storage anchor
        StorageAnchorBlockEntity anchor = getPairedAnchor(level);
        if (anchor == null) {
            if (currentState != ItemInputState.BLOCKED) {
                updateBlockState(level, pos, state, ItemInputState.BLOCKED);
            }
            applyBackoff();
            return;
        }

        // Get destinations
        List<StorageDestination> destinations = anchor.getAvailableDestinations();
        if (destinations.isEmpty()) {
            if (currentState != ItemInputState.BLOCKED) {
                updateBlockState(level, pos, state, ItemInputState.BLOCKED);
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
            if (currentState != ItemInputState.WORKING) {
                updateBlockState(level, pos, state, ItemInputState.WORKING);
            }
        } else if (!buffer.isEmpty()) {
            if (currentState != ItemInputState.BLOCKED) {
                updateBlockState(level, pos, state, ItemInputState.BLOCKED);
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

    /**
     * Updates the block state's ItemInputState property.
     */
    private void updateBlockState(Level level, BlockPos pos, BlockState state, ItemInputState newState) {
        level.setBlock(pos, state.setValue(STATE, newState), Block.UPDATE_ALL);
    }

    // ==================== BLOCK REMOVAL ====================

    /**
     * Drops buffer contents and releases claimed items when block is removed.
     */
    public void onRemoved() {
        if (level == null || level.isClientSide()) {
            return;
        }

        // Drop buffer contents
        List<ItemStack> drops = buffer.getContentsForDrop();
        if (!drops.isEmpty()) {
            SimpleContainer container = new SimpleContainer(drops.size());
            for (int i = 0; i < drops.size(); i++) {
                container.setItem(i, drops.get(i));
            }
            Containers.dropContents(level, worldPosition, container);
        }

        // Release claimed items
        int radius = Config.collectRadius;
        AABB scanArea = new AABB(
                worldPosition.getX() - radius, worldPosition.getY() - radius, worldPosition.getZ() - radius,
                worldPosition.getX() + radius + 1, worldPosition.getY() + radius + 1, worldPosition.getZ() + radius + 1
        );

        List<ItemEntity> nearbyItems = level.getEntitiesOfClass(ItemEntity.class, scanArea);
        for (ItemEntity itemEntity : nearbyItems) {
            ClaimedItemData claimData = itemEntity.getData(FloraFaunaRegistry.CLAIMED_ITEM_DATA);
            if (claimData.claimed() && claimData.itemInputPos().equals(worldPosition)) {
                // Reset claim and pickup delay
                itemEntity.setData(FloraFaunaRegistry.CLAIMED_ITEM_DATA, ClaimedItemData.DEFAULT);
                itemEntity.setPickUpDelay(0);
            }
        }

        // Unpair from anchor
        unpairAnchor();
    }

    // ==================== SERIALIZATION ====================

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        buffer.serialize(output);

        if (pairedAnchorPos != null) {
            output.store(KEY_PAIRED_ANCHOR, BlockPos.CODEC, pairedAnchorPos);
        }

        output.putInt(KEY_BACKOFF_TICKS, currentBackoff);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        buffer.deserialize(input);

        pairedAnchorPos = input.read(KEY_PAIRED_ANCHOR, BlockPos.CODEC).orElse(null);
        currentBackoff = input.getIntOr(KEY_BACKOFF_TICKS, 0);
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

    // ==================== ACCESSORS ====================

    public ItemInputBuffer getBuffer() {
        return buffer;
    }

    @Nullable
    public BlockPos getPairedAnchorPos() {
        return pairedAnchorPos;
    }

    public boolean isPaired() {
        return pairedAnchorPos != null;
    }
}
