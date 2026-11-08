package net.j40climb.florafauna.common.datagen;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.wood.WoodType;
import net.j40climb.florafauna.common.block.wood.WoodBlockSet;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.j40climb.florafauna.setup.FloraFaunaTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.references.BlockIds;
import net.minecraft.references.BlockItemId;
import net.minecraft.references.BlockItemIds;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

import java.util.concurrent.CompletableFuture;

public class FloraFaunaBlockTagsProvider extends BlockTagsProvider {
    public FloraFaunaBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, FloraFauna.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Pod replaceable blocks - blocks that storage pods can replace when spawning
        this.tag(FloraFaunaTags.Blocks.POD_REPLACEABLE)
                .add(BlockItemIds.SHORT_GRASS.block())
                .add(BlockItemIds.TALL_GRASS.block())
                .add(BlockItemIds.FERN.block())
                .add(BlockItemIds.LARGE_FERN.block())
                .add(BlockItemIds.DEAD_BUSH.block())
                .add(BlockItemIds.SEAGRASS.block())
                .add(BlockIds.TALL_SEAGRASS)
                .add(BlockItemIds.DANDELION.block())
                .add(BlockItemIds.POPPY.block())
                .add(BlockItemIds.BLUE_ORCHID.block())
                .add(BlockItemIds.ALLIUM.block())
                .add(BlockItemIds.AZURE_BLUET.block())
                .add(BlockItemIds.RED_TULIP.block())
                .add(BlockItemIds.ORANGE_TULIP.block())
                .add(BlockItemIds.WHITE_TULIP.block())
                .add(BlockItemIds.PINK_TULIP.block())
                .add(BlockItemIds.OXEYE_DAISY.block())
                .add(BlockItemIds.CORNFLOWER.block())
                .add(BlockItemIds.LILY_OF_THE_VALLEY.block())
                .add(BlockItemIds.WITHER_ROSE.block())
                .add(BlockItemIds.SUNFLOWER.block())
                .add(BlockItemIds.LILAC.block())
                .add(BlockItemIds.ROSE_BUSH.block())
                .add(BlockItemIds.PEONY.block())
                .add(BlockItemIds.SNOW.block());

        // Reflective blocks - endermen see their reflection and become scared
        this.tag(FloraFaunaTags.Blocks.REFLECTIVE_BLOCKS)
                // Polished stones
                .add(BlockItemIds.POLISHED_ANDESITE.block())
                .add(BlockItemIds.POLISHED_DIORITE.block())
                .add(BlockItemIds.POLISHED_GRANITE.block())
                .add(BlockItemIds.POLISHED_DEEPSLATE.block())
                .add(BlockItemIds.POLISHED_BLACKSTONE.block())
                .add(BlockItemIds.POLISHED_BASALT.block())
                // Glass blocks
                .add(BlockItemIds.GLASS.block())
                .addAll(BlockItemIds.STAINED_GLASS.asList().stream().map(BlockItemId::block))
                .add(BlockItemIds.TINTED_GLASS.block())
                // Ice blocks
                .add(BlockItemIds.ICE.block())
                .add(BlockItemIds.PACKED_ICE.block())
                .add(BlockItemIds.BLUE_ICE.block())
                // Other reflective surfaces
                .add(BlockItemIds.OBSIDIAN.block())
                .add(BlockItemIds.CRYING_OBSIDIAN.block());

        // Cold blocks - blazes fear when surrounded by these (along with snow golems)
        this.tag(FloraFaunaTags.Blocks.COLD_BLOCKS)
                // Snow blocks
                .add(BlockItemIds.SNOW.block())
                .add(BlockItemIds.SNOW_BLOCK.block())
                .add(BlockItemIds.POWDER_SNOW.block())
                // Ice blocks
                .add(BlockItemIds.ICE.block())
                .add(BlockItemIds.PACKED_ICE.block())
                .add(BlockItemIds.BLUE_ICE.block())
                .add(BlockIds.FROSTED_ICE);

