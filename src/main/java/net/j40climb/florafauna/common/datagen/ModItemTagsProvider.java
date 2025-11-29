package net.j40climb.florafauna.common.datagen;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.item.ModItems;
import net.j40climb.florafauna.common.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;

import java.util.concurrent.CompletableFuture;

public class ModItemTagsProvider extends ItemTagsProvider {

    public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTags) {
        super(output, lookupProvider, blockTags, FloraFauna.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ItemTags.PICKAXES)
                .add(ModItems.ENERGY_HAMMER.get())
                .addTag(ModTags.Items.PAXELS);
        tag(ItemTags.SHOVELS)
                .addTag(ModTags.Items.PAXELS);
        tag(ItemTags.AXES)
                .addTag(ModTags.Items.PAXELS);
        tag(ItemTags.HOES)
                .addTag(ModTags.Items.PAXELS);

        // Create my own tags and add my custom items to them
        tag(ModTags.Items.PAXELS)
                .add(ModItems.ENERGY_HAMMER.get()); // This allows all the enchantments to work
        tag(ModTags.Items.HAMMERS);

    }

}
