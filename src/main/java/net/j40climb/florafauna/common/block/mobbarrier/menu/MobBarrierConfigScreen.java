package net.j40climb.florafauna.common.block.mobbarrier.menu;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.j40climb.florafauna.client.gui.BaseScreen;
import net.j40climb.florafauna.common.block.mobbarrier.data.MobBarrierConfig;
import net.j40climb.florafauna.common.block.mobbarrier.networking.UpdateMobBarrierConfigPayload;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration screen for MobBarrierBlock.
 *
 * Layout:
 * - Title "Blocked Entities:" at top (like shulker boxes)
 * - Input field + Add button
 * - Scrollable list of blocked entities with remove buttons
 * - Scroll buttons for list navigation
 * - Autocomplete suggestions dropdown
 */
public class MobBarrierConfigScreen extends BaseScreen {

    // ===================
    // Layout Constants
    // ===================

    // Title section
    public static final int TITLE_Y_OFFSET = 10;
    public static final int TITLE_X_OFFSET = 8;

    // Input section (below title)
    private static final int INPUT_Y_OFFSET = 6;      // Below content start
    private static final int INPUT_WIDTH = 120;
    private static final int INPUT_HEIGHT = 16;
    private static final int ADD_BUTTON_GAP = 10;

    // List section
    private static final int LIST_Y_OFFSET = 26;       // Below input
    private static final int LIST_ENTRY_HEIGHT = 12;
    private static final int LIST_ENTRY_WIDTH = 128;
    private static final int LIST_ENTRY_MAX_CHARS = 22; // Max characters before truncation
    private static final int MAX_VISIBLE_ENTRIES = 6;
    private static final int REMOVE_BUTTON_SIZE = 12;
    private static final int LIST_ENTRY_GAP = 2;       // Vertical gap between rows

    // Scrollbar (same dimensions as creative menu)
    private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/creative_inventory/scroller");
    private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/creative_inventory/scroller_disabled");
    private static final int SCROLLER_WIDTH = 12;
    private static final int SCROLLER_HEIGHT = 15;

    // Autocomplete
    private static final int SUGGESTION_HEIGHT = 14;
    private static final int SUGGESTION_WIDTH = 140;
    private static final int SUGGESTION_MAX_CHARS = 24; // Max characters before truncation
    private static final int MAX_SUGGESTIONS = 5;
    public static final int INPUT_X_OFFSET = 2;

    // Vision blocking toggle
    private static final int TOGGLE_Y_OFFSET = 112; // Below list area
    private static final int TOGGLE_WIDTH = 120;
    private static final int TOGGLE_HEIGHT = 16;

    // ===================
    // State
    // ===================

    private MobBarrierConfig config;
    private EditBox inputField;
    private int scrollOffset = 0;

    // Scrollbar state
    private boolean isDraggingScrollbar = false;

    // Autocomplete state
    private final List<String> suggestions = new ArrayList<>();
    private final List<String> autocompleteSource = new ArrayList<>();
    private int suggestionScrollOffset = 0;

    // ===================
    // Texture
    // ===================

    private static final Identifier GUI_TEXTURE =
            Identifier.fromNamespaceAndPath("florafauna", "textures/gui/mob_barrier_block_gui.png");

    // ===================
    // Constructor
    // ===================

    public MobBarrierConfigScreen() {
        super(Component.translatable("gui.florafauna.mob_barrier.blocked_entities"), GUI_TEXTURE);
        loadConfig();
        buildAutocompleteSource();
    }

