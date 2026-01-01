package net.j40climb.florafauna.common.block.containmentchamber;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.client.gui.BaseContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ContainmentChamberScreen extends BaseContainerScreen<ContainmentChamberMenu> {
    private static final Identifier GUI_TEXTURE =
            Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "textures/gui/containment_chamber/convert_item_gui.png");

    public ContainmentChamberScreen(ContainmentChamberMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI_TEXTURE);
    }
}
