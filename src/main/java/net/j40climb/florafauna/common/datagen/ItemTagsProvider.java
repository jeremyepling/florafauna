package net.j40climb.florafauna.common.datagen;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.item.ModItems;
import net.j40climb.florafauna.common.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ItemTagsProvider extends net.minecraft.data.tags.ItemTagsProvider {
    public ItemTagsProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pLookupProvider,
                            CompletableFuture<TagLookup<Block>> pBlockTags, @Nullable ExistingFileHelper existingFileHelper) {
        super(pOutput, pLookupProvider, pBlockTags, FloraFauna.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Tag for items that the magic block can transform
        tag(ModTags.Items.TRANSFORMABLE_ITEMS)
                .add(ModItems.BLACK_OPAL.get())
                .add(Items.COAL)
                .add(Items.DANDELION)
                .add(Items.COMPASS);

        tag(ItemTags.SWORDS)
                .add(ModItems.BLACK_OPAL_SWORD.get());
        tag(ItemTags.PICKAXES)
                .add(ModItems.BLACK_OPAL_PICKAXE.get())
                .add(ModItems.BLACK_OPAL_HAMMER.get())
                .add(ModItems.ENERGY_HAMMER.get())
                .addTag(ModTags.Items.PAXELS);
        tag(ItemTags.SHOVELS)
                .add(ModItems.BLACK_OPAL_SHOVEL.get())
                .addTag(ModTags.Items.PAXELS);
        tag(ItemTags.AXES)
                .add(ModItems.BLACK_OPAL_AXE.get())
                .addTag(ModTags.Items.PAXELS);
        tag(ItemTags.HOES)
                .add(ModItems.BLACK_OPAL_HOE.get())
                .addTag(ModTags.Items.PAXELS);

        // Create my own tags and add my custom items to them
        tag(ModTags.Items.PAXELS)
                .add(ModItems.BLACK_OPAL_PAXEL.get())
                .add(ModItems.ENERGY_HAMMER.get()); // This allows all the enchantments to work
        tag(ModTags.Items.HAMMERS)
                .add(ModItems.BLACK_OPAL_HAMMER.get());
        tag(Tags.Items.MINING_TOOL_TOOLS)
                .add(ModItems.BLACK_OPAL_PICKAXE.get())
                .add(ModItems.BLACK_OPAL_PAXEL.get())
                .add(ModItems.BLACK_OPAL_HAMMER.get());

        tag(Tags.Items.MELEE_WEAPON_TOOLS)
                .add(ModItems.BLACK_OPAL_SWORD.get())
                .add(ModItems.BLACK_OPAL_AXE.get());

        tag(ItemTags.CLUSTER_MAX_HARVESTABLES)
                .add(ModItems.BLACK_OPAL_PICKAXE.get())
                .add(ModItems.BLACK_OPAL_PAXEL.get());

    }

}
