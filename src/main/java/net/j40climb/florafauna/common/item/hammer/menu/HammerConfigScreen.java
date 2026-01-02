package net.j40climb.florafauna.common.item.hammer.menu;

import net.j40climb.florafauna.client.gui.BaseInventoryScreen;
import net.j40climb.florafauna.common.RegisterDataComponentTypes;
import net.j40climb.florafauna.common.item.hammer.abilities.UpdateHammerConfigPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Configuration screen for the Energy Hammer.
 * Allows players to configure fortune, silk touch, and mining speed settings.
 */
public class HammerConfigScreen extends BaseInventoryScreen {
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 24;

    private HammerConfig config;
    private Button enchantmentButton;
    private Button miningSpeedButton;

    public HammerConfigScreen() {
        super(Component.translatable("gui.florafauna.hammer_config.title"));

        // Get the current config from the player's held item
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            ItemStack heldItem = player.getMainHandItem();
            this.config = heldItem.getOrDefault(RegisterDataComponentTypes.HAMMER_CONFIG, HammerConfig.DEFAULT);
        } else {
            this.config = HammerConfig.DEFAULT;
        }
    }

    @Override
    protected void initContent() {
        // Position buttons in the content area
        int buttonX = leftPos + 38;
        int startY = getContentY() + 2;

        // Enchantment toggle button - shows current enchantment
        this.enchantmentButton = Button.builder(
                getEnchantmentName(),
                button -> toggleEnchantment()
        ).bounds(buttonX, startY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addRenderableWidget(this.enchantmentButton);

        // Mining Speed button - shows current speed
        this.miningSpeedButton = Button.builder(
                getMiningSpeedName(),
                button -> cycleMiningSpeed()
        ).bounds(buttonX, startY + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addRenderableWidget(this.miningSpeedButton);
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int labelX = getContentX();
        int startY = getContentY() + 2;

        // Draw static labels to the left of buttons
        guiGraphics.drawString(this.font, Component.translatable("gui.florafauna.hammer_config.enchantment"), labelX, startY + 6, 0x404040);
        guiGraphics.drawString(this.font, Component.translatable("gui.florafauna.hammer_config.mining_speed"), labelX, startY + BUTTON_SPACING + 6, 0x404040);
    }

    private void toggleEnchantment() {
        config = config.withToggledEnchantment();
        updateButtonLabels();
        sendConfigUpdate();
    }

    private void cycleMiningSpeed() {
        config = config.withNextMiningSpeed();
        updateButtonLabels();
        sendConfigUpdate();
    }

    private void updateButtonLabels() {
        this.enchantmentButton.setMessage(getEnchantmentName());
        this.miningSpeedButton.setMessage(getMiningSpeedName());
    }

    private Component getEnchantmentName() {
        if (config.fortune()) {
            return Component.translatable("gui.florafauna.hammer_config.enchantment.fortune");
        } else {
            return Component.translatable("gui.florafauna.hammer_config.enchantment.silk_touch");
        }
    }

    private Component getMiningSpeedName() {
        return switch (config.miningSpeed()) {
            case STANDARD -> Component.translatable("gui.florafauna.hammer_config.speed.standard");
            case EFFICIENCY -> Component.translatable("gui.florafauna.hammer_config.speed.efficiency");
            case INSTABREAK -> Component.translatable("gui.florafauna.hammer_config.speed.instabreak");
        };
    }

    private void sendConfigUpdate() {
        ClientPacketDistributor.sendToServer(new UpdateHammerConfigPayload(config));
    }
}
