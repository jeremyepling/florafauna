package net.j40climb.florafauna.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.ArrayList;
import java.util.List;

public class HammerItem extends DiggerItem {
    public HammerItem(Tier pTier, Properties pProperties) {
        // New tags in 1.21.30 make this easier #minecraft:iron_tier_destructible for all diggers or
        // #minecraft:is_pickaxe_item_destructible for pickaxe
        super(pTier, BlockTags.MINEABLE_WITH_PICKAXE, pProperties);
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
}
