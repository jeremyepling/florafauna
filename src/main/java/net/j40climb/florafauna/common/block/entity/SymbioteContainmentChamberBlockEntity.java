package net.j40climb.florafauna.common.block.entity;

import net.j40climb.florafauna.client.screen.custom.ContainmentChamberMenu;
import net.j40climb.florafauna.common.item.custom.SymbioteItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for the Symbiote Containment Chamber.
 * Stores a symbiote item and feeding items to unlock abilities on the symbiote.
 * Slot 0: Symbiote item
 * Slots 1-9: Feeding items
 */
public class SymbioteContainmentChamberBlockEntity extends BlockEntity implements MenuProvider {
    public final ItemStacksResourceHandler handler = new ItemStacksResourceHandler(2) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            // Slot 0 only accepts SymbioteItem
            if (index == 0) {
                return resource.getItem() instanceof SymbioteItem;
            }
            return true;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            super.onContentsChanged(index, previousContents);
            setChanged();
            assert level != null;
            if (!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        protected int getCapacity(int index, ItemResource resource) {
            if (index == 0) {
                return 1;
            }
            return 64;
        }
    };

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 72;

    public SymbioteContainmentChamberBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.SYMBIOTE_CONTAINMENT_CHAMBER.get(), pos, blockState);
        data = new ContainerData() {
            @Override
            public int get(int i) {
                return switch (i) {
                    case 0 -> SymbioteContainmentChamberBlockEntity.this.progress;
                    case 1 -> SymbioteContainmentChamberBlockEntity.this.maxProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int i, int value) {
                switch (i) {
                    case 0: SymbioteContainmentChamberBlockEntity.this.progress = value;
                    case 1: SymbioteContainmentChamberBlockEntity.this.maxProgress = value;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.florafauna.containment_chamber");

    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainmentChamberMenu(containerId, playerInventory, this, this.data);
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(this.handler.size());
        for (int i = 0; i < this.handler.size(); i++) {
            int amount = this.handler.getAmountAsInt(i);
            inventory.setItem(i, this.handler.getResource(i).toStack(amount));
        }

        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        drops();
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        handler.serialize(output);
        output.putInt("growth_chamber.progress", progress);
        output.putInt("growth_chamber.max_progress", maxProgress);

        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        handler.deserialize(input);
        progress = input.getIntOr("growth_chamber.progress", 0);
        maxProgress = input.getIntOr("growth_chamber.max_progress", 0);
    }

    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
//        if(hasRecipe()) {
//            increaseCraftingProgress();
//            setChanged(level, blockPos, blockState);
//
//            if(hasCraftingFinished()) {
//                craftItem();
//                resetProgress();
//            }
//        } else {
//            resetProgress();
//        }
    }

    private void resetProgress() {
        progress = 0;
        maxProgress = 72;
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