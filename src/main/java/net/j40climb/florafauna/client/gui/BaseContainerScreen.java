package net.j40climb.florafauna.client.gui;

import net.j40climb.florafauna.FloraFauna;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Base class for screens WITH inventory slots that need server synchronization.
 * Uses the standard 176x166 Minecraft GUI texture format with inventory at the bottom.
 * <p>
 * <h2>When to use BaseContainerScreen vs BaseScreen:</h2>
 * <ul>
 *   <li><b>BaseContainerScreen</b> - GUIs with item slots (storage, crafting, machines).
 *       Requires a Menu class extending {@link BaseContainerMenu}.
 *       Opens via {@code player.openMenu()} on server side.
 *       Examples: Chests, Furnaces, ContainmentChamberScreen.</li>
 *   <li><b>BaseScreen</b> - Button-only GUIs, config screens, no item slots.
 *       Opens directly on client via {@code Minecraft.getInstance().setScreen()}.
 *       Examples: Sign editing, MobBarrierConfigScreen, CocoonChamberScreen.</li>
 * </ul>
 * <p>
 * The Menu system exists for <b>slot synchronization</b>. Only use it when you have
 * inventory slots that need client-server sync.
 *
 * @see BaseScreen for screens WITHOUT inventory slots
 * @see BaseContainerMenu for the corresponding menu base class
 */
public abstract class BaseContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    // Default GUI texture with empty top area and standard inventory/hotbar slots
    protected static final Identifier DEFAULT_GUI_TEXTURE =
            Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "textures/gui/inventory_hotbar_base_gui.png");

    // Standard Minecraft GUI dimensions (inherited from AbstractContainerScreen as imageWidth/imageHeight)
    // These constants are provided for reference and use in subclasses
    public static final int STANDARD_IMAGE_WIDTH = 176;
    public static final int STANDARD_IMAGE_HEIGHT = 166;

    // Vanilla inventory positioning (from top-left of GUI)
    public static final int INVENTORY_X = 8;
    public static final int INVENTORY_Y = 84;

    // Content area bounds (area above inventory for custom content)
    public static final int CONTENT_X = 8;
    public static final int CONTENT_Y = 18;
    public static final int CONTENT_WIDTH = 160;
    public static final int CONTENT_HEIGHT = 62; // Space between title and inventory

    // Title positioning
    public static final int TITLE_Y = 6;

    // GUI texture to use
    protected final Identifier guiTexture;

    /**
     * Creates a container screen with the default GUI texture.
     */
    protected BaseContainerScreen(T menu, Inventory playerInventory, Component title) {
        this(menu, playerInventory, title, DEFAULT_GUI_TEXTURE);
    }

    /**
     * Creates a container screen with a custom GUI texture.
     *
     * @param guiTexture the texture to use (should be 176x166 with standard slot layout)
     */
    protected BaseContainerScreen(T menu, Inventory playerInventory, Component title, Identifier guiTexture) {
        super(menu, playerInventory, title);
        this.guiTexture = guiTexture;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, guiTexture, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /**
     * Gets the X position for content relative to the GUI.
     */
    protected int getContentX() {
        return leftPos + CONTENT_X;
    }

    /**
     * Gets the Y position for content relative to the GUI.
     */
    protected int getContentY() {
        return topPos + CONTENT_Y;
    }
}
