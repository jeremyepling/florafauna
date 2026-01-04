package net.j40climb.florafauna.common.datagen;

import net.j40climb.florafauna.common.block.wood.WoodType;
import net.j40climb.florafauna.common.block.wood.WoodBlockSet;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.Set;

public class FloraFaunaBlockLootTableProvider extends BlockLootSubProvider {
    protected FloraFaunaBlockLootTableProvider(HolderLookup.Provider provider) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);
    }

    @Override
    protected void generate() {
        dropSelf(FloraFaunaRegistry.TEAL_MOSS_BLOCK.get());
        dropSelf(FloraFaunaRegistry.SYMBIOTE_CONTAINMENT_CHAMBER.get());
        dropSelf(FloraFaunaRegistry.COCOON_CHAMBER.get());
        dropSelf(FloraFaunaRegistry.MOB_BARRIER.get());

        // Husk drops nothing - items are retrieved via interaction
        add(FloraFaunaRegistry.HUSK.get(), noDrop());

        // Item Input System blocks
        dropSelf(FloraFaunaRegistry.STORAGE_ANCHOR.get());
        dropSelf(FloraFaunaRegistry.ITEM_INPUT.get());
        dropSelf(FloraFaunaRegistry.FIELD_RELAY.get());

        // Mining Anchor System blocks
        dropSelf(FloraFaunaRegistry.TIER1_MINING_ANCHOR.get());
        dropSelf(FloraFaunaRegistry.TIER2_MINING_ANCHOR.get());
        // Feral pods drop nothing - items spill on break (handled in block code)
        add(FloraFaunaRegistry.TIER1_POD.get(), noDrop());
        // Hardened pods drop themselves with inventory (handled in block code)
        dropSelf(FloraFaunaRegistry.TIER2_POD.get());

        // Wood blocks - all drop themselves (slabs use special loot table)
        for (WoodType woodType : WoodType.values()) {
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
        return FloraFaunaRegistry.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
