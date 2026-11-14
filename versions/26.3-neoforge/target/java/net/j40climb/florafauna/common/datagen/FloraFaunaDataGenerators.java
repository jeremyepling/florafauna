package net.j40climb.florafauna.common.datagen;

import net.j40climb.florafauna.common.block.wood.WoodTransformsProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.data.event.GatherDataRegistryEntriesEvent;

public class FloraFaunaDataGenerators {
    public static void gatherRegistries(GatherDataRegistryEntriesEvent event) {
        event.lootTable(new LootTableProvider.SubProviderEntry(FloraFaunaBlockLootTableProvider::new, LootContextParamSets.BLOCK));
        event.recipe(FloraFaunaRecipeProvider::new);
    }

    public static void gatherClientData(GatherDataEvent.Client event) {
        event.createProvider(FloraFaunaModelProvider::new);
        gatherServerProviders(event);
    }

    public static void gatherServerData(GatherDataEvent.Server event) {
        gatherServerProviders(event);
    }

    private static void gatherServerProviders(GatherDataEvent event) {
        event.createProvider(FloraFaunaBlockTagsProvider::new);
        event.createProvider(FloraFaunaItemTagsProvider::new);
        event.createProvider(FloraFaunaEntityTypeTagsProvider::new);
        event.createProvider(WoodTransformsProvider::new);
    }
}
