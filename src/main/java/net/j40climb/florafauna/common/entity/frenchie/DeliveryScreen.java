package net.j40climb.florafauna.common.entity.frenchie;

import net.j40climb.florafauna.FloraFauna;
import net.minecraft.ChatFormatting;
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
import java.util.Optional;

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
    private final Optional<Address> address;
    private final Component description;
    private int left, top, cardWidth, cardHeight, footer, inset;
    private int addressWidth, dimensionY, arrivalY;
    private int page, pages;

    private record Address(Component recipient, Component dimension, Component arrival) {}

    public DeliveryScreen(DeliveryPopupPayload data) {
        super(label(data.preview().isPresent() ? "confirm_title" : "title"));
        this.data = data;
        address = data.preview().map(preview -> {
            var clock = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(minecraft.getLanguageManager().getJavaLocale());
            String arrival = clock.format(ZonedDateTime.now().plusSeconds(preview.seconds()));
            var name = preview.destination().label().copy().withStyle(ChatFormatting.BOLD);
            var dimension = preview.dimension();
            return new Address(label("address", name),
                    Component.translatableWithFallback(dimension.toLanguageKey("dimension"), dimension.toString()),
                    label("eta", duration(preview.seconds()), arrival));
        });
        description = address.<Component>map(a -> Component.empty().append(a.recipient()).append("\n")
                .append(a.dimension()).append("\n").append(a.arrival()))
                .orElseGet(() -> label(data.choices().isEmpty() ? "no_destinations" : "instructions"));
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
        cardWidth = Math.min(address.isPresent() ? 232 : 212, width - 12);
        left = (width - cardWidth) / 2;
        inset = address.isPresent() ? 16 : PAD;
        int inside = cardWidth - inset * 2;
        int x = left + inset;
        if (address.isPresent()) {
            Address lines = address.get();
            addressWidth = inside - 32;
            dimensionY = 20 + font.wordWrapHeight(lines.recipient(), addressWidth) + 5;
            arrivalY = dimensionY + font.wordWrapHeight(lines.dimension(), addressWidth) + 18;
            int actionsY = arrivalY + font.wordWrapHeight(lines.arrival(), inside) + 20;
            cardHeight = actionsY + 20 + 14;
            top = (height - cardHeight) / 2;
            footer = top + actionsY;
            var send = label("confirm");
            int limit = (inside - 6) / 2;
            int cancelWidth = Math.min(limit, Math.max(58, font.width(CommonComponents.GUI_CANCEL) + 16));
            int sendWidth = Math.min(limit, Math.max(58, font.width(send) + 16));
            button(x, footer, cancelWidth, 20, CommonComponents.GUI_CANCEL, false, this::onClose);
            button(x + cancelWidth + 6, footer, sendWidth, 20, send, true,
                    () -> respond("send " + data.preview().orElseThrow().destination().address()));
            return;
        }
        int bodyEnd = 37 + font.wordWrapHeight(description, inside) + 5;
        int perPage = Math.clamp((height - bodyEnd - 37) / ROW, 1, 4);
        int count = data.choices().size();
        pages = Math.max(1, (count + perPage - 1) / perPage);
        page = Math.min(page, pages - 1);
        int first = page * perPage;
        int last = Math.min(count, first + perPage);
        cardHeight = bodyEnd + Math.min(perPage, count) * ROW + 31;
        top = (height - cardHeight) / 2;
        int y = top + bodyEnd;
        for (int i = first; i < last; i++) {
            var choice = data.choices().get(i);
            button(x, y, inside, 16, choice.label(), false, () -> respond("choose " + choice.address()));
            y += ROW;
        }
        footer = top + cardHeight - 24;
        int cancelWidth = Math.min(64, inside / 2 - 2);
        button(x, footer, cancelWidth, 16, CommonComponents.GUI_CANCEL, false, this::onClose);
        if (pages > 1) {
            int end = left + cardWidth - PAD;
            var prev = button(end - 38, footer, 17, 16, Component.literal("<"), false, () -> changePage(-1));
            prev.active = page > 0;
            prev.setTooltip(Tooltip.create(Component.translatable("gui.recipebook.previous_page")));
            var next = button(end - 17, footer, 17, 16, Component.literal(">"), false, () -> changePage(1));
            next.active = page < pages - 1;
            next.setTooltip(Tooltip.create(Component.translatable("gui.recipebook.next_page")));
        }
    }

    private void changePage(int delta) {
        page += delta;
        rebuildWidgets();
    }

    private Button button(int x, int y, int w, int h, Component message, boolean primary, Runnable action) {
        var button = new Button(x, y, w, h, message, b -> action.run(), narration -> narration.get()) {
            @Override
            protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
                boolean highlight = isHoveredOrFocused() && active;
                int color = !active ? 0xFFD7C9AF : highlight ? INK : primary ? BLUE : 0xFFE8D5B4;
                g.fill(x, y, x + w, y + h, color);
                g.outline(x, y, w, h, highlight || primary ? INK : RULE);
                String full = message.getString();
                String text = font.width(full) <= w - 6 ? full : font.plainSubstrByWidth(full, w - 6 - font.width("…")) + "…";
                g.text(font, text, x + (w - font.width(text)) / 2, y + (h - font.lineHeight + 1) / 2,
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
        int stampX = right - inset - 22, stampY = top + (address.isPresent() ? 14 : 9);
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
        int x = left + inset;
        int inside = cardWidth - 2 * inset;
        if (address.isPresent()) {
            Address lines = address.get();
            g.textWithWordWrap(font, lines.recipient(), x, top + 20, addressWidth, INK, false);
            g.textWithWordWrap(font, lines.dimension(), x, top + dimensionY, addressWidth, BLUE, false);
            g.textWithWordWrap(font, lines.arrival(), x, top + arrivalY, inside, INK, false);
        } else {
            g.text(font, title, left + PAD, top + 16, INK, false);
            g.fill(left + PAD, top + 32, right - PAD, top + 33, RULE);
            g.textWithWordWrap(font, description, x, top + 37, inside, INK, false);
        }
        int ruleY = footer - (address.isPresent() ? 10 : 4);
        g.fill(x, ruleY, right - inset, ruleY + 1, RULE);
        if (pages > 1) {
            String number = (page + 1) + "/" + pages;
            g.text(font, number, right - PAD - 42 - font.width(number), footer + 4, INK, false);
        }
    }

    private void respond(String command) {
        if (minecraft.player != null) minecraft.player.connection.sendCommand("frenchiedelivery " + data.dog() + " " + command);
        minecraft.gui.setScreen(null);
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
