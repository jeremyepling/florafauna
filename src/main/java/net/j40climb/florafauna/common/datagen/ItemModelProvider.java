package net.j40climb.florafauna.common.datagen;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredItem;

public class ItemModelProvider extends net.neoforged.neoforge.client.model.generators.ItemModelProvider {
    public ItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, FloraFauna.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.TOMATO.get());

        //withExistingParent(ModItems.GECKO_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        //withExistingParent(ModItems.LIZARD_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.FRENCHIE_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
    }


    private ItemModelBuilder handheldItem(DeferredItem<Item> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/handheld")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID,"item/" + item.getId().getPath()));
    }

}
