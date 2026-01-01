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
    private static final int SLOT_SIZE = 18; // Slot spacing (18 pixels between slot positions)
    private static final int HOTBAR_Y_OFFSET = 58; // Y offset from inventory start to hotbar (matches vanilla)

    private static final int SLOT_HOVER_COLOR = 0x80FFFFFF; // Semi-transparent white for hover

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
        int hotbarY = startY + HOTBAR_Y_OFFSET;
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
     * Renders a single inventory slot with item.
     * X and Y should be the slot position where the item will be rendered.
     *
     * @param guiGraphics the graphics context
     * @param font the font renderer
     * @param itemStack the item to render in the slot
     * @param x the X position where the item should be rendered
     * @param y the Y position where the item should be rendered
     */
    public static void renderSlot(GuiGraphics guiGraphics, Font font, ItemStack itemStack, int x, int y) {
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
        return HOTBAR_Y_OFFSET + SLOT_SIZE;
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

    /**
     * Renders the player inventory with hover support.
     *
     * @param guiGraphics the graphics context
     * @param font the font renderer
     * @param inventory the player's inventory
     * @param centerX the center X position
     * @param startY the top Y position
     * @param mouseX the mouse X position
     * @param mouseY the mouse Y position
     * @return the ItemStack being hovered, or ItemStack.EMPTY if none
     */
    public static ItemStack renderPlayerInventoryWithHover(GuiGraphics guiGraphics, Font font, Inventory inventory, int centerX, int startY, int mouseX, int mouseY) {
        ItemStack hoveredItem = renderInventoryGridWithHover(guiGraphics, font, inventory, centerX, startY, mouseX, mouseY);

        int hotbarY = startY + HOTBAR_Y_OFFSET;
        ItemStack hotbarHovered = renderHotbarWithHover(guiGraphics, font, inventory, centerX, hotbarY, mouseX, mouseY);

        return hoveredItem.isEmpty() ? hotbarHovered : hoveredItem;
    }

    /**
     * Renders the inventory grid with hover support.
     *
     * @return the ItemStack being hovered, or ItemStack.EMPTY if none
     */
    public static ItemStack renderInventoryGridWithHover(GuiGraphics guiGraphics, Font font, Inventory inventory, int centerX, int startY, int mouseX, int mouseY) {
        ItemStack hoveredItem = ItemStack.EMPTY;

        for (int row = 0; row < INVENTORY_ROWS; row++) {
            for (int col = 0; col < INVENTORY_COLS; col++) {
                int slotIndex = col + (row + 1) * INVENTORY_COLS;
                int x = centerX - (INVENTORY_COLS * SLOT_SIZE / 2) + col * SLOT_SIZE;
                int y = startY + row * SLOT_SIZE;

                ItemStack itemStack = inventory.getItem(slotIndex);
                // Check hover over the item area (16x16 pixels)
                boolean isHovered = isMouseOver(mouseX, mouseY, x, y, 16, 16);

                renderSlotWithHover(guiGraphics, font, itemStack, x, y, isHovered);

                if (isHovered && !itemStack.isEmpty()) {
                    hoveredItem = itemStack;
                }
            }
        }

        return hoveredItem;
    }

    /**
     * Renders the hotbar with hover support.
     *
     * @return the ItemStack being hovered, or ItemStack.EMPTY if none
     */
    public static ItemStack renderHotbarWithHover(GuiGraphics guiGraphics, Font font, Inventory inventory, int centerX, int startY, int mouseX, int mouseY) {
        ItemStack hoveredItem = ItemStack.EMPTY;

        for (int col = 0; col < HOTBAR_SLOTS; col++) {
            int x = centerX - (HOTBAR_SLOTS * SLOT_SIZE / 2) + col * SLOT_SIZE;
            ItemStack itemStack = inventory.getItem(col);
            // Check hover over the item area (16x16 pixels)
            boolean isHovered = isMouseOver(mouseX, mouseY, x, startY, 16, 16);

            renderSlotWithHover(guiGraphics, font, itemStack, x, startY, isHovered);

            if (isHovered && !itemStack.isEmpty()) {
                hoveredItem = itemStack;
            }
        }

        return hoveredItem;
    }

    /**
     * Renders a single slot with hover highlight.
     */
    public static void renderSlotWithHover(GuiGraphics guiGraphics, Font font, ItemStack itemStack, int x, int y, boolean isHovered) {
        // Draw item
        if (!itemStack.isEmpty()) {
            guiGraphics.renderItem(itemStack, x, y);
            guiGraphics.renderItemDecorations(font, itemStack, x, y);
        }

        // Draw hover highlight over the item area (16x16)
        if (isHovered) {
            guiGraphics.fill(x, y, x + 16, y + 16, SLOT_HOVER_COLOR);
        }
    }

    /**
     * Checks if the mouse is over a rectangular area.
     */
    public static boolean isMouseOver(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
