package net.j40climb.florafauna.item.custom;

import net.j40climb.florafauna.component.DataComponentTypes;
import net.j40climb.florafauna.component.HitRangeData;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HammerItem extends DiggerItem {

    public HammerItem(Tier pTier, Properties pProperties) {
        // New tags in 1.21.30 make this easier #minecraft:iron_tier_destructible for all diggers or
        // #minecraft:is_pickaxe_item_destructible for pickaxe
        super(pTier, BlockTags.MINEABLE_WITH_PICKAXE, pProperties.component(DataComponentTypes.HIT_RANGE, new HitRangeData(3, 1)));
    }

    public static List<BlockPos> getBlocksToBeDestroyed(int range, BlockPos initalBlockPos, ServerPlayer player) {
        List<BlockPos> positions = new ArrayList<>();
        BlockHitResult traceResult = player.level().clip(new ClipContext(player.getEyePosition(1f),
                        (player.getEyePosition(1f).add(player.getViewVector(1f).scale(6f))),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if(traceResult.getType() == HitResult.Type.MISS) {
            return positions;
        }

        /* Iterate through the ranges in the X and Y and then expand it based on where the player is look. Expanding up
        and down, vs side-to-side.*/
        for(int x = -range; x <= range; x++) {
            for(int y = -range; y <= range; y++) {
                if(traceResult.getDirection() == Direction.DOWN || traceResult.getDirection() == Direction.UP)
                    positions.add(new BlockPos(initalBlockPos.getX() + x, initalBlockPos.getY(), initalBlockPos.getZ() + y));
                if(traceResult.getDirection() == Direction.NORTH || traceResult.getDirection() == Direction.SOUTH)
                    positions.add(new BlockPos(initalBlockPos.getX() + x, initalBlockPos.getY() + y, initalBlockPos.getZ()));
                if(traceResult.getDirection() == Direction.EAST || traceResult.getDirection() == Direction.WEST)
                    positions.add(new BlockPos(initalBlockPos.getX(), initalBlockPos.getY() + y, initalBlockPos.getZ() + x));
            }
        }
        return positions;
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        if(pStack.get(DataComponentTypes.HIT_RANGE.get()) != null) {
            pTooltipComponents.add(Component.literal("Mining size " + Objects.requireNonNull(pStack.get(DataComponentTypes.HIT_RANGE)).range()));
        }

        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
    }
}