        // Fear line-of-sight transparent blocks - mobs can see fear sources through these
        this.tag(FloraFaunaTags.Blocks.FEAR_LOS_TRANSPARENT)
                // Glass blocks
                .add(BlockItemIds.GLASS.block())
                .addAll(BlockItemIds.STAINED_GLASS.asList().stream().map(BlockItemId::block))
                .add(BlockItemIds.TINTED_GLASS.block())
                // Glass panes
                .add(BlockItemIds.GLASS_PANE.block())
                .addAll(BlockItemIds.STAINED_GLASS_PANE.asList().stream().map(BlockItemId::block));

        // Ferric poppy plantable blocks - iron golems can plant ferric poppies on these
        this.tag(FloraFaunaTags.Blocks.FERRIC_POPPY_PLANTABLE)
                .add(BlockItemIds.DIRT.block())
                .add(BlockItemIds.GRASS_BLOCK.block())
                .add(BlockItemIds.PODZOL.block())
                .add(BlockItemIds.COARSE_DIRT.block())
                .add(BlockItemIds.ROOTED_DIRT.block())
                .add(BlockItemIds.FARMLAND.block())
                .add(BlockItemIds.MUD.block())
                .add(BlockItemIds.MUDDY_MANGROVE_ROOTS.block())
                .add(BlockItemIds.MOSS_BLOCK.block())
                .add(BlockItemIds.MYCELIUM.block());

        // Iron garden storage - containers iron golems can deposit poppies into
        this.tag(FloraFaunaTags.Blocks.IRON_GARDEN_STORAGE)
                .add(BlockItemIds.CHEST.block())
                .add(BlockItemIds.TRAPPED_CHEST.block())
                .add(BlockItemIds.BARREL.block())
                .add(BlockItemIds.HOPPER.block())
                .add(BlockItemIds.SHULKER_BOX.block())
                .addAll(BlockItemIds.DYED_SHULKER_BOX.asList().stream().map(BlockItemId::block));

        // Mineable with pickaxe
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(FloraFaunaRegistry.HUSK.key())
                .add(FloraFaunaRegistry.STORAGE_ANCHOR.key());

        // Mineable with axe (organic/wood-like)
        this.tag(BlockTags.MINEABLE_WITH_AXE)
                .add(FloraFaunaRegistry.ITEM_INPUT.key())
                .add(FloraFaunaRegistry.MOB_INPUT.key())
                .add(FloraFaunaRegistry.MOB_OUTPUT.key());

        // Wood blocks - add to appropriate tags
        for (WoodType woodType : WoodType.values()) {
            WoodBlockSet wood = woodType.getBlockSet();

            // Logs tag (used for smelting to charcoal, etc.)
            this.tag(BlockTags.LOGS)
                    .add(wood.log().key())
                    .add(wood.strippedLog().key())
                    .add(wood.wood().key())
                    .add(wood.strippedWood().key());

            // Logs that burn (for fire spreading)
            this.tag(BlockItemTags.LOGS_THAT_BURN.block())
                    .add(wood.log().key())
                    .add(wood.strippedLog().key())
                    .add(wood.wood().key())
                    .add(wood.strippedWood().key());

            // Planks tag
            this.tag(BlockTags.PLANKS)
                    .add(wood.planks().key());

            // Slabs tag
            this.tag(BlockTags.WOODEN_SLABS)
                    .add(wood.slab().key());

            // Fences tag
            this.tag(BlockTags.WOODEN_FENCES)
                    .add(wood.fence().key());
            this.tag(BlockTags.FENCES)
                    .add(wood.fence().key());

            // Fence gates tag
            this.tag(BlockTags.FENCE_GATES)
                    .add(wood.fenceGate().key());

            // Mineable with axe
            this.tag(BlockTags.MINEABLE_WITH_AXE)
                    .add(wood.log().key())
                    .add(wood.strippedLog().key())
                    .add(wood.wood().key())
                    .add(wood.strippedWood().key())
                    .add(wood.planks().key())
                    .add(wood.slab().key())
                    .add(wood.fence().key())
                    .add(wood.fenceGate().key());
        }
    }
}