    private void loadConfig() {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            ItemStack heldItem = player.getMainHandItem();
            this.config = heldItem.getOrDefault(FloraFaunaRegistry.MOB_BARRIER_CONFIG.get(), MobBarrierConfig.DEFAULT);
        } else {
            this.config = MobBarrierConfig.DEFAULT;
        }
    }

    private void buildAutocompleteSource() {
        autocompleteSource.clear();
        for (Identifier id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            autocompleteSource.add(id.toString());
        }
        autocompleteSource.addAll(MobBarrierConfig.COMMON_ENTITY_TAGS);
    }

    // ===================
    // Widget Setup
    // ===================

    @Override
    protected void initContent() {
        int x = getContentX();
        int y = getContentY();

        // Input field
        inputField = new EditBox(font, x + INPUT_X_OFFSET, y + INPUT_Y_OFFSET, INPUT_WIDTH, INPUT_HEIGHT,
                Component.translatable("gui.florafauna.mob_barrier.input_hint"));
        inputField.setMaxLength(90);
        inputField.setHint(Component.translatable("gui.florafauna.mob_barrier.input_hint"));
        inputField.setResponder(this::onInputChanged);
        inputField.setFocused(true);
        inputField.setBordered(false);
        inputField.setTextColor(CommonColors.WHITE);
        addRenderableWidget(inputField);

        // Add button
        addRenderableWidget(Button.builder(
                Component.literal("+"), //.translatable("gui.florafauna.mob_barrier.add"),
                this::onAddClicked
        ).bounds(x + INPUT_WIDTH + ADD_BUTTON_GAP, y + INPUT_Y_OFFSET - 2, REMOVE_BUTTON_SIZE, INPUT_HEIGHT - 4).build());

        // Remove buttons for each visible entry
        createRemoveButtons();

        // Block vision toggle button
        addRenderableWidget(Button.builder(
                getBlockVisionButtonText(),
                this::onBlockVisionToggled
        ).bounds(x, y + TOGGLE_Y_OFFSET, TOGGLE_WIDTH, TOGGLE_HEIGHT).build());
    }

    private Component getBlockVisionButtonText() {
        String stateKey = config.blockVision()
                ? "gui.florafauna.mob_barrier.block_vision.on"
                : "gui.florafauna.mob_barrier.block_vision.off";
        return Component.translatable(stateKey);
    }

    private void onBlockVisionToggled(Button button) {
        config = config.withBlockVision(!config.blockVision());
        sendConfigToServer();
        rebuildWidgets();
    }

    private void createRemoveButtons() {
        int x = getContentX();
        int listY = getContentY() + LIST_Y_OFFSET;
        int removeX = x + LIST_ENTRY_WIDTH + LIST_ENTRY_GAP;
        int rowHeight = LIST_ENTRY_HEIGHT + LIST_ENTRY_GAP;

        List<String> entries = getEntries();
        int visibleCount = Math.min(MAX_VISIBLE_ENTRIES, entries.size() - scrollOffset);

        for (int i = 0; i < visibleCount; i++) {
            int index = scrollOffset + i;
            if (index >= entries.size()) break;

            String entry = entries.get(index);
            int entryY = listY + i * rowHeight;

            addRenderableWidget(Button.builder(Component.literal("X"), btn -> onRemoveClicked(entry))
                    .bounds(removeX, entryY, REMOVE_BUTTON_SIZE, REMOVE_BUTTON_SIZE).build());
        }
    }

    /** Combines entity IDs and tags into one list for display */
    private List<String> getEntries() {
        List<String> entries = new ArrayList<>();
        entries.addAll(config.entityIds());
        entries.addAll(config.entityTags());
        return entries;
    }

    // ===================
    // Event Handlers
    // ===================

    private void onInputChanged(String input) {
        suggestions.clear();
        suggestionScrollOffset = 0;

        if (!input.isEmpty()) {
            String lowerInput = input.toLowerCase();
            for (String entry : autocompleteSource) {
                if (entry.toLowerCase().contains(lowerInput)) {
                    suggestions.add(entry);
                    if (suggestions.size() >= 50) break;
                }
            }
        }
    }

    private void onAddClicked(Button button) {
        String input = inputField.getValue().trim();
        if (input.isEmpty()) return;

        if (input.startsWith("#")) {
            config = config.withAddedEntityTag(input);
        } else {
            config = config.withAddedEntityId(input);
        }

        inputField.setValue("");
        suggestions.clear();
        sendConfigToServer();
        rebuildWidgets();
    }

    private void onRemoveClicked(String entry) {
        if (entry.startsWith("#")) {
            config = config.withRemovedEntityTag(entry);
        } else {
            config = config.withRemovedEntityId(entry);
        }

        // Adjust scroll if needed
        List<String> entries = getEntries();
        if (scrollOffset > 0 && scrollOffset >= entries.size()) {
            scrollOffset = Math.max(0, entries.size() - MAX_VISIBLE_ENTRIES);
        }

        sendConfigToServer();
        rebuildWidgets();
    }

    private void onSuggestionClicked(String suggestion) {
        inputField.setValue(suggestion);
        suggestions.clear();
        inputField.setFocused(true);
    }

    private void sendConfigToServer() {
        ClientPacketDistributor.sendToServer(new UpdateMobBarrierConfigPayload(config));
    }

    // ===================
    // Mouse Input
    // ===================

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (!suggestions.isEmpty() && handleSuggestionClick(event.x(), event.y())) {
            return true;
        }
        if (handleScrollbarClick(event.x(), event.y())) {
            return true;
        }
        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (isDraggingScrollbar) {
            updateScrollFromMouse(event.y(), true);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (isDraggingScrollbar) {
            isDraggingScrollbar = false;
            rebuildWidgets(); // Rebuild to sync remove buttons with new scroll position
            return true;
        }
        return super.mouseReleased(event);
    }

    private boolean handleScrollbarClick(double mouseX, double mouseY) {
        List<String> entries = getEntries();
        if (entries.size() <= MAX_VISIBLE_ENTRIES) {
            return false; // No scrollbar
        }

        int[] scrollbarBounds = getScrollbarBounds();
        int trackX = scrollbarBounds[0];
        int trackY = scrollbarBounds[1];
        int trackHeight = scrollbarBounds[2];

        if (mouseX >= trackX && mouseX <= trackX + SCROLLER_WIDTH &&
            mouseY >= trackY && mouseY <= trackY + trackHeight) {
            isDraggingScrollbar = true;
            updateScrollFromMouse(mouseY, false);
            return true;
        }
        return false;
    }

    private void updateScrollFromMouse(double mouseY, boolean isDragging) {
        List<String> entries = getEntries();
        int maxScroll = Math.max(0, entries.size() - MAX_VISIBLE_ENTRIES);
        if (maxScroll == 0) return;

        int[] scrollbarBounds = getScrollbarBounds();
        int trackY = scrollbarBounds[1];
        int trackHeight = scrollbarBounds[2];

        // Calculate scroll position from mouse Y (like creative menu)
        int scrollableHeight = trackHeight - SCROLLER_HEIGHT;
        if (scrollableHeight <= 0) return;

        double relativeY = mouseY - trackY - SCROLLER_HEIGHT / 2.0;
        double scrollPercent = Math.max(0, Math.min(1, relativeY / scrollableHeight));
        int newOffset = (int) Math.round(scrollPercent * maxScroll);

        if (newOffset != scrollOffset) {
            scrollOffset = newOffset;
            // Only rebuild widgets when not dragging (on click or release)
            if (!isDragging) {
                rebuildWidgets();
            }
        }
    }

    /** Returns [trackX, trackY, trackHeight] */
    private int[] getScrollbarBounds() {
        int x = getContentX();
        int listY = getContentY() + LIST_Y_OFFSET;
        int rowHeight = LIST_ENTRY_HEIGHT + LIST_ENTRY_GAP;
        int trackX = x + LIST_ENTRY_WIDTH + LIST_ENTRY_GAP + REMOVE_BUTTON_SIZE + LIST_ENTRY_GAP;
        int trackHeight = MAX_VISIBLE_ENTRIES * rowHeight - LIST_ENTRY_GAP;
        return new int[]{trackX, listY, trackHeight};
    }

    private boolean handleSuggestionClick(double mouseX, double mouseY) {
        int suggestionsX = getContentX() + 2;
        int suggestionsY = getContentY() + INPUT_Y_OFFSET + INPUT_HEIGHT + 2;

        int visibleCount = Math.min(MAX_SUGGESTIONS, suggestions.size() - suggestionScrollOffset);
        for (int i = 0; i < visibleCount; i++) {
            int y = suggestionsY + i * SUGGESTION_HEIGHT;
            if (mouseX >= suggestionsX && mouseX <= suggestionsX + SUGGESTION_WIDTH &&
                mouseY >= y && mouseY <= y + SUGGESTION_HEIGHT) {
                onSuggestionClicked(suggestions.get(suggestionScrollOffset + i));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Autocomplete scroll takes priority
        if (!suggestions.isEmpty() && handleSuggestionScroll(mouseX, mouseY, scrollY)) {
            return true;
        }

        // Main list scroll
        if (handleListScroll(mouseX, mouseY, scrollY)) {
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean handleListScroll(double mouseX, double mouseY, double scrollY) {
        List<String> entries = getEntries();
        if (entries.size() <= MAX_VISIBLE_ENTRIES) {
            return false; // No scrolling needed
        }

        int x = getContentX();
        int listY = getContentY() + LIST_Y_OFFSET;
        int listHeight = MAX_VISIBLE_ENTRIES * (LIST_ENTRY_HEIGHT + LIST_ENTRY_GAP);
        int listWidth = LIST_ENTRY_WIDTH + LIST_ENTRY_GAP + REMOVE_BUTTON_SIZE + LIST_ENTRY_GAP + SCROLLER_WIDTH;

        if (mouseX >= x && mouseX <= x + listWidth &&
            mouseY >= listY && mouseY <= listY + listHeight) {
            int maxScroll = Math.max(0, entries.size() - MAX_VISIBLE_ENTRIES);
            scrollOffset = Math.max(0, Math.min(scrollOffset - (int) scrollY, maxScroll));
            rebuildWidgets();
            return true;
        }
        return false;
    }

    private boolean handleSuggestionScroll(double mouseX, double mouseY, double scrollY) {
        int suggestionsX = getContentX() + 2;
        int suggestionsY = getContentY() + INPUT_Y_OFFSET + INPUT_HEIGHT + 2;
        int suggestionsHeight = Math.min(MAX_SUGGESTIONS, suggestions.size()) * SUGGESTION_HEIGHT;

        if (mouseX >= suggestionsX && mouseX <= suggestionsX + SUGGESTION_WIDTH &&
            mouseY >= suggestionsY && mouseY <= suggestionsY + suggestionsHeight) {
            int maxScroll = Math.max(0, suggestions.size() - MAX_SUGGESTIONS);
            suggestionScrollOffset = Math.max(0, Math.min(suggestionScrollOffset - (int) scrollY, maxScroll));
            return true;
        }
        return false;
    }

    // ===================
    // Rendering
    // ===================

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getContentX();
        int y = getContentY();

        // Title
        graphics.drawString(font, title, leftPos + TITLE_X_OFFSET, topPos + TITLE_Y_OFFSET, CommonColors.DARK_GRAY, false);

        // Entry list
        renderEntryList(graphics, x, y + LIST_Y_OFFSET, mouseX, mouseY);

        // Autocomplete dropdown (renders on top)
        renderSuggestions(graphics, mouseX, mouseY);
    }

    private void renderEntryList(GuiGraphics graphics, int x, int listY, int mouseX, int mouseY) {
        List<String> entries = getEntries();

        if (entries.isEmpty()) {
            graphics.drawString(font,
                    Component.translatable("gui.florafauna.mob_barrier.no_entries"),
                    x, listY, CommonColors.GRAY, true);
            return;
        }

        int rowHeight = LIST_ENTRY_HEIGHT + LIST_ENTRY_GAP;
        int visibleCount = Math.min(MAX_VISIBLE_ENTRIES, entries.size() - scrollOffset);
        for (int i = 0; i < visibleCount; i++) {
            int index = scrollOffset + i;
            if (index >= entries.size()) break;

            String entry = entries.get(index);
            int entryY = listY + i * rowHeight;

            // Background (alternating colors)
            int bgColor = (i % 2 == 0) ? CommonColors.GRAY : CommonColors.LIGHT_GRAY;
            graphics.fill(x, entryY + 0, x + LIST_ENTRY_WIDTH, entryY + LIST_ENTRY_HEIGHT, bgColor);

            // Text
            String displayText = truncateText(entry, LIST_ENTRY_MAX_CHARS);
            graphics.drawString(font, displayText, x + 4, entryY + 3, CommonColors.WHITE, true);
        }

        // Render scrollbar if needed
        if (entries.size() > MAX_VISIBLE_ENTRIES) {
            renderScrollbar(graphics, entries.size(), mouseX, mouseY);
        }
    }

    private void renderScrollbar(GuiGraphics graphics, int totalEntries, int mouseX, int mouseY) {
        int[] bounds = getScrollbarBounds();
        int trackX = bounds[0];
        int trackY = bounds[1];
        int trackHeight = bounds[2];

        // Calculate scroller position (like creative menu)
        int maxScroll = totalEntries - MAX_VISIBLE_ENTRIES;
        int scrollableHeight = trackHeight - SCROLLER_HEIGHT;
        int scrollerY = trackY + (maxScroll > 0 ? scrollOffset * scrollableHeight / maxScroll : 0);

        // Draw scroller sprite (same as creative menu)
        boolean canScroll = maxScroll > 0;
        Identifier sprite = canScroll ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, trackX + 4, scrollerY + 1, SCROLLER_WIDTH, SCROLLER_HEIGHT);

        // Change cursor when hovering over scrollbar (like creative menu)
        if (canScroll && mouseX >= trackX && mouseX <= trackX + SCROLLER_WIDTH &&
            mouseY >= trackY && mouseY <= trackY + trackHeight) {
            graphics.requestCursor(isDraggingScrollbar ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
        }
    }

    private void renderSuggestions(GuiGraphics graphics, int mouseX, int mouseY) {
        if (suggestions.isEmpty()) return;

        int x = getContentX() + 2;
        int y = getContentY() + INPUT_Y_OFFSET + INPUT_HEIGHT + 2;

        int visibleCount = Math.min(MAX_SUGGESTIONS, suggestions.size() - suggestionScrollOffset);
        int bgHeight = visibleCount * SUGGESTION_HEIGHT + 2;

        // Background
        graphics.fill(x - 1, y - 1, x + SUGGESTION_WIDTH + 1, y + bgHeight, CommonColors.BLACK);

        // Entries
        for (int i = 0; i < visibleCount; i++) {
            int index = suggestionScrollOffset + i;
            String suggestion = suggestions.get(index);
            int entryY = y + i * SUGGESTION_HEIGHT;

            boolean hovered = mouseX >= x && mouseX <= x + SUGGESTION_WIDTH &&
                              mouseY >= entryY && mouseY <= entryY + SUGGESTION_HEIGHT;

            int bgColor = hovered ? CommonColors.GRAY : CommonColors.DARK_GRAY;
            graphics.fill(x, entryY, x + SUGGESTION_WIDTH, entryY + SUGGESTION_HEIGHT - 2, bgColor);

            String displayText = truncateText(suggestion, SUGGESTION_MAX_CHARS);
            graphics.drawString(font, displayText, x + 2, entryY + 2, CommonColors.WHITE, true);
        }

        // Scroll indicator
        if (suggestions.size() > MAX_SUGGESTIONS) {
            String indicator = String.format("(%d/%d)", suggestionScrollOffset + 1, suggestions.size() - MAX_SUGGESTIONS + 1);
            graphics.drawString(font, indicator, x + SUGGESTION_WIDTH - 30, y + bgHeight + 2, CommonColors.GRAY, true);
        }
    }

    private String truncateText(String text, int maxChars) {
        return text.length() > maxChars ? text.substring(0, maxChars - 3) + "..." : text;
    }
}
