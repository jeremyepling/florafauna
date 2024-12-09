package net.j40climb.florafauna.item.custom;

import net.j40climb.florafauna.common.items.interfaces.DiggerTool;
import net.j40climb.florafauna.component.DataComponentTypes;
import net.j40climb.florafauna.component.HitRangeData;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.*;

public class HammerItem extends DiggerItem implements DiggerTool {
    private TagKey<Block> BLOCK_TAG_KEY = BlockTags.MINEABLE_WITH_PICKAXE;

    public HammerItem(Tier pTier, Properties pProperties) {
        // New tags in 1.21.30 make this easier #minecraft:iron_tier_destructible for all diggers or
        // #minecraft:is_pickaxe_item_destructible for pickaxe
        super(pTier, BlockTags.MINEABLE_WITH_PICKAXE, pProperties.component(DataComponentTypes.HIT_RANGE, new HitRangeData(3, 1)));
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        if(pStack.get(DataComponentTypes.HIT_RANGE.get()) != null) {
            pTooltipComponents.add(Component.literal("Mining size " + Objects.requireNonNull(pStack.get(DataComponentTypes.HIT_RANGE)).range()));
        }
        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
    }

    @Override
    public void setBlockTags(TagKey<Block> blockTags) {
        BLOCK_TAG_KEY = blockTags;
    }

    @Override
    public TagKey<Block> getBlockTags(TagKey<Block> blockTags) {
        return BLOCK_TAG_KEY;
    }
}
