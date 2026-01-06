package net.j40climb.florafauna.common.block.mininganchor.pod;

import net.j40climb.florafauna.common.block.mininganchor.AbstractMiningAnchorBlockEntity;
import net.j40climb.florafauna.common.block.vacuum.ItemBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Abstract base class for storage pods that attach to Mining Anchors.
 * Pods provide additional storage capacity for the anchor system.
 * Each pod tier defines its own slot count (capacity).
 */
public abstract class AbstractStoragePodBlockEntity extends BlockEntity {

    protected static final String TAG_PARENT_ANCHOR = "ParentAnchor";

    protected final ItemBuffer podBuffer;
    protected final int slotCount;
    @Nullable
    protected BlockPos parentAnchorPos;

    /**
     * Creates a pod with the specified slot count.
     * @param type The block entity type
     * @param pos The block position
     * @param state The block state
     * @param slotCount Number of inventory slots (capacity = slotCount * 64)
     */
    public AbstractStoragePodBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slotCount) {
        super(type, pos, state);
        this.slotCount = slotCount;
        this.podBuffer = new ItemBuffer(slotCount);
    }

    /**
     * Returns the number of slots in this pod.
     */
    public int getSlotCount() {
        return slotCount;
    }

    /**
     * Returns the maximum capacity of this pod in items.
     * Each slot can hold up to 64 items.
     */
    public int getCapacity() {
        return slotCount * 64;
    }

    /**
     * Returns the number of items currently stored.
     */
    public int getStoredCount() {
        return podBuffer.getItemCount();
    }

    /**
     * Returns whether the pod is full (reached max item capacity or all slots full).
     */
    public boolean isFull() {
        return podBuffer.isFull() || podBuffer.getItemCount() >= getCapacity();
    }

    /**
     * Returns whether the pod is empty.
     */
    public boolean isEmpty() {
        return podBuffer.isEmpty();
    }

    /**
     * Attempts to add items to this pod.
     * @param stack The items to add
     * @return The number of items that were added
     */
    public int addItems(ItemStack stack) {
        int added = podBuffer.add(stack);
        if (added > 0) {
            setChanged();
        }
        return added;
    }

    /**
     * Sets the parent anchor position.
     */
    public void setParentAnchor(@Nullable BlockPos anchorPos) {
        this.parentAnchorPos = anchorPos;
        setChanged();
    }

    /**
     * Gets the parent anchor position.
     */
    @Nullable
    public BlockPos getParentAnchorPos() {
        return parentAnchorPos;
    }

    /**
     * Gets the pod's buffer for direct access.
     */
    public ItemBuffer getBuffer() {
        return podBuffer;
    }

    /**
     * Gets the item handler for capability registration.
     * Creates a new handler each time - it's lightweight since it delegates to the buffer.
     */
    public PodItemHandler getItemHandler() {
        return new PodItemHandler(this);
    }

    /**
     * Called when the pod block is broken.
     * Subclasses implement specific break behavior (spill vs. keep items).
     */
    protected abstract void onBlockBroken(Level level, BlockPos pos, @Nullable Player player);

    /**
     * Called when the pod is removed from the world.
     * Notifies the parent anchor if one exists.
     */
    public void onRemoved() {
        if (parentAnchorPos != null && level != null) {
            BlockEntity be = level.getBlockEntity(parentAnchorPos);
            if (be instanceof AbstractMiningAnchorBlockEntity anchor) {
                anchor.onPodRemoved(worldPosition);
            }
        }
    }

    /**
     * Marks the pod as changed and notifies clients.
     * Call this after modifying the buffer contents.
     */
    public void markChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        podBuffer.serialize(output);
        if (parentAnchorPos != null) {
            output.store(TAG_PARENT_ANCHOR, BlockPos.CODEC, parentAnchorPos);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        podBuffer.deserialize(input);
        Optional<BlockPos> anchorPos = input.read(TAG_PARENT_ANCHOR, BlockPos.CODEC);
        parentAnchorPos = anchorPos.orElse(null);
    }

    // ==================== CLIENT SYNC ====================

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
