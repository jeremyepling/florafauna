package net.j40climb.florafauna.item.custom;

import net.j40climb.florafauna.component.ModDataComponentTypes;
import net.j40climb.florafauna.component.MiningModeData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class HammerItem extends DiggerItem {
    public HammerItem(Tier pTier, Properties pProperties) {
        // New tags in 1.21.30 make this easier #minecraft:iron_tier_destructible for all diggers or
        // #minecraft:is_pickaxe_item_destructible for pickaxe

        // TODO Changing this to ModTags.Blocks.PAXEL_MINEABLE causes a max networking error
        super(pTier, BlockTags.MINEABLE_WITH_PICKAXE, pProperties
                .component(ModDataComponentTypes.MINING_MODE_DATA, new MiningModeData())
        );
    }

    //
    // Overrides for DiggerItem
    //

    @Override
    public @NotNull InteractionResult useOn(UseOnContext pContext) {
        if(!pContext.getLevel().isClientSide()) {
            Level level = pContext.getLevel();
            BlockPos blockpos = pContext.getClickedPos();
            Player player = pContext.getPlayer();
            if (player != null) {
                ItemStack hammerItemStack = player.getMainHandItem();

                // Get current mode and shapeId
                MiningModeData miningMode = hammerItemStack.getOrDefault(ModDataComponentTypes.MINING_MODE_DATA, new MiningModeData());
                int shapeId = miningMode.shape().id();

                // Go to next shape
                hammerItemStack.set(ModDataComponentTypes.MINING_MODE_DATA, MiningModeData.getNextMode(shapeId));

                // Output the change
                MiningModeData miningModeManager2 = hammerItemStack.getOrDefault(ModDataComponentTypes.MINING_MODE_DATA, new MiningModeData());
                player.displayClientMessage(Component.literal("New Mining Mode: " + miningModeManager2.shape().name()), true);
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack ItemStack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if(ItemStack.get(ModDataComponentTypes.MINING_MODE_DATA.get()) != null) {
            MiningModeData miningModeData = ItemStack.getOrDefault(ModDataComponentTypes.MINING_MODE_DATA, new MiningModeData());
            tooltipComponents.add(Component.literal("Mining shape:" + miningModeData.shape().name() + " radius:" + miningModeData.shape().getRadius()));
        }
        super.appendHoverText(ItemStack, context, tooltipComponents, tooltipFlag);
    }
}
