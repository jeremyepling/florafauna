package net.j40climb.florafauna.common.block.vacuum;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.common.block.iteminput.rootiteminput.networking.ItemInputAnimationPayload;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
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
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Abstract base class for vacuum-type block entities that collect nearby items.
 * Handles the collection and absorption phases of item processing.
 *
 * Item Lifecycle: FREE -> CLAIMED -> ABSORBED -> BUFFERED
 *
 * Subclasses can override behavior or add additional processing phases.
 */
public abstract class AbstractVacuumBlockEntity extends BlockEntity {
    public static final EnumProperty<VacuumState> STATE = EnumProperty.create("state", VacuumState.class);

    // Buffer for collected items
    protected final ItemBuffer buffer;

    // Tick cooldowns
    private int collectCooldown = 0;

    protected AbstractVacuumBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState, int bufferCapacity) {
        super(type, pos, blockState);
        this.buffer = new ItemBuffer(bufferCapacity);
    }

    protected AbstractVacuumBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        this(type, pos, blockState, Config.maxBufferedStacks);
    }

    /**
     * Main tick handler called by the block's ticker.
     * Subclasses can override to add additional phases.
     */
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }

        // Run collection phase
        tickCollection(level, pos);

        // Run absorption phase
        tickAbsorption(level);

        // Subclasses implement additional processing
        tickProcessing(level, pos, state);
    }

    /**
     * Called after collection and absorption. Subclasses implement their specific processing.
     * For ItemInput, this is transfer to StorageAnchor.
     * For MiningAnchor, this is overflow to pods.
     */
    protected abstract void tickProcessing(Level level, BlockPos pos, BlockState state);

    // ==================== COLLECTION PHASE ====================

    /**
     * Returns the collection radius for this vacuum block.
     * Override to customize per block type.
     */
    protected int getCollectRadius() {
        return Config.collectRadius;
    }

    /**
     * Returns the collection interval in ticks.
     * Override to customize per block type.
     */
    protected int getCollectInterval() {
        return Config.collectIntervalTicks;
    }

    /**
     * Returns the maximum entities to claim per collection cycle.
     */
    protected int getMaxEntitiesPerCollect() {
        return Config.maxItemEntitiesPerCollect;
    }

    /**
     * Returns the maximum items to claim per collection cycle.
     */
    protected int getMaxItemsPerCollect() {
        return Config.maxItemsPerCollect;
    }

    /**
     * Returns the animation duration for claimed items in ticks.
     */
    protected int getAnimationDuration() {
        return Config.animationDurationTicks;
    }

    /**
     * Returns whether this vacuum block should only collect block drops.
     * Override in subclasses to enable block-drop-only mode.
     *
     * @return true to only collect items that came from block breaking
     */
    protected boolean collectBlockDropsOnly() {
        return false; // Default: collect all items
    }

    /**
     * Determines if an item entity should be collected.
     * Subclasses can override to add additional filtering logic.
     *
     * @param itemEntity The item entity to check
     * @return true if this item should be collected
     */
    protected boolean shouldCollectItem(ItemEntity itemEntity) {
        // If block drops only mode is enabled, filter by BlockDropData
        if (collectBlockDropsOnly()) {
            BlockDropData dropData = itemEntity.getData(FloraFaunaRegistry.BLOCK_DROP_DATA);
            if (!dropData.isBlockDrop()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Scans for nearby item entities and claims them.
     */
    protected void tickCollection(Level level, BlockPos pos) {
        collectCooldown--;
        if (collectCooldown > 0) {
            return;
        }
        collectCooldown = getCollectInterval();

        // Don't collect if buffer is full
        if (buffer.isFull()) {
            return;
        }

        int radius = getCollectRadius();
        AABB scanArea = new AABB(
                pos.getX() - radius, pos.getY() - radius, pos.getZ() - radius,
                pos.getX() + radius + 1, pos.getY() + radius + 1, pos.getZ() + radius + 1
        );

        List<ItemEntity> nearbyItems = level.getEntitiesOfClass(ItemEntity.class, scanArea);

        int claimedEntities = 0;
        int claimedItems = 0;

        for (ItemEntity itemEntity : nearbyItems) {
            if (claimedEntities >= getMaxEntitiesPerCollect()) {
                break;
            }
            if (claimedItems >= getMaxItemsPerCollect()) {
                break;
            }

            // Skip already claimed items
            ClaimedItemData existingData = itemEntity.getData(FloraFaunaRegistry.CLAIMED_ITEM_DATA);
            if (existingData.claimed()) {
                continue;
            }

            // Apply subclass filter
            if (!shouldCollectItem(itemEntity)) {
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
                getAnimationDuration()
        );
        itemEntity.setData(FloraFaunaRegistry.CLAIMED_ITEM_DATA, claimData);

        // Send animation payload to nearby clients
        if (level instanceof ServerLevel serverLevel) {
            ItemInputAnimationPayload payload = new ItemInputAnimationPayload(
                    itemEntity.getId(),
                    worldPosition,
                    getAnimationDuration()
            );
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, level.getChunk(worldPosition).getPos(), payload);
        }
    }

    // ==================== ABSORPTION PHASE ====================

    /**
     * Checks claimed items and absorbs those whose animation has completed.
     */
    protected void tickAbsorption(Level level) {
        int radius = getCollectRadius();
        AABB scanArea = new AABB(
                worldPosition.getX() - radius, worldPosition.getY() - radius, worldPosition.getZ() - radius,
                worldPosition.getX() + radius + 1, worldPosition.getY() + radius + 1, worldPosition.getZ() + radius + 1
        );

        List<ItemEntity> nearbyItems = level.getEntitiesOfClass(ItemEntity.class, scanArea);
        long currentTick = level.getGameTime();

        for (ItemEntity itemEntity : nearbyItems) {
            ClaimedItemData claimData = itemEntity.getData(FloraFaunaRegistry.CLAIMED_ITEM_DATA);

            // Only process items claimed by this block
            if (!claimData.claimed() || !claimData.vacuumBlockPos().equals(worldPosition)) {
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
            // Mark as changed for saving
            setChanged();
        }

        // Remove the entity (even if we couldn't add everything - safety rule)
        itemEntity.discard();
    }

    // ==================== STATE MANAGEMENT ====================

    /**
     * Updates the block state's VacuumState property.
     */
    protected void updateBlockState(Level level, BlockPos pos, BlockState state, VacuumState newState) {
        if (state.hasProperty(STATE)) {
            level.setBlock(pos, state.setValue(STATE, newState), Block.UPDATE_ALL);
        }
    }

    /**
     * Gets the current vacuum state from the block state.
     */
    protected VacuumState getCurrentState(BlockState state) {
        return state.hasProperty(STATE) ? state.getValue(STATE) : VacuumState.NORMAL;
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
        int radius = getCollectRadius();
        AABB scanArea = new AABB(
                worldPosition.getX() - radius, worldPosition.getY() - radius, worldPosition.getZ() - radius,
                worldPosition.getX() + radius + 1, worldPosition.getY() + radius + 1, worldPosition.getZ() + radius + 1
        );

        List<ItemEntity> nearbyItems = level.getEntitiesOfClass(ItemEntity.class, scanArea);
        for (ItemEntity itemEntity : nearbyItems) {
            ClaimedItemData claimData = itemEntity.getData(FloraFaunaRegistry.CLAIMED_ITEM_DATA);
            if (claimData.claimed() && claimData.vacuumBlockPos().equals(worldPosition)) {
                // Reset claim and pickup delay
                itemEntity.setData(FloraFaunaRegistry.CLAIMED_ITEM_DATA, ClaimedItemData.DEFAULT);
                itemEntity.setPickUpDelay(0);
            }
        }
    }

    // ==================== SERIALIZATION ====================

    private static final String KEY_BUFFER = "vacuum_buffer";

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        buffer.serialize(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        buffer.deserialize(input);
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

    public ItemBuffer getBuffer() {
        return buffer;
    }
}
