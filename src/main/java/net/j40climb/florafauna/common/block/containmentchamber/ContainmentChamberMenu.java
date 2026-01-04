package net.j40climb.florafauna.common.block.containmentchamber;

import net.j40climb.florafauna.client.gui.BaseContainerMenu;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public class ContainmentChamberMenu extends BaseContainerMenu {
    public final ContainmentChamberBlockEntity blockEntity;
    private final Level level;


    /**
     * Client-side constructor (called from packet).
     */
    public ContainmentChamberMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, playerInventory.player.level().getBlockEntity(buffer.readBlockPos()), new SimpleContainerData(2));

    }

    /**
     * Server-side constructor (called when opening menu).
 */
    public ContainmentChamberMenu(int containerId, Inventory playerInventory, BlockEntity entity, ContainerData data) {
        super(FloraFaunaRegistry.CONTAINMENT_CHAMBER_MENU.get(), containerId);

        this.blockEntity = ((ContainmentChamberBlockEntity) entity);
        this.level = playerInventory.player.level();

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);

        addSlot(new ResourceHandlerSlot(blockEntity.handler, blockEntity.handler::set, 0, 54, 34 ));
        addSlot(new ResourceHandlerSlot(blockEntity.handler, blockEntity.handler::set, 1, 104, 34 ));

        addDataSlots(data);
    }

    // Custom slot indices (after player inventory/hotbar from BaseContainerMenu)
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;
    private static final int TE_INVENTORY_SLOT_COUNT = 2;

    @Override
    public ItemStack quickMoveStack(Player playerIn, int pIndex) {
        Slot sourceSlot = slots.get(pIndex);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;  //EMPTY_ITEM
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // Check if the slot clicked is one of the vanilla container slots
        if (pIndex < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // This is a vanilla container slot so merge the stack into the tile inventory
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX
                    + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;  // EMPTY_ITEM
            }
        } else if (pIndex < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // This is a TE slot so merge the stack into the players inventory
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            System.out.println("Invalid slotIndex:" + pIndex);
            return ItemStack.EMPTY;
        }
        // If stack size == 0 (the entire stack was moved) set slot contents to null
        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, FloraFaunaRegistry.SYMBIOTE_CONTAINMENT_CHAMBER.get());
    }
}
