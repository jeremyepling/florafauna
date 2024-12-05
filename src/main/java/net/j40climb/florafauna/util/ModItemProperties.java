package net.j40climb.florafauna.util;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.component.DataComponentTypes;
import net.j40climb.florafauna.item.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;


// TODO This should move to item model data components in 1.21.X
public class ModItemProperties {
    public static void addCustomItemProperties() {
        ItemProperties.register(ModItems.DATA_TABLET.get(), ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "on"),
                (pStack, pLevel, pEntity, pSeed) -> pStack.get(DataComponentTypes.FOUND_BLOCK) != null ? 1f : 0f);
    }
}