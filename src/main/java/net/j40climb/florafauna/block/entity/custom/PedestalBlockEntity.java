package net.j40climb.florafauna.block.entity.custom;

import net.j40climb.florafauna.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class PedestalBlockEntity extends BlockEntity implements Container {
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(1, ItemStack.EMPTY);
    private float rotation;

    public PedestalBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.PEDESTAL_BE.get(), pPos, pBlockState);
    }
    @Override
    public int getContainerSize() {
        return inventory.size();
    }
    @Override
    public boolean isEmpty() {
        for(int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if(!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }
    @Override
    public ItemStack getItem(int pSlot) {
        setChanged();
        return inventory.get(pSlot);
    }
    @Override
    public ItemStack removeItem(int pSlot, int pAmount) {
        setChanged();
        ItemStack stack = inventory.get(pSlot);
        stack.shrink(pAmount);
        return inventory.set(pSlot, stack);
    }
    @Override
    public ItemStack removeItemNoUpdate(int pSlot) {
        setChanged();
        return ContainerHelper.takeItem(inventory, pSlot);
    }
    @Override
    public void setItem(int pSlot, ItemStack pStack) {
        setChanged();
        inventory.set(pSlot, pStack.copyWithCount(1));
    }
    @Override
    public boolean stillValid(Player pPlayer) {
        return Container.stillValidBlockEntity(this, pPlayer);
    }
    @Override
    public void clearContent() {
        inventory.clear();
    }
    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        ContainerHelper.saveAllItems(pTag, inventory, pRegistries);
    }
    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        ContainerHelper.loadAllItems(pTag, inventory, pRegistries);
    }

    public float getRenderingRotation() {
        assert level != null;
        long time = level.getGameTime(); // Get the world time to sync up all pedestal rotations
        float speedMultiplier = 1f; // Adjust this value to control speed, where 1.0f is normal speed
        return (time * speedMultiplier) % 360;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return saveWithoutMetadata(pRegistries);
    }
}