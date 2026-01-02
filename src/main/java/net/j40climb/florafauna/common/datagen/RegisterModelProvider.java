package net.j40climb.florafauna.common.datagen;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.RegisterBlocks;
import net.j40climb.florafauna.common.block.wood.ModWoodType;
import net.j40climb.florafauna.common.block.wood.WoodBlockSet;
import net.j40climb.florafauna.common.item.RegisterItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;

import java.util.Collections;
import java.util.stream.Stream;

public class RegisterModelProvider extends ModelProvider {
    public RegisterModelProvider(PackOutput output) {
        super(output, FloraFauna.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {

        ///  ITEM MODELS

        itemModels.generateFlatItem(RegisterItems.TOMATO.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RegisterItems.DORMANT_SYMBIOTE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RegisterItems.SYMBIOTE_STEW.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RegisterItems.GECKO_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RegisterItems.LIZARD_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RegisterItems.FRENCHIE_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);

        itemModels.itemModelOutput.register(
            RegisterItems.HAMMER.get(),
            new ClientItem(
                // Defines the model to render
                new BlockModelWrapper.Unbaked(
                    // Points to a model JSON relative to the 'models' directory
                    // Located at 'assets/examplemod/models/item/example_item.json'
                    ModelLocationUtils.getModelLocation(RegisterItems.HAMMER.get()),
                    Collections.emptyList()
                ),
                // Defines some settings to use during the rendering process
                new ClientItem.Properties(
                    true, false, 1.0F
                )
            )
        );

        ///  BLOCK MODELS

        blockModels.createTrivialCube(RegisterBlocks.TEAL_MOSS_BLOCK.get());
        blockModels.createTrivialCube(RegisterBlocks.SYMBIOTE_CONTAINMENT_CHAMBER.get());
        blockModels.createTrivialCube(RegisterBlocks.COCOON_CHAMBER.get());

        // Copper Golem Barrier - invisible block, model not actually rendered
        blockModels.createTrivialCube(RegisterBlocks.COPPER_GOLEM_BARRIER.get());

        // Wood blocks - iterate through all wood types
        for (ModWoodType woodType : ModWoodType.values()) {
            WoodBlockSet wood = woodType.getBlockSet();

            // Logs use log column model (expects {name}.png for side, {name}_top.png for ends)
            blockModels.createRotatedPillarWithHorizontalVariant(
                    wood.log().get(),
                    TexturedModel.COLUMN_ALT,
                    TexturedModel.COLUMN_HORIZONTAL_ALT
            );
            blockModels.createRotatedPillarWithHorizontalVariant(
                    wood.strippedLog().get(),
                    TexturedModel.COLUMN_ALT,
                    TexturedModel.COLUMN_HORIZONTAL_ALT
            );

            // Wood blocks use log side texture on all faces
            blockModels.woodProvider(wood.log().get()).wood(wood.wood().get());
            blockModels.woodProvider(wood.strippedLog().get()).wood(wood.strippedWood().get());

            // Planks use simple cube
            blockModels.createTrivialCube(wood.planks().get());

            // Slab, fence, fence gate derive from planks using family pattern
            blockModels.familyWithExistingFullBlock(wood.planks().get())
                    .slab(wood.slab().get())
                    .fence(wood.fence().get())
                    .fenceGate(wood.fenceGate().get());
        }
    }

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        return RegisterItems.ITEMS.getEntries().stream().filter(x -> !x.equals(RegisterItems.HAMMER));
    }
}
