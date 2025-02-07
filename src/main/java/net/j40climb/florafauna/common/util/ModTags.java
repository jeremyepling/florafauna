package net.j40climb.florafauna.common.util;

import net.j40climb.florafauna.FloraFauna;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> INCORRECT_FOR_BLACK_OPAL_TOOL = createTag("incorrect_for_black_opal_tool");
        public static final TagKey<Block> NEEDS_FOR_BLACK_OPAL_TOOL = createTag("needs_for_black_opal_tool");

        public static final TagKey<Block> MINEABLE_WITH_PAXEL = createTag("mineable/paxel");

        private static TagKey<Block> createTag(String name) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, name));
        }
    }

    public static class Items {
        public static final TagKey<Item> TRANSFORMABLE_ITEMS = createTag("transformable_items");
        public static final TagKey<Item> PAXELS = createTag("paxels");
        public static final TagKey<Item> HAMMERS = createTag("hammers");

        private static TagKey<Item> createTag(String name) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, name));
        }
    }
}
