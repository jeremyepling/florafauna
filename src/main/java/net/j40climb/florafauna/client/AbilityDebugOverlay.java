package net.j40climb.florafauna.client;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna.common.item.abilities.data.ThrowableAbilityData;
import net.j40climb.florafauna.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;

/**
 * Debug overlay that displays ability data components on the held item.
 * Toggle with /florafauna ability_debug command.
 * Mutually exclusive with DebugOverlay (symbiote debug).
 */
public class AbilityDebugOverlay implements GuiLayer {
    public static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "ability_debug");

    private static boolean enabled = false;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (value) {
            // Turn off symbiote debug overlay for mutual exclusivity
            DebugOverlay.setEnabled(false);
        }
    }

    public static void toggle() {
        setEnabled(!enabled);
    }

    /**
     * Registers the GUI layer. Called from ClientSetup.
     */
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, new AbilityDebugOverlay());
    }

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!enabled) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }

        // Don't show when F3 debug screen is open
        if (mc.getDebugOverlay().showDebugScreen()) {
            return;
        }

        Font font = mc.font;
        ItemStack heldItem = player.getMainHandItem();

        // Scale to half size for compact display
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().scale(0.5f, 0.5f);

        // Coordinates in scaled space
        int x = 10;
        int y = 10;
        int lineHeight = 10;
        int sectionGap = 3;
        int padding = 4;
        int color = 0xFFF0F0F0; // Very bright gray
        int headerColor = 0xFFCB8AFF; // Bright purple
        int enabledColor = 0xFF55FF77; // Bright green
        int disabledColor = 0xFFFF7777; // Bright red
        int bgColor = 0xE0080808; // Near black, 88% opaque

        // Calculate content height
        int lineCount = 2; // Header + Item name
        if (heldItem.isEmpty()) {
            lineCount += 1; // "No item in hand"
        } else {
            lineCount += 1; // Abilities header
            // Count abilities present
            if (heldItem.has(FloraFaunaRegistry.LIGHTNING_ABILITY)) lineCount++;
            if (heldItem.has(FloraFaunaRegistry.TELEPORT_SURFACE_ABILITY)) lineCount++;
            if (heldItem.has(FloraFaunaRegistry.THROWABLE_ABILITY)) lineCount += 2;
            if (heldItem.has(FloraFaunaRegistry.MULTI_BLOCK_MINING)) lineCount++;
            if (heldItem.has(FloraFaunaRegistry.TOOL_CONFIG)) lineCount++;

            // If no abilities, add one line for "No abilities"
            boolean hasAny = heldItem.has(FloraFaunaRegistry.LIGHTNING_ABILITY)
                    || heldItem.has(FloraFaunaRegistry.TELEPORT_SURFACE_ABILITY)
                    || heldItem.has(FloraFaunaRegistry.THROWABLE_ABILITY)
                    || heldItem.has(FloraFaunaRegistry.MULTI_BLOCK_MINING)
                    || heldItem.has(FloraFaunaRegistry.TOOL_CONFIG);
            if (!hasAny) lineCount++;
        }

        int totalHeight = lineCount * lineHeight + sectionGap + padding * 2;
        int maxWidth = 220;

        // Draw background
        guiGraphics.fill(x - padding, y - padding, x + maxWidth, y + totalHeight - padding, bgColor);

        // Header
        guiGraphics.drawString(font, "=== Ability Debug ===", x, y, headerColor, false);
        y += lineHeight;

        if (heldItem.isEmpty()) {
            guiGraphics.drawString(font, "No item in hand", x, y, disabledColor, false);
        } else {
            // Item name
            guiGraphics.drawString(font, "Item: " + heldItem.getHoverName().getString(), x, y, color, false);
            y += lineHeight;

            // Abilities section
            y += sectionGap;
            guiGraphics.drawString(font, "--- Abilities ---", x, y, headerColor, false);
            y += lineHeight;

            boolean hasAny = false;

            // Lightning ability
            if (heldItem.has(FloraFaunaRegistry.LIGHTNING_ABILITY)) {
                guiGraphics.drawString(font, "Lightning: Yes", x, y, enabledColor, false);
                y += lineHeight;
                hasAny = true;
            }

            // Teleport ability
            if (heldItem.has(FloraFaunaRegistry.TELEPORT_SURFACE_ABILITY)) {
                guiGraphics.drawString(font, "Teleport: Yes", x, y, enabledColor, false);
                y += lineHeight;
                hasAny = true;
            }

            // Throwable ability
            if (heldItem.has(FloraFaunaRegistry.THROWABLE_ABILITY)) {
                ThrowableAbilityData throwData = heldItem.get(FloraFaunaRegistry.THROWABLE_ABILITY);
                guiGraphics.drawString(font, String.format("Throw: dmg=%.1f, range=%.1f",
                        throwData.damage(), throwData.maxRange()), x, y, enabledColor, false);
                y += lineHeight;
                guiGraphics.drawString(font, String.format("  return=%s, speed=%.1f",
                        throwData.autoReturn() ? "yes" : "no", throwData.returnSpeed()), x, y, color, false);
                y += lineHeight;
                hasAny = true;
            }

            // Mining mode
            if (heldItem.has(FloraFaunaRegistry.MULTI_BLOCK_MINING)) {
                MiningModeData miningData = heldItem.get(FloraFaunaRegistry.MULTI_BLOCK_MINING);
                guiGraphics.drawString(font, String.format("Mining: %s, radius=%d",
                        miningData.shape().name(), miningData.radius()), x, y, enabledColor, false);
                y += lineHeight;
                hasAny = true;
            }

            // Tool config
            if (heldItem.has(FloraFaunaRegistry.TOOL_CONFIG)) {
                ToolConfig config = heldItem.get(FloraFaunaRegistry.TOOL_CONFIG);
                guiGraphics.drawString(font, String.format("Config: %s, %s",
                        config.fortune() ? "Fortune" : "SilkTouch",
                        config.miningSpeed().name()), x, y, enabledColor, false);
                y += lineHeight;
                hasAny = true;
            }

            if (!hasAny) {
                guiGraphics.drawString(font, "No abilities", x, y, disabledColor, false);
            }
        }

        guiGraphics.pose().popMatrix();
    }
}
