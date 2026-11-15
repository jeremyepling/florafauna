package net.j40climb.florafauna.common.datagen;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.wood.WoodType;
import net.j40climb.florafauna.common.block.wood.WoodBlockSet;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.tags.ItemTags;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

import java.util.concurrent.CompletableFuture;

public class FloraFaunaItemTagsProvider extends ItemTagsProvider {

    public FloraFaunaItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, FloraFauna.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Hammer is a multi-tool - add to all tool tags
        tag(ItemTags.PICKAXES)
                .add(FloraFaunaRegistry.HAMMER.key());
        tag(ItemTags.SHOVELS)
                .add(FloraFaunaRegistry.HAMMER.key());
        tag(ItemTags.AXES)
                .add(FloraFaunaRegistry.HAMMER.key());
        tag(ItemTags.HOES)
                .add(FloraFaunaRegistry.HAMMER.key());

        // Wood block items - add to appropriate tags
        for (WoodType woodType : WoodType.values()) {
            WoodBlockSet wood = woodType.getBlockSet();

            // Logs item tag
            tag(ItemTags.LOGS)
                    .add(wood.log().get().asItem().builtInRegistryHolder().key())
                    .add(wood.strippedLog().get().asItem().builtInRegistryHolder().key())
                    .add(wood.wood().get().asItem().builtInRegistryHolder().key())
                    .add(wood.strippedWood().get().asItem().builtInRegistryHolder().key());

            // Logs that burn item tag
            tag(ItemTags.LOGS_THAT_BURN)
                    .add(wood.log().get().asItem().builtInRegistryHolder().key())
                    .add(wood.strippedLog().get().asItem().builtInRegistryHolder().key())
                    .add(wood.wood().get().asItem().builtInRegistryHolder().key())
                    .add(wood.strippedWood().get().asItem().builtInRegistryHolder().key());

            // Planks item tag
            tag(ItemTags.PLANKS)
                    .add(wood.planks().get().asItem().builtInRegistryHolder().key());

            // Slabs item tag
            tag(ItemTags.WOODEN_SLABS)
                    .add(wood.slab().get().asItem().builtInRegistryHolder().key());

            // Fences item tag
            tag(ItemTags.WOODEN_FENCES)
                    .add(wood.fence().get().asItem().builtInRegistryHolder().key());
            tag(BlockItemTags.FENCES.item())
                    .add(wood.fence().get().asItem().builtInRegistryHolder().key());

            // Fence gates item tag
            tag(ItemTags.FENCE_GATES)
                    .add(wood.fenceGate().get().asItem().builtInRegistryHolder().key());
        }
    }

}
