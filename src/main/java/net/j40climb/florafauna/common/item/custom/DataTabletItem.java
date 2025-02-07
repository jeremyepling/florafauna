package net.j40climb.florafauna.common.item.custom;

import net.j40climb.florafauna.common.component.ModDataComponentTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class DataTabletItem extends Item {
    public DataTabletItem(Properties pProperties) {
        super(pProperties);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        if(pPlayer.getItemInHand(pUsedHand).get(ModDataComponentTypes.FOUND_BLOCK) != null) {
            pPlayer.getItemInHand(pUsedHand).set(ModDataComponentTypes.FOUND_BLOCK, null);
        }
        return InteractionResultHolder.success(pPlayer.getItemInHand(pUsedHand));
    }

    @Override
    public boolean isFoil(ItemStack pStack) {
        return pStack.get(ModDataComponentTypes.FOUND_BLOCK) != null;
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        if(pStack.get(ModDataComponentTypes.FOUND_BLOCK) != null) {
            pTooltipComponents.add(Component.literal(pStack.get(ModDataComponentTypes.FOUND_BLOCK).getOutputString()));
        }
        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
    }
}