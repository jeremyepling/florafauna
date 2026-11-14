package net.j40climb.florafauna.common.entity.frenchie;

import net.j40climb.florafauna.FloraFauna;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/** A small airmail card that leaves the dog and its surroundings visible. */
public final class DeliveryScreen extends Screen {
    private static final Identifier PARCEL = Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "frenchie/parcel");
    private static final int PAPER = 0xFFFFEED2;
    private static final int INK = 0xFF293448;
    private static final int BLUE = 0xFF385976;
    private static final int RED = 0xFFA34743;
    private static final int RULE = 0xFFBDA987;
    private static final int PAD = 10;
    private static final int ROW = 19;

    private final DeliveryPopupPayload data;
    private final Component description;
    private int left, top, cardWidth, cardHeight, footer;
    private int textTop, textWidth;
    private int page, pages;

    public DeliveryScreen(DeliveryPopupPayload data) {
        super(label(data.preview().isPresent() ? "confirm_title" : "title"));
        this.data = data;
        description = data.preview().map(preview -> {
            var destination = preview.destination();
            var clock = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(minecraft.getLanguageManager().getJavaLocale());
            String arrival = clock.format(ZonedDateTime.now().plusSeconds(preview.seconds()));
            return Component.empty().append(label("address", destination.label())).append("\n")
                    .append(label("eta", duration(preview.seconds()), arrival));
        }).orElseGet(() -> Component.empty().append(label(data.choices().isEmpty() ? "no_destinations" : "instructions")));
    }

    private static Component label(String key, Object... args) {
        return Component.translatable("frenchie.florafauna.delivery." + key, args);
    }

    private static Component duration(int seconds) {
        if (seconds < 60) return label("seconds", seconds);
        int minutes = seconds / 60;
        if (minutes < 60) return label("minutes", minutes, seconds % 60);
        return label("hours", minutes / 60, minutes % 60, seconds % 60);
    }

    @Override
    protected void init() {
        cardWidth = Math.min(212, width - 12);
        left = (width - cardWidth) / 2;
        int inside = cardWidth - PAD * 2;
        boolean addressed = data.preview().isPresent();
        textTop = addressed ? 12 : 37;
        textWidth = addressed ? inside - 28 : inside;
        int bodyEnd = Math.max(32, textTop + font.wordWrapHeight(description, textWidth)) + 5;
        int perPage = Math.clamp((height - bodyEnd - 37) / ROW, 1, 4);
        int count = data.choices().size();
        pages = Math.max(1, (count + perPage - 1) / perPage);
        page = Math.min(page, pages - 1);
        int first = page * perPage;
        int last = Math.min(count, first + perPage);
        cardHeight = bodyEnd + Math.min(perPage, count) * ROW + 31;
        top = (height - cardHeight) / 2;
        int x = left + PAD;
        int y = top + bodyEnd;
        for (int i = first; i < last; i++) {
            var choice = data.choices().get(i);
            button(x, y, inside, choice.label(), false, () -> respond("choose " + choice.address()));
            y += ROW;
        }
        footer = top + cardHeight - 24;
        int cancelWidth = Math.min(64, inside / 2 - 2);
        button(x, footer, cancelWidth, CommonComponents.GUI_CANCEL, false, this::onClose);
        data.preview().ifPresent(preview -> button(x + cancelWidth + 4, footer, inside - cancelWidth - 4,
                label("confirm"), true, () -> respond("send " + preview.destination().address())));
        if (pages > 1) {
            int end = left + cardWidth - PAD;
            var prev = button(end - 38, footer, 17, Component.literal("<"), false, () -> changePage(-1));
            prev.active = page > 0;
            prev.setTooltip(Tooltip.create(Component.translatable("gui.recipebook.previous_page")));
            var next = button(end - 17, footer, 17, Component.literal(">"), false, () -> changePage(1));
            next.active = page < pages - 1;
            next.setTooltip(Tooltip.create(Component.translatable("gui.recipebook.next_page")));
        }
    }

    private void changePage(int delta) {
        page += delta;
        rebuildWidgets();
    }

    private Button button(int x, int y, int w, Component message, boolean primary, Runnable action) {
        var button = new Button(x, y, w, 16, message, b -> action.run(), narration -> narration.get()) {
            @Override
            protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
                boolean highlight = isHoveredOrFocused() && active;
                int color = !active ? 0xFFD7C9AF : highlight ? INK : primary ? BLUE : 0xFFE8D5B4;
                g.fill(x, y, x + w, y + 16, color);
                g.outline(x, y, w, 16, highlight || primary ? INK : RULE);
                String full = message.getString();
                String text = font.width(full) <= w - 6 ? full : font.plainSubstrByWidth(full, w - 6 - font.width("…")) + "…";
                g.text(font, text, x + (w - font.width(text)) / 2, y + 4,
                        !active ? 0xFF716958 : primary || highlight ? 0xFFFFF7DE : INK, false);
            }
        };
        if (font.width(message) > w - 6) button.setTooltip(Tooltip.create(message));
        return addRenderableWidget(button);
    }

    @Override
    public Component getNarrationMessage() {
        return Component.empty().append(title).append(". ").append(description);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int right = left + cardWidth, bottom = top + cardHeight;
        g.fill(left + 2, top + 2, right + 2, bottom + 2, 0xAA292123);
        g.fill(left, top, right, bottom, PAPER);
        g.outline(left, top, cardWidth, cardHeight, INK);
        // Diagonal tricolour selvage, like the edge of a French airmail envelope.
        for (int x = left + 2, band = 0; x < right - 4; x += 11, band++) {
            for (int dy = 0; dy < 3; dy++) {
                g.fill(x + dy, top + 2 + dy, Math.min(right - 2, x + dy + 5), top + 3 + dy, band % 2 == 0 ? BLUE : RED);
            }
        }
        int stampX = right - PAD - 22, stampY = top + 9;
        g.fill(stampX + 1, stampY, stampX + 21, stampY + 21, BLUE);
        g.fill(stampX, stampY + 1, stampX + 22, stampY + 20, BLUE);
        for (int i = 3; i < 20; i += 4) {
            g.fill(stampX + i, stampY, stampX + i + 2, stampY + 1, PAPER);
            g.fill(stampX + i, stampY + 20, stampX + i + 2, stampY + 21, PAPER);
            g.fill(stampX, stampY + i, stampX + 1, stampY + i + 2, PAPER);
            g.fill(stampX + 21, stampY + i, stampX + 22, stampY + i + 2, PAPER);
        }
        g.fill(stampX + 3, stampY + 3, stampX + 19, stampY + 18, 0xFFD7DDCE);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, PARCEL, stampX + 3, stampY + 2, 16, 16);
        if (data.preview().isEmpty()) {
            g.text(font, title, left + PAD, top + 16, INK, false);
            g.fill(left + PAD, top + 32, right - PAD, top + 33, RULE);
        }
        g.textWithWordWrap(font, description, left + PAD, top + textTop, textWidth, INK, false);
        g.fill(left + PAD, footer - 4, right - PAD, footer - 3, RULE);
        if (pages > 1) {
            String number = (page + 1) + "/" + pages;
            g.text(font, number, right - PAD - 42 - font.width(number), footer + 4, INK, false);
        }
    }

    private void respond(String command) {
        if (minecraft.player != null) minecraft.player.connection.sendCommand("frenchiedelivery " + data.dog() + " " + command);
        minecraft.setScreen(null);
    }

    @Override
    public void onClose() {
        respond("dismiss");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
