package net.j40climb.florafauna.common.block.cocoonchamber;

import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for the Cocoon Chamber.
 * This is a minimal block entity - it just provides MenuProvider functionality.
 * No inventory, no ticking - the chamber just opens a menu with action buttons.
 */
public class CocoonChamberBlockEntity extends BlockEntity implements MenuProvider {

    public CocoonChamberBlockEntity(BlockPos pos, BlockState blockState) {
        super(FloraFaunaRegistry.COCOON_CHAMBER_BE.get(), pos, blockState);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("gui.florafauna.cocoon.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CocoonChamberMenu(containerId, playerInventory, getBlockPos());
    }
}
