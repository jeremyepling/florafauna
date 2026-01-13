package net.j40climb.florafauna.client;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.mininganchor.pod.AbstractStoragePodBlockEntity;
import net.j40climb.florafauna.common.block.mininganchor.pod.PodContents;
import net.j40climb.florafauna.common.block.mobbarrier.data.MobBarrierConfig;
import net.j40climb.florafauna.common.entity.fear.FearData;
import net.j40climb.florafauna.common.entity.fear.FearState;
import net.j40climb.florafauna.common.entity.mobsymbiote.MobSymbioteData;
import net.j40climb.florafauna.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna.common.item.abilities.data.ThrowableAbilityData;
import net.j40climb.florafauna.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna.common.block.mininganchor.AnchorFillState;
import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.common.symbiote.data.SymbioteData;
import net.j40climb.florafauna.common.symbiote.progress.ProgressSignalTracker;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.j40climb.florafauna.common.block.mobbarrier.MobBarrierBlockEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Debug overlay that displays symbiote state information on screen.
 * Toggle with /symbiote_debug command.
 */
public class DebugOverlay implements GuiLayer {
    public static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "symbiote_debug");

    // Auto-enable in dev environment for convenience
    private static boolean enabled = !FMLEnvironment.isProduction();

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static void toggle() {
        setEnabled(!enabled);
    }

    /**
     * Registers the GUI layer. Called from FloraFauna mod event bus.
     */
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, new DebugOverlay());
    }

    // Simple record to hold a line of text with its color
    private record DebugLine(String text, int color) {}

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

        Font font = mc.font;

        // Colors
        int color = 0xFFF0F0F0; // Very bright gray
        int headerColor = 0xFFCB8AFF; // Bright purple
        int valueColor = 0xFFFFFFFF; // White
        int enabledColor = 0xFF55FF77; // Bright green
        int disabledColor = 0xFFFF7777; // Bright red
        int bgColor = 0xE0080808; // Near black, 88% opaque

        // Build all lines first
        List<DebugLine> lines = new ArrayList<>();
        buildDebugLines(lines, player, color, headerColor, valueColor, enabledColor, disabledColor);

        // Calculate dimensions
        int lineHeight = 10;
        int padding = 4;
        int x = 10;
        int y = 10;

        // Calculate max width based on actual text
        int maxWidth = 0;
        for (DebugLine line : lines) {
            int width = font.width(line.text());
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        maxWidth += padding * 2;

        int totalHeight = lines.size() * lineHeight + padding;

        // Scale to half size for compact display
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().scale(0.5f, 0.5f);

        // Draw background (sized to fit content)
        guiGraphics.fill(x - padding, y - padding, x + maxWidth, y + totalHeight, bgColor);

        // Draw all lines
        for (DebugLine line : lines) {
            guiGraphics.drawString(font, line.text(), x, y, line.color(), false);
            y += lineHeight;
        }

        guiGraphics.pose().popMatrix();
    }

    /**
     * Builds all debug lines with their colors.
     */
    private void buildDebugLines(List<DebugLine> lines, LocalPlayer player,
                                  int color, int headerColor, int valueColor,
                                  int enabledColor, int disabledColor) {
        PlayerSymbioteData data = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);

        // Header
        lines.add(new DebugLine("=== Symbiote Debug ===", headerColor));

        // Symbiote State section
        lines.add(new DebugLine("State: " + data.symbioteState().getSerializedName(), valueColor));

        if (data.symbioteState().isBonded()) {
            lines.add(new DebugLine("Bond Time: " + data.bondTime(), color));
            lines.add(new DebugLine("Tier: " + data.tier(), color));
            lines.add(new DebugLine("Abilities Active: " + (data.symbioteState().areAbilitiesActive() ? "Yes" : "No"),
                    data.symbioteState().areAbilitiesActive() ? enabledColor : disabledColor));
            lines.add(new DebugLine("  Dash: " + formatBool(data.dash()), data.dash() ? enabledColor : color));
            lines.add(new DebugLine("  Feather: " + formatBool(data.featherFalling()), data.featherFalling() ? enabledColor : color));
            lines.add(new DebugLine("  Speed: " + formatBool(data.speed()), data.speed() ? enabledColor : color));
            lines.add(new DebugLine("  Jump: " + data.jumpBoost(), data.jumpBoost() > 0 ? enabledColor : color));
        }

        // Cocoon State section
        lines.add(new DebugLine("--- Cocoon State ---", headerColor));
        lines.add(new DebugLine("Bindable: " + formatBool(data.symbioteBindable()), data.symbioteBindable() ? enabledColor : color));
        lines.add(new DebugLine("Stew: " + formatBool(data.symbioteStewConsumedOnce()), color));
        lines.add(new DebugLine("Cocoon Set: " + formatBool(data.cocoonSpawnSetOnce()), color));
        lines.add(new DebugLine("Cocoon: " + formatPosAndDim(data.cocoonSpawnPos(), data.cocoonSpawnDim()), color));
        lines.add(new DebugLine("Prev Bed: " + formatPosAndDim(data.previousBedSpawnPos(), data.previousBedSpawnDim()), color));

        // Restoration Husk section
        lines.add(new DebugLine("--- Restoration Husk ---", headerColor));
        lines.add(new DebugLine("Active: " + formatBool(data.restorationHuskActive()), data.restorationHuskActive() ? enabledColor : color));
        lines.add(new DebugLine("Pos: " + formatPosAndDim(data.restorationHuskPos(), data.restorationHuskDim()), color));

        // Mining Anchor section
        lines.add(new DebugLine("--- Mining Anchor ---", headerColor));
        boolean hasBound = data.hasAnchorBound();
        lines.add(new DebugLine("Bound: " + formatBool(hasBound), hasBound ? enabledColor : color));
        lines.add(new DebugLine("Anchor: " + formatPosAndDim(data.boundAnchorPos(), data.boundAnchorDim()), color));
        lines.add(new DebugLine("Waypoint: " + formatPosAndDim(data.activeWaypointAnchorPos(), data.activeWaypointAnchorDim()), color));
        lines.add(new DebugLine("Fill State: " + formatFillState(data.lastAnnouncedFillState()),
                getFillStateColor(data.lastAnnouncedFillState())));

        // Held Item section
        ItemStack heldItem = player.getMainHandItem();
        if (!heldItem.isEmpty()) {
            lines.add(new DebugLine("--- Held Item ---", headerColor));
            lines.add(new DebugLine("Item: " + heldItem.getHoverName().getString(), valueColor));

            List<String> componentLines = getModDataComponentLines(heldItem);
            if (componentLines.isEmpty()) {
                lines.add(new DebugLine("No mod components", color));
            } else {
                for (String line : componentLines) {
                    lines.add(new DebugLine(line, color));
                }
            }
        }

        // Targeted Entity or Block section
        buildTargetedEntityOrBlockLines(lines, player, color, headerColor, valueColor, enabledColor, disabledColor);
    }

    /**
     * Builds debug lines for the entity or block the player is currently looking at.
     * Prioritizes entities over blocks.
     */
    private void buildTargetedEntityOrBlockLines(List<DebugLine> lines, LocalPlayer player,
                                                  int color, int headerColor, int valueColor,
                                                  int enabledColor, int disabledColor) {
        Minecraft mc = Minecraft.getInstance();

        // First check for entity hit (crosshairPickEntity is the entity under crosshair)
        Entity targetEntity = mc.crosshairPickEntity;
        if (targetEntity != null) {
            buildTargetedEntityLines(lines, targetEntity, player, color, headerColor, valueColor, enabledColor, disabledColor);
            return;
        }

        // Fall back to block hit
        HitResult hitResult = mc.hitResult;
        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            buildTargetedBlockLines(lines, player, color, headerColor, valueColor);
        }
    }

    /**
     * Builds debug lines for a targeted entity.
     */
    private void buildTargetedEntityLines(List<DebugLine> lines, Entity entity, LocalPlayer player,
                                           int color, int headerColor, int valueColor,
                                           int enabledColor, int disabledColor) {
        lines.add(new DebugLine("--- Targeted Entity ---", headerColor));
        lines.add(new DebugLine("Entity: " + entity.getName().getString(), valueColor));
        lines.add(new DebugLine("Type: " + entity.getType().toShortString(), color));
        lines.add(new DebugLine("ID: " + entity.getId(), color));

        // Only show FloraFauna data for Mob entities
        if (!(entity instanceof Mob mob)) {
            return;
        }

        long currentTick = player.level().getGameTime();

        // Check for MobSymbiote data
        if (mob.hasData(FloraFaunaRegistry.MOB_SYMBIOTE_DATA)) {
            MobSymbioteData symbioteData = mob.getData(FloraFaunaRegistry.MOB_SYMBIOTE_DATA);
            if (symbioteData.hasMobSymbiote()) {
                lines.add(new DebugLine("-- MobSymbiote --", headerColor));
                lines.add(new DebugLine("Level: " + symbioteData.mobSymbioteLevel(), enabledColor));

                // Show release immunity if active
                if (symbioteData.hasReleaseImmunity(currentTick)) {
                    long immunityRemaining = symbioteData.recentlyReleasedUntil() - currentTick;
                    lines.add(new DebugLine("Capture Immune: " + formatTicks(immunityRemaining), disabledColor));
                }
            } else {
                lines.add(new DebugLine("MobSymbiote: None", color));
            }
        }

        // Check for Fear data
        if (mob.hasData(FloraFaunaRegistry.FEAR_DATA)) {
            FearData fearData = mob.getData(FloraFaunaRegistry.FEAR_DATA);
            FearState state = fearData.fearState();

            lines.add(new DebugLine("-- Fear State --", headerColor));
            lines.add(new DebugLine("State: " + state.name(), getFearStateColor(state)));

            long ticksInState = fearData.getTicksInState(currentTick);
            lines.add(new DebugLine("In State: " + formatTicks(ticksInState), color));

            // Show state-specific timing
            switch (state) {
                case PANICKED -> {
                    long ticksUntilLeak = Config.panicDurationForLeak - ticksInState;
                    if (ticksUntilLeak > 0) {
                        lines.add(new DebugLine("Until LEAK: " + formatTicks(ticksUntilLeak), 0xFFFFAA00));
                    } else {
                        lines.add(new DebugLine("LEAK imminent!", 0xFFFF5555));
                    }
                }
                case EXHAUSTED -> {
                    long ticksUntilCalm = Config.exhaustedCooldownTicks - ticksInState;
                    if (ticksUntilCalm > 0) {
                        lines.add(new DebugLine("Until CALM: " + formatTicks(ticksUntilCalm), 0xFF55FFFF));
                    } else {
                        lines.add(new DebugLine("CALM soon", 0xFF55FF77));
                    }
                }
                case CALM -> {
                    // Nothing special to show
                }
                case LEAK, OVERSTRESS -> {
                    // Transient states, just show current
                }
            }

            // Show leak count
            int leakCount = fearData.leakCountSinceCooldown();
            if (leakCount > 0) {
                int remaining = Config.maxLeaksBeforeOverstress - leakCount;
                int leakColor = remaining <= 1 ? 0xFFFF5555 : (remaining <= 2 ? 0xFFFFAA00 : color);
                lines.add(new DebugLine("Leaks: " + leakCount + "/" + Config.maxLeaksBeforeOverstress, leakColor));
            }

            // Show fear source if present
            fearData.getFearSourcePos().ifPresent(pos ->
                    lines.add(new DebugLine("Fear Source: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ(), color)));
        }
    }

    /**
     * Builds debug lines for the block the player is currently looking at.
     */
    private void buildTargetedBlockLines(List<DebugLine> lines, LocalPlayer player,
                                          int color, int headerColor, int valueColor) {
        Minecraft mc = Minecraft.getInstance();
        HitResult hitResult = mc.hitResult;

        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockPos pos = blockHit.getBlockPos();
        Level level = player.level();
        BlockState blockState = level.getBlockState(pos);

        // Check if this is a FloraFauna block by checking the item form for our components
        ItemStack blockItem = new ItemStack(blockState.getBlock());
        List<String> componentLines = getModDataComponentLines(blockItem);

        // Check for FloraFauna block entities
        BlockEntity blockEntity = level.getBlockEntity(pos);
        List<String> blockLines = blockEntity != null ? getBlockEntityDebugLines(blockEntity) : List.of();

        // Only show section if there's something to display
        if (!componentLines.isEmpty() || !blockLines.isEmpty()) {
            lines.add(new DebugLine("--- Targeted Block ---", headerColor));
            lines.add(new DebugLine("Block: " + blockState.getBlock().getName().getString(), valueColor));
            lines.add(new DebugLine("Pos: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ(), valueColor));

            // Show item-form data components
            for (String line : componentLines) {
                lines.add(new DebugLine(line, color));
            }

            // Show block entity specific data
            for (String line : blockLines) {
                lines.add(new DebugLine(line, color));
            }
        }
    }

    /**
     * Gets debug lines for FloraFauna block entities.
     */
    private List<String> getBlockEntityDebugLines(BlockEntity blockEntity) {
        List<String> lines = new ArrayList<>();

        if (blockEntity instanceof MobBarrierBlockEntity mobBarrierBE) {
            MobBarrierConfig config = mobBarrierBE.getConfig();
            lines.add("Mob Barrier Block:");
            if (config.entityIds().isEmpty() && config.entityTags().isEmpty()) {
                lines.add("  (no entities configured)");
            } else {
                for (String id : config.entityIds()) {
                    lines.add("  ID: " + id);
                }
                for (String tag : config.entityTags()) {
                    lines.add("  Tag: " + tag);
                }
            }
        }

        if (blockEntity instanceof AbstractStoragePodBlockEntity podBE) {
            lines.add("Storage Pod:");
            if (podBE.isEmpty()) {
                lines.add("  (empty)");
            } else {
                lines.add("  Total: " + podBE.getStoredCount() + " items");
                for (ItemStack item : podBE.getBuffer().getContentsForDrop()) {
                    lines.add("  " + item.getHoverName().getString() + " x" + item.getCount());
                }
            }
        }

        return lines;
    }

    /**
     * Gets debug lines for all FloraFauna data components on the given item.
     */
    private static List<String> getModDataComponentLines(ItemStack stack) {
        List<String> lines = new ArrayList<>();

        // Check each FloraFauna data component
        if (stack.has(FloraFaunaRegistry.MULTI_BLOCK_MINING.get())) {
            MiningModeData miningData = stack.get(FloraFaunaRegistry.MULTI_BLOCK_MINING.get());
            if (miningData != null) {
                lines.add("Mining: " + miningData.shape().name() + ", radius=" + miningData.radius());
            }
        }

        if (stack.has(FloraFaunaRegistry.TOOL_CONFIG.get())) {
            ToolConfig toolConfig = stack.get(FloraFaunaRegistry.TOOL_CONFIG.get());
            if (toolConfig != null) {
                String enchant = toolConfig.fortune() ? "Fortune" : "SilkTouch";
                lines.add("Tool Config: " + enchant + ", " + toolConfig.miningSpeed().name());
            }
        }

        if (stack.has(FloraFaunaRegistry.LIGHTNING_ABILITY.get())) {
            lines.add("Lightning: Yes");
        }

        if (stack.has(FloraFaunaRegistry.TELEPORT_SURFACE_ABILITY.get())) {
            lines.add("Teleport: Yes");
        }

        if (stack.has(FloraFaunaRegistry.SYMBIOTE_DATA.get())) {
            SymbioteData symbioteData = stack.get(FloraFaunaRegistry.SYMBIOTE_DATA.get());
            if (symbioteData != null) {
                lines.add("Symbiote: bonded=" + symbioteData.bonded() + ", tier=" + symbioteData.tier());
            }
        }

        if (stack.has(FloraFaunaRegistry.SYMBIOTE_PROGRESS.get())) {
            lines.add("Symbiote Progress: Present");
        }

        if (stack.has(FloraFaunaRegistry.THROWABLE_ABILITY.get())) {
            ThrowableAbilityData throwData = stack.get(FloraFaunaRegistry.THROWABLE_ABILITY.get());
            if (throwData != null) {
                lines.add("Throw: dmg=" + throwData.damage() + ", range=" + throwData.maxRange());
                lines.add("  return=" + (throwData.autoReturn() ? "yes" : "no") + ", speed=" + throwData.returnSpeed());
            }
        }

        if (stack.has(FloraFaunaRegistry.MOB_BARRIER_CONFIG.get())) {
            MobBarrierConfig config = stack.get(FloraFaunaRegistry.MOB_BARRIER_CONFIG.get());
            if (config != null) {
                lines.add("Mob Barrier Config:");
                if (config.entityIds().isEmpty() && config.entityTags().isEmpty()) {
                    lines.add("  (no entities)");
                } else {
                    for (String id : config.entityIds()) {
                        lines.add("  ID: " + id);
                    }
                    for (String tag : config.entityTags()) {
                        lines.add("  Tag: " + tag);
                    }
                }
            }
        }

        if (stack.has(FloraFaunaRegistry.POD_CONTENTS.get())) {
            PodContents contents = stack.get(FloraFaunaRegistry.POD_CONTENTS.get());
            if (contents != null && !contents.isEmpty()) {
                lines.add("Pod Contents: " + contents.getItemCount() + " items");
                // Show item types with counts
                for (ItemStack item : contents.items()) {
                    lines.add("  " + item.getHoverName().getString() + " x" + item.getCount());
                }
            } else {
                lines.add("Pod Contents: Empty");
            }
        }

        return lines;
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

    private static String formatFillState(AnchorFillState state) {
        return switch (state) {
            case NORMAL -> "Normal (<75%)";
            case WARNING -> "Warning (>=75%)";
            case FULL -> "Full (100%)";
        };
    }

    private static int getFillStateColor(AnchorFillState state) {
        return switch (state) {
            case NORMAL -> 0xFF55FF77;   // Green
            case WARNING -> 0xFFFFAA00;  // Orange
            case FULL -> 0xFFFF5555;     // Red
        };
    }

    /**
     * Formats ticks as a human-readable time string.
     * Shows seconds with one decimal place, or just ticks if under 1 second.
     */
    private static String formatTicks(long ticks) {
        if (ticks < 0) {
            return "0t";
        }
        if (ticks < 20) {
            return ticks + "t";
        }
        double seconds = ticks / 20.0;
        if (seconds < 60) {
            return String.format("%.1fs", seconds);
        }
        int mins = (int) (seconds / 60);
        double secs = seconds % 60;
        return String.format("%dm %.0fs", mins, secs);
    }

    /**
     * Gets the display color for a fear state.
     */
    private static int getFearStateColor(FearState state) {
        return switch (state) {
            case CALM -> 0xFF55FF77;       // Green
            case PANICKED -> 0xFFFFAA00;   // Orange
            case LEAK -> 0xFFFF55FF;       // Magenta
            case EXHAUSTED -> 0xFF55FFFF;  // Cyan
            case OVERSTRESS -> 0xFFFF5555; // Red
        };
    }
}
