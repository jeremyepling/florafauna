package net.j40climb.florafauna.client.gui;

import net.j40climb.florafauna.FloraFauna;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Base class for screens that display the standard GUI layout but blank.
 * Uses the standard 176x166 Minecraft GUI texture format.
 * <p>
 * Subclasses can customize the content area at the top of the GUI (above the inventory).
 */
public abstract class BaseInventoryScreen extends Screen {
    // Default GUI texture with empty top area
    protected static final Identifier DEFAULT_GUI_TEXTURE =
            Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "textures/gui/base_gui.png");

    // Standard Minecraft GUI dimensions
    protected static final int IMAGE_WIDTH = 176;
    protected static final int IMAGE_HEIGHT = 166;

    // Content area bounds (area above inventory for custom content)
    protected static final int CONTENT_X = 8;
    protected static final int CONTENT_Y = 18;
    protected static final int CONTENT_WIDTH = 160;
    protected static final int CONTENT_HEIGHT = 166; // Space between title and inventory

    // Title positioning
    protected static final int TITLE_Y = 6;

    // GUI texture to use (can be overridden by subclass)
    protected final Identifier guiTexture;

    // GUI position (calculated in init)
    protected int leftPos;
    protected int topPos;

    /**
     * Creates a screen with the default GUI texture.
     *
     * @param title the screen title
     */
    protected BaseInventoryScreen(Component title) {
        this(title, DEFAULT_GUI_TEXTURE);
    }

    /**
     * Creates a screen with a custom GUI texture.
     *
     * @param title the screen title
     * @param guiTexture the texture to use (should be 176x166 with standard slot layout)
     */
    protected BaseInventoryScreen(Component title, Identifier guiTexture) {
        super(title);
        this.guiTexture = guiTexture;
    }

    @Override
    protected void init() {
        super.init();

        // Center the GUI on screen
        this.leftPos = (this.width - IMAGE_WIDTH) / 2;
        this.topPos = (this.height - IMAGE_HEIGHT) / 2;

        // Let subclasses add their widgets
        initContent();
    }

    /**
     * Called during init() to allow subclasses to add widgets and initialize content.
     * The leftPos and topPos are already calculated when this is called.
     */
    protected abstract void initContent();

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. Render blurred background
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // 2. Render the GUI texture
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, guiTexture, leftPos, topPos, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, 256, 256);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. Render background and widgets
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 2. Draw title centered at top of GUI
        guiGraphics.drawCenteredString(this.font, this.title, leftPos + IMAGE_WIDTH / 2, topPos + TITLE_Y, 0x404040);

        // 3. Let subclass render custom content
        renderContent(guiGraphics, mouseX, mouseY, partialTick);
    }

    /**
     * Called during render() to allow subclasses to render custom content in the content area.
     * This is called after the title but before the inventory.
     *
     * @param guiGraphics the graphics context
     * @param mouseX mouse X position
     * @param mouseY mouse Y position
     * @param partialTick partial tick time
     */
    protected abstract void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);

    @Override
    public boolean isPauseScreen() {
        return false;
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
