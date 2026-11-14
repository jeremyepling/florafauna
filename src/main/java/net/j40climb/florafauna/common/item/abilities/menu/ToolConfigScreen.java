package net.j40climb.florafauna.common.item.abilities.menu;

import net.j40climb.florafauna.client.gui.BaseScreen;
import net.j40climb.florafauna.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna.common.item.abilities.networking.UpdateToolConfigPayload;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Configuration screen for tools with configurable enchantments and mining speed.
 * Works with any item that has the TOOL_CONFIG data component.
 */
public class ToolConfigScreen extends BaseScreen {
    private static final int BUTTON_WIDTH = 88;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 24;

    private ToolConfig config;
    private final boolean hasVeinMining;
    private Button enchantmentButton;
    private Button miningSpeedButton;

    public ToolConfigScreen() {
        super(Component.translatable("gui.florafauna.tool_config.title"));

        // Get the current config from the player's held item
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            ItemStack heldItem = player.getMainHandItem();
            this.hasVeinMining = heldItem.has(FloraFaunaRegistry.MULTI_BLOCK_MINING);
            this.config = heldItem.getOrDefault(FloraFaunaRegistry.TOOL_CONFIG, ToolConfig.DEFAULT);
        } else {
            this.hasVeinMining = false;
            this.config = ToolConfig.DEFAULT;
        }
    }

    @Override
    protected void initContent() {
        // Position buttons in the content area
        int buttonX = getContentX() + CONTENT_WIDTH - BUTTON_WIDTH;
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

        if (hasVeinMining) {
            this.addRenderableWidget(Button.builder(veinMatchingName(), button -> {
                config = config.withToggledVeinMatching();
                button.setMessage(veinMatchingName());
                sendConfigUpdate();
            }).bounds(buttonX, startY + 2 * BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        }
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        int labelX = getContentX();
        int startY = getContentY() + 2;

        // Draw static labels to the left of buttons
        guiGraphics.text(this.font, Component.translatable("gui.florafauna.tool_config.enchantment"), labelX, startY + 6, 0xFF404040);
        guiGraphics.text(this.font, Component.translatable("gui.florafauna.tool_config.mining_speed"), labelX, startY + BUTTON_SPACING + 6, 0xFF404040);
        if (hasVeinMining) {
            guiGraphics.text(this.font, Component.translatable("gui.florafauna.tool_config.vein_matching"),
                    labelX, startY + 2 * BUTTON_SPACING + 6, 0xFF404040);
        }
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
            return Component.translatable("gui.florafauna.tool_config.enchantment.fortune");
        } else {
            return Component.translatable("gui.florafauna.tool_config.enchantment.silk_touch");
        }
    }

    private Component getMiningSpeedName() {
        return switch (config.miningSpeed()) {
            case STANDARD -> Component.translatable("gui.florafauna.tool_config.speed.standard");
            case EFFICIENCY -> Component.translatable("gui.florafauna.tool_config.speed.efficiency");
            case INSTABREAK -> Component.translatable("gui.florafauna.tool_config.speed.instabreak");
        };
    }

    private Component veinMatchingName() {
        return Component.translatable(config.exactVein() ? "gui.florafauna.tool_config.vein.exact"
                : "gui.florafauna.tool_config.vein.same_ore");
    }

    private void sendConfigUpdate() {
        ClientPacketDistributor.sendToServer(new UpdateToolConfigPayload(config));
    }
}
