package net.j40climb.florafauna.common.block.cocoonchamber;

import net.j40climb.florafauna.client.gui.BaseScreen;
import net.j40climb.florafauna.common.block.cocoonchamber.networking.CocoonActionPayload;
import net.j40climb.florafauna.common.block.cocoonchamber.networking.CocoonActionPayload.CocoonAction;
import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Screen for the Cocoon Chamber - a button-only GUI.
 * Displays four action buttons: Set Spawn, Clear Spawn, Bind, Unbind.
 * <p>
 * This screen uses {@link BaseScreen} (not BaseContainerScreen) because it has
 * no inventory slots. Server communication is handled via {@link CocoonActionPayload}.
 * <p>
 * Opened via {@link net.j40climb.florafauna.common.block.cocoonchamber.networking.OpenCocoonScreenPayload}
 * when player right-clicks the block.
 */
public class CocoonChamberScreen extends BaseScreen {

    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 24;

    private final BlockPos chamberPos;

    private Button setSpawnButton;
    private Button clearSpawnButton;
    private Button bindButton;
    private Button unbindButton;

    public CocoonChamberScreen(BlockPos chamberPos) {
        super(Component.translatable("gui.florafauna.cocoon.title"));
        this.chamberPos = chamberPos;
    }

    @Override
    protected void initContent() {
        // Center the buttons in the content area
        int buttonX = leftPos + (IMAGE_WIDTH - BUTTON_WIDTH) / 2;
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

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Draw title centered at top
        int titleX = leftPos + (IMAGE_WIDTH - font.width(title)) / 2;
        guiGraphics.drawString(font, title, titleX, topPos + 8, CommonColors.DARK_GRAY, false);
    }

    private void updateButtonStates() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        PlayerSymbioteData data = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);

        // Clear spawn is only active if cocoon spawn is set
        clearSpawnButton.active = data.cocoonSpawnPos() != null;

        // Bind is only active if:
        // - Player is bindable (consumed stew)
        // - Player is not already bonded
        // (We don't check inventory client-side - server will validate)
        bindButton.active = data.symbioteBindable() && !data.symbioteState().isBonded();

        // Unbind is only active if player is bonded
        unbindButton.active = data.symbioteState().isBonded();
    }

    private void onSetSpawn(Button button) {
        ClientPacketDistributor.sendToServer(new CocoonActionPayload(CocoonAction.SET_SPAWN, chamberPos));
        onClose();
    }

    private void onClearSpawn(Button button) {
        ClientPacketDistributor.sendToServer(new CocoonActionPayload(CocoonAction.CLEAR_SPAWN, chamberPos));
        onClose();
    }

    private void onBind(Button button) {
        ClientPacketDistributor.sendToServer(new CocoonActionPayload(CocoonAction.BIND, chamberPos));
        onClose();
    }

    private void onUnbind(Button button) {
        ClientPacketDistributor.sendToServer(new CocoonActionPayload(CocoonAction.UNBIND, chamberPos));
        onClose();
    }
}
