package net.j40climb.florafauna.common.datagen;

import net.j40climb.florafauna.common.block.wood.ModWoodType;
import net.j40climb.florafauna.common.block.wood.WoodBlockSet;
import net.j40climb.florafauna.setup.ModRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.Set;

public class RegisterBlockLootTableProvider extends BlockLootSubProvider {
    protected RegisterBlockLootTableProvider(HolderLookup.Provider provider) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);
    }

    @Override
    protected void generate() {
        dropSelf(ModRegistry.TEAL_MOSS_BLOCK.get());
        dropSelf(ModRegistry.SYMBIOTE_CONTAINMENT_CHAMBER.get());
        dropSelf(ModRegistry.COCOON_CHAMBER.get());
        dropSelf(ModRegistry.COPPER_GOLEM_BARRIER.get());

        // Husk drops nothing - items are retrieved via interaction
        add(ModRegistry.HUSK.get(), noDrop());

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
        return ModRegistry.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
