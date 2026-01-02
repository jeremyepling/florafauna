package net.j40climb.florafauna.common.block.cocoonchamber;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.RegisterAttachmentTypes;
import net.j40climb.florafauna.common.block.cocoonchamber.networking.CocoonActionPayload;
import net.j40climb.florafauna.common.block.cocoonchamber.networking.CocoonActionPayload.CocoonAction;
import net.j40climb.florafauna.common.item.symbiote.PlayerSymbioteData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Screen for the Cocoon Chamber.
 * Displays four action buttons: Set Spawn, Clear Spawn, Bind, Unbind.
 * No inventory slots - just buttons.
 */
public class CocoonChamberScreen extends AbstractContainerScreen<CocoonChamberMenu> {
    private static final Identifier GUI_TEXTURE =
            Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "textures/gui/inventory_hotbar_base_gui.png");

    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 24;

    private Button setSpawnButton;
    private Button clearSpawnButton;
    private Button bindButton;
    private Button unbindButton;

    public CocoonChamberScreen(CocoonChamberMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();

        // Center the buttons in the content area
        int buttonX = leftPos + (imageWidth - BUTTON_WIDTH) / 2;
        int startY = topPos + 25;

        // Set Spawn button
        setSpawnButton = Button.builder(
                Component.translatable("gui.florafauna.cocoon.set_spawn"),
                this::onSetSpawn
        ).bounds(buttonX, startY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        addRenderableWidget(setSpawnButton);

        // Clear Spawn button
        clearSpawnButton = Button.builder(
                Component.translatable("gui.florafauna.cocoon.clear_spawn"),
                this::onClearSpawn
        ).bounds(buttonX, startY + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        addRenderableWidget(clearSpawnButton);

        // Bind button
        bindButton = Button.builder(
                Component.translatable("gui.florafauna.cocoon.bind"),
                this::onBind
        ).bounds(buttonX, startY + BUTTON_SPACING * 2, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        addRenderableWidget(bindButton);

        // Unbind button
        unbindButton = Button.builder(
                Component.translatable("gui.florafauna.cocoon.unbind"),
                this::onUnbind
        ).bounds(buttonX, startY + BUTTON_SPACING * 3, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        addRenderableWidget(unbindButton);

        updateButtonStates();
    }

    private void updateButtonStates() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        PlayerSymbioteData data = player.getData(RegisterAttachmentTypes.PLAYER_SYMBIOTE_DATA);

        // Clear spawn is only active if cocoon spawn is set
        clearSpawnButton.active = data.cocoonSpawnPos() != null;

        // Bind is only active if:
        // - Player is bindable (consumed stew)
        // - Player is not already bonded
        // (We don't check inventory client-side - server will validate)
        bindButton.active = data.symbioteBindable() && !data.bonded();

        // Unbind is only active if player is bonded
        unbindButton.active = data.bonded();
    }

    private void onSetSpawn(Button button) {
        ClientPacketDistributor.sendToServer(new CocoonActionPayload(CocoonAction.SET_SPAWN, menu.getChamberPos()));
        onClose();
    }

    private void onClearSpawn(Button button) {
        ClientPacketDistributor.sendToServer(new CocoonActionPayload(CocoonAction.CLEAR_SPAWN, menu.getChamberPos()));
        onClose();
    }

    private void onBind(Button button) {
        ClientPacketDistributor.sendToServer(new CocoonActionPayload(CocoonAction.BIND, menu.getChamberPos()));
        onClose();
    }

    private void onUnbind(Button button) {
        ClientPacketDistributor.sendToServer(new CocoonActionPayload(CocoonAction.UNBIND, menu.getChamberPos()));
        onClose();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, leftPos, topPos, 0, 0,
                imageWidth, imageHeight, 256, 256);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Draw title centered at top
        guiGraphics.drawString(this.font, this.title, (imageWidth - this.font.width(this.title)) / 2, 8, 0x404040, false);
    }
}
