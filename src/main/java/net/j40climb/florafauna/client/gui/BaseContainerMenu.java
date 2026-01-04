package net.j40climb.florafauna.client.gui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for container menus WITH player inventory slots.
 * Provides standard slot setup helpers for the player inventory and hotbar.
 * <p>
 * <h2>When to use a Menu:</h2>
 * <ul>
 *   <li><b>Use BaseContainerMenu</b> - When your GUI has item slots that need server sync.
 *       Call {@link #addPlayerInventory} and {@link #addPlayerHotbar} in constructor.
 *       Pair with {@link BaseContainerScreen}.</li>
 *   <li><b>Don't use a Menu</b> - For button-only GUIs (config screens, action buttons).
 *       Use {@link BaseScreen} instead and handle server communication via packets.</li>
 * </ul>
 * <p>
 * The Menu system exists for <b>slot synchronization</b>. If you have no slots,
 * using a Menu adds unnecessary overhead.
 *
 * @see BaseContainerScreen for the corresponding screen base class
 * @see BaseScreen for screens WITHOUT inventory slots (no menu needed)
 */
public abstract class BaseContainerMenu extends AbstractContainerMenu {

    // Standard slot counts
    protected static final int HOTBAR_SLOT_COUNT = 9;
    protected static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    protected static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    protected static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    protected static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    protected static final int VANILLA_FIRST_SLOT_INDEX = 0;

    // Standard positioning (matches BaseContainerScreen constants)
    protected static final int INVENTORY_X = 8;
    protected static final int INVENTORY_Y = 84;
    protected static final int HOTBAR_Y = 142;
    protected static final int SLOT_SIZE = 18;

    protected BaseContainerMenu(@Nullable MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    /**
     * Adds the player's main inventory (3x9 grid) at the standard position (8, 84).
     * Call this in your constructor after setting up custom slots.
     */
    protected void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < PLAYER_INVENTORY_ROW_COUNT; row++) {
            for (int col = 0; col < PLAYER_INVENTORY_COLUMN_COUNT; col++) {
                int slotIndex = col + row * PLAYER_INVENTORY_COLUMN_COUNT + HOTBAR_SLOT_COUNT;
                int x = INVENTORY_X + col * SLOT_SIZE;
                int y = INVENTORY_Y + row * SLOT_SIZE;
                addSlot(new Slot(playerInventory, slotIndex, x, y));
            }
        }
    }

    /**
     * Adds the player's hotbar (1x9 grid) at the standard position (8, 142).
     * Call this in your constructor after setting up custom slots.
     */
    protected void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < HOTBAR_SLOT_COUNT; col++) {
            int x = INVENTORY_X + col * SLOT_SIZE;
            addSlot(new Slot(playerInventory, col, x, HOTBAR_Y));
        }
    }
}
