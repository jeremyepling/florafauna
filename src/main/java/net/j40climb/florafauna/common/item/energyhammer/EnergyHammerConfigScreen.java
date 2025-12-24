package net.j40climb.florafauna.common.item.energyhammer;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.RegisterDataComponentTypes;
import net.j40climb.florafauna.common.item.energyhammer.networking.UpdateEnergyHammerConfigPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Configuration screen for the Energy Hammer.
 * Allows players to configure fortune, silk touch, and mining speed settings.
 */
public class EnergyHammerConfigScreen extends Screen {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "textures/gui/inventory_hotbar_base_gui.png");

    // GUI dimensions (matches texture size)
    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 166;

    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 24;

    private EnergyHammerConfig config;
    private Button enchantmentButton;
    private Button miningSpeedButton;

    // GUI position (calculated in init)
    private int leftPos;
    private int topPos;

    public EnergyHammerConfigScreen() {
        super(Component.literal("Energy Hammer Configuration"));

        // Get the current config from the player's held item
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            ItemStack heldItem = player.getMainHandItem();
            this.config = heldItem.getOrDefault(RegisterDataComponentTypes.ENERGY_HAMMER_CONFIG, EnergyHammerConfig.DEFAULT);
        } else {
            this.config = EnergyHammerConfig.DEFAULT;
        }
    }

    @Override
    protected void init() {
        super.init();

        // Center the GUI
        this.leftPos = (this.width - IMAGE_WIDTH) / 2;
        this.topPos = (this.height - IMAGE_HEIGHT) / 2;

        // Position buttons in the upper area of the GUI texture
        int buttonX = leftPos + 76;
        int startY = topPos + 20;

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
            return Component.translatable("gui.florafauna.energy_hammer_config.enchantment.fortune");
        } else {
            return Component.translatable("gui.florafauna.energy_hammer_config.enchantment.silk_touch");
        }
    }

    private Component getMiningSpeedName() {
        return switch (config.miningSpeed()) {
            case STANDARD -> Component.translatable("gui.florafauna.energy_hammer_config.speed.standard");
            case EFFICIENCY -> Component.translatable("gui.florafauna.energy_hammer_config.speed.efficiency");
            case INSTABREAK -> Component.translatable("gui.florafauna.energy_hammer_config.speed.instabreak");
        };
    }

    private void sendConfigUpdate() {
        ClientPacketDistributor.sendToServer(new UpdateEnergyHammerConfigPayload(config));
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. Render blurred background
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // 2. Render the GUI texture on top of background
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, leftPos, topPos, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, 256, 256);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. Render background and widgets (buttons)
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 2. Draw title centered at top of GUI
        guiGraphics.drawCenteredString(this.font, this.title, leftPos + IMAGE_WIDTH / 2, topPos + 6, 0x404040);

        int labelX = leftPos + 8;
        int startY = topPos + 20;

        // 3. Draw static labels to the left of buttons
        guiGraphics.drawString(this.font, Component.translatable("gui.florafauna.energy_hammer_config.enchantment"), labelX, startY + 6, 0x404040);
        guiGraphics.drawString(this.font, Component.translatable("gui.florafauna.energy_hammer_config.mining_speed"), labelX, startY + BUTTON_SPACING + 6, 0x404040);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
