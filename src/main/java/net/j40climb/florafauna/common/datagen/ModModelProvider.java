package net.j40climb.florafauna.common.datagen;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.ModBlocks;
import net.j40climb.florafauna.common.item.ModItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;

import java.util.Collections;
import java.util.stream.Stream;

public class ModModelProvider extends ModelProvider {
    public ModModelProvider(PackOutput output) {
        super(output, FloraFauna.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {

        ///  ITEM MODELS

        itemModels.generateFlatItem(ModItems.TOMATO.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.GECKO_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.LIZARD_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.FRENCHIE_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);

        itemModels.itemModelOutput.register(
            ModItems.ENERGY_HAMMER.get(),
            new ClientItem(
                // Defines the model to render
                new BlockModelWrapper.Unbaked(
                    // Points to a model JSON relative to the 'models' directory
                    // Located at 'assets/examplemod/models/item/example_item.json'
                    ModelLocationUtils.getModelLocation(ModItems.ENERGY_HAMMER.get()),
                    Collections.emptyList()
                ),
                // Defines some settings to use during the rendering process
                new ClientItem.Properties(
                    true, false
                )
            )
        );

        ///  BLOCK MODELS

        blockModels.createTrivialCube(ModBlocks.BISMUTH_BLOCK.get());
    }

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        return ModItems.ITEMS.getEntries().stream().filter(x -> !x.is(ModItems.ENERGY_HAMMER));
    }
}
