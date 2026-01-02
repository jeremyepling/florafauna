package net.j40climb.florafauna.common.datagen;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.wood.ModWoodType;
import net.j40climb.florafauna.common.block.wood.WoodBlockSet;
import net.j40climb.florafauna.common.util.FloraFaunaTags;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

import java.util.concurrent.CompletableFuture;

public class RegisterBlockTagsProvider extends BlockTagsProvider {
    public RegisterBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, FloraFauna.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(FloraFaunaTags.Blocks.MINEABLE_WITH_HAMMER)
                .addTag(BlockTags.MINEABLE_WITH_PICKAXE)
                .addTag(BlockTags.MINEABLE_WITH_AXE)
                .addTag(BlockTags.MINEABLE_WITH_SHOVEL);

        // Husk block - mineable with pickaxe
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(FloraFaunaRegistry.HUSK.get());

        // Wood blocks - add to appropriate tags
        for (ModWoodType woodType : ModWoodType.values()) {
            WoodBlockSet wood = woodType.getBlockSet();

            // Logs tag (used for smelting to charcoal, etc.)
            this.tag(BlockTags.LOGS)
                    .add(wood.log().get())
                    .add(wood.strippedLog().get())
                    .add(wood.wood().get())
                    .add(wood.strippedWood().get());

            // Logs that burn (for fire spreading)
            this.tag(BlockTags.LOGS_THAT_BURN)
                    .add(wood.log().get())
                    .add(wood.strippedLog().get())
                    .add(wood.wood().get())
                    .add(wood.strippedWood().get());

            // Planks tag
            this.tag(BlockTags.PLANKS)
                    .add(wood.planks().get());

            // Slabs tag
            this.tag(BlockTags.WOODEN_SLABS)
                    .add(wood.slab().get());

            // Fences tag
            this.tag(BlockTags.WOODEN_FENCES)
                    .add(wood.fence().get());
            this.tag(BlockTags.FENCES)
                    .add(wood.fence().get());

            // Fence gates tag
            this.tag(BlockTags.FENCE_GATES)
                    .add(wood.fenceGate().get());

            // Mineable with axe
            this.tag(BlockTags.MINEABLE_WITH_AXE)
                    .add(wood.log().get())
                    .add(wood.strippedLog().get())
                    .add(wood.wood().get())
                    .add(wood.strippedWood().get())
                    .add(wood.planks().get())
                    .add(wood.slab().get())
                    .add(wood.fence().get())
                    .add(wood.fenceGate().get());
        }
    }
}
