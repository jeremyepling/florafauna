package net.j40climb.florafauna.common.datagen;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.wood.ModWoodType;
import net.j40climb.florafauna.common.block.wood.WoodBlockSet;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
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

        itemModels.generateFlatItem(FloraFaunaRegistry.DORMANT_SYMBIOTE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(FloraFaunaRegistry.SYMBIOTE_STEW.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(FloraFaunaRegistry.GECKO_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(FloraFaunaRegistry.LIZARD_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(FloraFaunaRegistry.FRENCHIE_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);

        itemModels.itemModelOutput.register(
            FloraFaunaRegistry.HAMMER.get(),
            new ClientItem(
                // Defines the model to render
                new BlockModelWrapper.Unbaked(
                    // Points to a model JSON relative to the 'models' directory
                    // Located at 'assets/examplemod/models/item/example_item.json'
                    ModelLocationUtils.getModelLocation(FloraFaunaRegistry.HAMMER.get()),
                    Collections.emptyList()
                ),
                // Defines some settings to use during the rendering process
                new ClientItem.Properties(
                    true, false, 1.0F
                )
            )
        );

        ///  BLOCK MODELS

        blockModels.createTrivialCube(FloraFaunaRegistry.TEAL_MOSS_BLOCK.get());
        blockModels.createTrivialCube(FloraFaunaRegistry.SYMBIOTE_CONTAINMENT_CHAMBER.get());
        blockModels.createTrivialCube(FloraFaunaRegistry.COCOON_CHAMBER.get());

        // Copper Golem Barrier - invisible block, model not actually rendered
        blockModels.createTrivialCube(FloraFaunaRegistry.COPPER_GOLEM_BARRIER.get());

        // Husk block - uses a simple cube for now (blockstate variants handled by json)
        blockModels.createTrivialCube(FloraFaunaRegistry.HUSK.get());

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
        return FloraFaunaRegistry.ITEMS.getEntries().stream().filter(x -> !x.equals(FloraFaunaRegistry.HAMMER));
    }
}
