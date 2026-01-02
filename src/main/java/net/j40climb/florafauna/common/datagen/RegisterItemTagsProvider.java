package net.j40climb.florafauna.common.datagen;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.wood.ModWoodType;
import net.j40climb.florafauna.common.block.wood.WoodBlockSet;
import net.j40climb.florafauna.common.util.FloraFaunaTags;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.ItemTags;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

import java.util.concurrent.CompletableFuture;

public class RegisterItemTagsProvider extends ItemTagsProvider {

    public RegisterItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, FloraFauna.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ItemTags.PICKAXES)
                .add(FloraFaunaRegistry.HAMMER.get())
                .addTag(FloraFaunaTags.Items.HAMMERS);
        tag(ItemTags.SHOVELS)
                .addTag(FloraFaunaTags.Items.HAMMERS);
        tag(ItemTags.AXES)
                .addTag(FloraFaunaTags.Items.HAMMERS);
        tag(ItemTags.HOES)
                .addTag(FloraFaunaTags.Items.HAMMERS);

        // Create custom hammer tag for multi-tool functionality
        tag(FloraFaunaTags.Items.HAMMERS)
                .add(FloraFaunaRegistry.HAMMER.get());

        // Wood block items - add to appropriate tags
        for (ModWoodType woodType : ModWoodType.values()) {
            WoodBlockSet wood = woodType.getBlockSet();

            // Logs item tag
            tag(ItemTags.LOGS)
                    .add(wood.log().get().asItem())
                    .add(wood.strippedLog().get().asItem())
                    .add(wood.wood().get().asItem())
                    .add(wood.strippedWood().get().asItem());

            // Logs that burn item tag
            tag(ItemTags.LOGS_THAT_BURN)
                    .add(wood.log().get().asItem())
                    .add(wood.strippedLog().get().asItem())
                    .add(wood.wood().get().asItem())
                    .add(wood.strippedWood().get().asItem());

            // Planks item tag
            tag(ItemTags.PLANKS)
                    .add(wood.planks().get().asItem());

            // Slabs item tag
            tag(ItemTags.WOODEN_SLABS)
                    .add(wood.slab().get().asItem());

            // Fences item tag
            tag(ItemTags.WOODEN_FENCES)
                    .add(wood.fence().get().asItem());
            tag(ItemTags.FENCES)
                    .add(wood.fence().get().asItem());

            // Fence gates item tag
            tag(ItemTags.FENCE_GATES)
                    .add(wood.fenceGate().get().asItem());
        }
    }

}
