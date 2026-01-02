package net.j40climb.florafauna.client;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.item.symbiote.PlayerSymbioteData;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jetbrains.annotations.Nullable;

/**
 * Debug overlay that displays symbiote state information on screen.
 * Toggle with /symbiote_debug command.
 */
public class SymbioteDebugOverlay implements GuiLayer {
    public static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "symbiote_debug");

    private static boolean enabled = false;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static void toggle() {
        enabled = !enabled;
    }

    /**
     * Registers the GUI layer. Called from FloraFauna mod event bus.
     */
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, new SymbioteDebugOverlay());
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

        PlayerSymbioteData data = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
        Font font = mc.font;

        int x = 5;
        int y = 5;
        int lineHeight = 10;
        int color = 0xFFAAAAAA; // Light gray
        int headerColor = 0xFF9B59B6; // Purple (matches command header)
        int valueColor = 0xFFFFFFFF; // White
        int enabledColor = 0xFF2ECC71; // Green
        int disabledColor = 0xFFE74C3C; // Red

        // Header
        guiGraphics.drawString(font, "=== Symbiote Debug ===", x, y, headerColor, false);
        y += lineHeight;

        // Symbiote State section
        guiGraphics.drawString(font, "State: " + data.symbioteState().getSerializedName(), x, y, valueColor, false);
        y += lineHeight;

        if (data.symbioteState().isBonded()) {
            // Bond info
            guiGraphics.drawString(font, "Bond Time: " + data.bondTime(), x, y, color, false);
            y += lineHeight;

            guiGraphics.drawString(font, "Tier: " + data.tier(), x, y, color, false);
            y += lineHeight;

            // Abilities
            guiGraphics.drawString(font, "Abilities Active: " + (data.symbioteState().areAbilitiesActive() ? "Yes" : "No"),
                    x, y, data.symbioteState().areAbilitiesActive() ? enabledColor : disabledColor, false);
            y += lineHeight;

            guiGraphics.drawString(font, "  Dash: " + formatBool(data.dash()), x, y,
                    data.dash() ? enabledColor : color, false);
            y += lineHeight;

            guiGraphics.drawString(font, "  Feather: " + formatBool(data.featherFalling()), x, y,
                    data.featherFalling() ? enabledColor : color, false);
            y += lineHeight;

            guiGraphics.drawString(font, "  Speed: " + formatBool(data.speed()), x, y,
                    data.speed() ? enabledColor : color, false);
            y += lineHeight;

            guiGraphics.drawString(font, "  Jump: " + data.jumpBoost(), x, y,
                    data.jumpBoost() > 0 ? enabledColor : color, false);
            y += lineHeight;
        }

        // Cocoon State section
        y += 3; // Small gap
        guiGraphics.drawString(font, "--- Cocoon State ---", x, y, headerColor, false);
        y += lineHeight;

        guiGraphics.drawString(font, "Bindable: " + formatBool(data.symbioteBindable()), x, y,
                data.symbioteBindable() ? enabledColor : color, false);
        y += lineHeight;

        guiGraphics.drawString(font, "Stew: " + formatBool(data.symbioteStewConsumedOnce()), x, y, color, false);
        y += lineHeight;

        guiGraphics.drawString(font, "Cocoon Set: " + formatBool(data.cocoonSpawnSetOnce()), x, y, color, false);
        y += lineHeight;

        guiGraphics.drawString(font, "Cocoon: " + formatPosAndDim(data.cocoonSpawnPos(), data.cocoonSpawnDim()),
                x, y, color, false);
        y += lineHeight;

        guiGraphics.drawString(font, "Prev Bed: " + formatPosAndDim(data.previousBedSpawnPos(), data.previousBedSpawnDim()),
                x, y, color, false);
        y += lineHeight;

        // Restoration Husk section
        y += 3; // Small gap
        guiGraphics.drawString(font, "--- Restoration Husk ---", x, y, headerColor, false);
        y += lineHeight;

        guiGraphics.drawString(font, "Active: " + formatBool(data.restorationHuskActive()), x, y,
                data.restorationHuskActive() ? enabledColor : color, false);
        y += lineHeight;

        guiGraphics.drawString(font, "Pos: " + formatPosAndDim(data.restorationHuskPos(), data.restorationHuskDim()),
                x, y, color, false);
    }

    private static String formatBool(boolean value) {
        return value ? "Yes" : "No";
    }

    private static String formatPosAndDim(@Nullable BlockPos pos, @Nullable ResourceKey<Level> dim) {
        if (pos == null || dim == null) {
            return "None";
        }
        String dimName = dim.identifier().getPath();
        return String.format("%d,%d,%d (%s)", pos.getX(), pos.getY(), pos.getZ(), dimName);
    }
}
