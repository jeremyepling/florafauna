package net.j40climb.florafauna.common.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FloraFaunaDataGenerators {
    public static void gatherClientData(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // Client-side data
        generator.addProvider(true, new FloraFaunaModelProvider(packOutput));

        // Server-side data (included here so runData generates everything in one pass)
        generator.addProvider(true, new LootTableProvider(packOutput, Collections.emptySet(),
                List.of(new LootTableProvider.SubProviderEntry(FloraFaunaBlockLootTableProvider::new, LootContextParamSets.BLOCK)), lookupProvider));
        generator.addProvider(true, new FloraFaunaBlockTagsProvider(packOutput, lookupProvider));
        generator.addProvider(true, new FloraFaunaItemTagsProvider(packOutput, lookupProvider));
        generator.addProvider(true, new FloraFaunaEntityTypeTagsProvider(packOutput, lookupProvider));
        generator.addProvider(true, new FloraFaunaRecipeProvider(packOutput, lookupProvider));

        // Test structures for GameTest
        generator.addProvider(true, new TestStructureProvider(packOutput, lookupProvider));
    }

    public static void gatherServerData(GatherDataEvent.Server event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // Server-side data only (no models - those are client-side)
        generator.addProvider(true , new LootTableProvider(packOutput, Collections.emptySet(),
                List.of(new LootTableProvider.SubProviderEntry(FloraFaunaBlockLootTableProvider::new, LootContextParamSets.BLOCK)), lookupProvider));
        generator.addProvider(true, new FloraFaunaBlockTagsProvider(packOutput, lookupProvider));
        generator.addProvider(true, new FloraFaunaItemTagsProvider(packOutput, lookupProvider));
        generator.addProvider(true, new FloraFaunaEntityTypeTagsProvider(packOutput, lookupProvider));
        generator.addProvider(true, new FloraFaunaRecipeProvider(packOutput, lookupProvider));
    }
}