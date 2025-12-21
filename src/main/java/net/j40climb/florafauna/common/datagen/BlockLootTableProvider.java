package net.j40climb.florafauna.common.datagen;

import net.j40climb.florafauna.common.block.ModBlocks;
import net.j40climb.florafauna.common.block.wood.ModWoodType;
import net.j40climb.florafauna.common.block.wood.WoodBlockSet;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.Set;

public class BlockLootTableProvider extends BlockLootSubProvider {
    protected BlockLootTableProvider(HolderLookup.Provider provider) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.TEAL_MOSS_BLOCK.get());
        dropSelf(ModBlocks.SYMBIOTE_CONTAINMENT_CHAMBER.get());
        dropSelf(ModBlocks.COPPER_GOLEM_BARRIER.get());

        // Wood blocks - all drop themselves (slabs use special loot table)
        for (ModWoodType woodType : ModWoodType.values()) {
            WoodBlockSet wood = woodType.getBlockSet();
            dropSelf(wood.log().get());
            dropSelf(wood.strippedLog().get());
            dropSelf(wood.wood().get());
            dropSelf(wood.strippedWood().get());
            dropSelf(wood.planks().get());
            add(wood.slab().get(), createSlabItemTable(wood.slab().get()));
            dropSelf(wood.fence().get());
            dropSelf(wood.fenceGate().get());
        }
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
