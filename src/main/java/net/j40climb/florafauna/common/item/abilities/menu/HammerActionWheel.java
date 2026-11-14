package net.j40climb.florafauna.common.item.abilities.menu;

import net.j40climb.florafauna.common.item.abilities.data.HammerAction;
import net.j40climb.florafauna.common.item.abilities.networking.SelectHammerActionPayload;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.EnumMap;

/** Five keyboard-accessible buttons around the currently selected right-click action. */
public final class HammerActionWheel extends Screen {
    private final int slot;
    private final HammerAction selected;
    private final ItemStack tool;
    private final EnumMap<HammerAction, Button> buttons = new EnumMap<>(HammerAction.class);

    public HammerActionWheel(int slot, ItemStack tool) {
        super(Component.translatable("gui.florafauna.hammer_action.title"));
        this.slot = slot;
        this.selected = tool.getOrDefault(FloraFaunaRegistry.HAMMER_ACTION, HammerAction.AUTO);
        this.tool = tool.copy();
    }

    @Override
    protected void init() {
        buttons.clear();
        HammerAction[] actions = HammerAction.values();
        for (int i = 0; i < actions.length; i++) {
            HammerAction action = actions[i];
            double angle = 2 * Math.PI * i / actions.length - Math.PI / 2;
            int centerX = width / 2 + (int) Math.round(Math.cos(angle) * 59);
            int centerY = height / 2 + (int) Math.round(Math.sin(angle) * 49);
            Button button = Button.builder(action.label(), ignored -> choose(action))
                    .bounds(centerX - 23, centerY - 9, 46, 18).build();
            buttons.put(action, addRenderableWidget(button));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        var current = buttons.get(selected);
        graphics.outline(current.getX() - 1, current.getY() - 1, current.getWidth() + 2, current.getHeight() + 2, 0xff83dcbc);
        graphics.item(tool, width / 2 - 8, height / 2 - 13);
        graphics.centeredText(font, selected.label(), width / 2, height / 2 + 7, 0xffb6f5db);
        graphics.centeredText(font, title, width / 2, height / 2 - 76, 0xffffffff);
    }

    private void choose(HammerAction action) {
        ClientPacketDistributor.sendToServer(new SelectHammerActionPayload(slot, action));
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
