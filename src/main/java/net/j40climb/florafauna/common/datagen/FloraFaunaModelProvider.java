package net.j40climb.florafauna.common.datagen;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.wood.WoodType;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Collections;
import java.util.stream.Stream;

public class FloraFaunaModelProvider extends ModelProvider {
    public FloraFaunaModelProvider(PackOutput output) {
        super(output, FloraFauna.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        registerGeneratedItemModels(itemModels);
        registerCustomItemModels(itemModels);

        registerGeneratedBlockModels(blockModels);
        registerCustomBlockModels(blockModels);
    }

    // ============================================================
    // BLOCK MODELS (GENERATED)
    // ============================================================
    // These are simple models that can be data generated and have no custom json from blockbench
    private static void registerGeneratedBlockModels(BlockModelGenerators blockModels) {
        blockModels.createTrivialCube(FloraFaunaRegistry.TEAL_MOSS_BLOCK.get());
        blockModels.createTrivialCube(FloraFaunaRegistry.SYMBIOTE_CONTAINMENT_CHAMBER.get());
        blockModels.createTrivialCube(FloraFaunaRegistry.COCOON_CHAMBER.get());

        // Copper Golem Barrier - invisible block, model not actually rendered
        blockModels.createTrivialCube(FloraFaunaRegistry.COPPER_GOLEM_BARRIER.get());

        // Husk block - uses a simple cube for now (blockstate variants handled by json)
        blockModels.createTrivialCube(FloraFaunaRegistry.HUSK.get());

        // Item Input System blocks
        blockModels.createTrivialCube(FloraFaunaRegistry.STORAGE_ANCHOR.get());
        // ROOT_ITEM_INPUT has STATE property - all assets manually defined in resources

        // Wood blocks
        registerGeneratedWoodSetModels(blockModels);
    }

    private static void registerGeneratedWoodSetModels(BlockModelGenerators blockModels) {
        // Wood blocks - iterate through all wood types
        for (WoodType woodType : WoodType.values()) {
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

    // These are simple models that can be data generated and have no custom json from blockbench
    private static void registerGeneratedItemModels(ItemModelGenerators itemModels) {
        itemModels.generateFlatItem(FloraFaunaRegistry.DORMANT_SYMBIOTE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(FloraFaunaRegistry.SYMBIOTE_STEW.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(FloraFaunaRegistry.GECKO_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(FloraFaunaRegistry.LIZARD_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(FloraFaunaRegistry.FRENCHIE_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
    }

    // ============================================================
    // ITEM MODELS (CUSTOM / MANUAL)
    // ============================================================

    private static void registerCustomItemModels(ItemModelGenerators itemModels) {
        // Custom: HAMMER uses a manually-authored item model JSON (or a custom loader chain)
        registerClientItemFromModelJson(itemModels, FloraFaunaRegistry.HAMMER.get());
    }

    /**
     * Registers a ClientItem that points at a JSON model location for the item:
     * assets/florafauna/models/item/<itemname>.json
     */
    private static void registerClientItemFromModelJson(ItemModelGenerators itemModels, Item item) {
        itemModels.itemModelOutput.register(
                item,
                new ClientItem(
                        new BlockModelWrapper.Unbaked(
                                ModelLocationUtils.getModelLocation(item),
                                Collections.emptyList()
                        ),
                        new ClientItem.Properties(true, false, 1.0F)
                )
        );
    }

    // ============================================================
    // BLOCK MODELS (CUSTOM / MANUAL)
    // ============================================================

    private static void registerCustomBlockModels(BlockModelGenerators blockModels) {
        // This method intentionally does NOT generate anything by default.
        // It exists as the explicit “do not datagen” bucket so future-you
        // can see what is expected to be manual.

        // Examples of what belongs here (do NOT call blockModels.*):
        // - Multipart blockstates
        // - Variant-driven blockstates
        // - Blockbench-exported block models
        // - Connected overlays / special parent chains

        // Example from your comments:
        // ROOT_ITEM_INPUT has STATE property - all assets manually defined in resources
        // (Intentionally omitted from generation)
        // cube(blockModels, FloraFaunaRegistry.ROOT_ITEM_INPUT.get()); // <-- DO NOT DO THIS
    }

    /**
     * When you have any manual item or block models (Blockbench/custom parents/etc).
     * These tell the provider which items and blocks it is responsible for vs. the mod provides.
     */
    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        // Exclude HAMMER (has custom item model) and item inputs with STATE property (manually defined)
        return FloraFaunaRegistry.ITEMS.getEntries().stream()
                .filter(x -> !x.equals(FloraFaunaRegistry.HAMMER))
                .filter(x -> !x.getRegisteredName().equals("florafauna:item_input"))
                .filter(x -> !x.getRegisteredName().equals("florafauna:field_relay"));
    }

    @Override
    protected Stream<? extends Holder<Block>> getKnownBlocks() {
        // Exclude blocks with STATE property - assets manually defined in resources
        return FloraFaunaRegistry.BLOCKS.getEntries().stream()
                .filter(x -> !x.equals(FloraFaunaRegistry.ITEM_INPUT))
                .filter(x -> !x.equals(FloraFaunaRegistry.FIELD_RELAY));
    }
}
