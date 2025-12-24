package net.j40climb.florafauna.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Utility class for rendering player inventory and hotbar in custom screens.
 * Provides reusable methods for consistent inventory rendering across all mod screens.
 */
public class InventoryRenderHelper {
    // Standard Minecraft inventory dimensions
    private static final int INVENTORY_ROWS = 3;
    private static final int INVENTORY_COLS = 9;
    private static final int HOTBAR_SLOTS = 9;
    private static final int SLOT_SIZE = 18;
    private static final int HOTBAR_SPACING = 4; // Space between inventory and hotbar

    // Slot rendering colors
    private static final int SLOT_BACKGROUND_COLOR = 0x8B000000; // Semi-transparent black
    private static final int SLOT_BORDER_COLOR = 0xFF8B8B8B; // Light gray border

    /**
     * Renders the full player inventory (3x9 grid) and hotbar (1x9 grid) centered at the given position.
     *
     * @param guiGraphics the graphics context
     * @param font the font renderer
     * @param inventory the player's inventory
     * @param centerX the center X position for the inventory
     * @param startY the top Y position for the inventory
     */
    public static void renderPlayerInventory(GuiGraphics guiGraphics, Font font, Inventory inventory, int centerX, int startY) {
        renderInventoryGrid(guiGraphics, font, inventory, centerX, startY);
        int hotbarY = startY + (INVENTORY_ROWS * SLOT_SIZE) + HOTBAR_SPACING;
        renderHotbar(guiGraphics, font, inventory, centerX, hotbarY);
    }

    /**
     * Renders just the inventory grid (3x9), excluding the hotbar.
     *
     * @param guiGraphics the graphics context
     * @param font the font renderer
     * @param inventory the player's inventory
     * @param centerX the center X position
     * @param startY the top Y position
     */
    public static void renderInventoryGrid(GuiGraphics guiGraphics, Font font, Inventory inventory, int centerX, int startY) {
        for (int row = 0; row < INVENTORY_ROWS; row++) {
            for (int col = 0; col < INVENTORY_COLS; col++) {
                // Inventory slots start at index 9 (after hotbar)
                int slotIndex = col + (row + 1) * INVENTORY_COLS;
                int x = centerX - (INVENTORY_COLS * SLOT_SIZE / 2) + col * SLOT_SIZE;
                int y = startY + row * SLOT_SIZE;

                renderSlot(guiGraphics, font, inventory.getItem(slotIndex), x, y);
            }
        }
    }

    /**
     * Renders just the hotbar (1x9).
     *
     * @param guiGraphics the graphics context
     * @param font the font renderer
     * @param inventory the player's inventory
     * @param centerX the center X position
     * @param startY the top Y position
     */
    public static void renderHotbar(GuiGraphics guiGraphics, Font font, Inventory inventory, int centerX, int startY) {
        for (int col = 0; col < HOTBAR_SLOTS; col++) {
            int x = centerX - (HOTBAR_SLOTS * SLOT_SIZE / 2) + col * SLOT_SIZE;
            renderSlot(guiGraphics, font, inventory.getItem(col), x, startY);
        }
    }

    /**
     * Renders a single inventory slot with background and item.
     *
     * @param guiGraphics the graphics context
     * @param font the font renderer
     * @param itemStack the item to render in the slot
     * @param x the X position
     * @param y the Y position
     */
    public static void renderSlot(GuiGraphics guiGraphics, Font font, ItemStack itemStack, int x, int y) {
        // Draw slot background (16x16 for the item)
        guiGraphics.fill(x, y, x + 16, y + 16, SLOT_BACKGROUND_COLOR);

        // Draw item if present
        if (!itemStack.isEmpty()) {
            guiGraphics.renderItem(itemStack, x, y);
            guiGraphics.renderItemDecorations(font, itemStack, x, y);
        }
    }

    /**
     * Calculates the total height needed for inventory + hotbar rendering.
     *
     * @return total height in pixels
     */
    public static int getInventoryHeight() {
        return (INVENTORY_ROWS * SLOT_SIZE) + HOTBAR_SPACING + SLOT_SIZE;
    }

    /**
     * Calculates the total width needed for inventory rendering.
     *
     * @return total width in pixels
     */
    public static int getInventoryWidth() {
        return INVENTORY_COLS * SLOT_SIZE;
    }

    /**
     * Gets the recommended Y position for inventory rendering at the bottom of the screen.
     *
     * @param screenHeight the screen height
     * @return Y position for inventory
     */
    public static int getBottomInventoryY(int screenHeight) {
        return screenHeight - getInventoryHeight() - 10; // 10px padding from bottom
    }
}
