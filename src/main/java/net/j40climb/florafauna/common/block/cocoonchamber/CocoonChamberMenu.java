package net.j40climb.florafauna.common.block.cocoonchamber;

import net.j40climb.florafauna.setup.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Menu for the Cocoon Chamber.
 * This is a button-only menu with no inventory slots.
 * Actions are handled via network packets sent from the screen.
 */
public class CocoonChamberMenu extends AbstractContainerMenu {
    private final BlockPos chamberPos;
    private final Level level;

    /**
     * Client-side constructor (called from network packet).
     */
    public CocoonChamberMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, buffer.readBlockPos());
    }

    /**
     * Server-side constructor (called when opening menu).
     */
    public CocoonChamberMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModRegistry.COCOON_CHAMBER_MENU.get(), containerId);
        this.chamberPos = pos;
        this.level = playerInventory.player.level();
    }

    /**
     * Gets the position of the Cocoon Chamber block.
     * Used by the screen to send action packets.
     */
    public BlockPos getChamberPos() {
        return chamberPos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // No slots to move
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return ContainerLevelAccess.create(level, chamberPos)
                .evaluate((lvl, pos) -> lvl.getBlockState(pos).getBlock() instanceof CocoonChamberBlock
                        && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0, true);
    }
}
