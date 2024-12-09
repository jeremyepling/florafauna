package net.j40climb.florafauna.common.items.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.HashSet;
import java.util.Set;

import static net.j40climb.florafauna.client.ClientUtils.raycastFromPlayer;

public interface DiggerTool {
    void setBlockTags(TagKey<Block> blockTags);
    TagKey<Block> getBlockTags(TagKey<Block> blockTags);


    static Set<BlockPos> getBlocksToBeBroken(int range, BlockPos initalBlockPos, Player player) {
        Set<BlockPos> positions = new HashSet<>();
        float maxDistance = 6f; // Define the maximum raycast distance
        BlockHitResult traceResult = (BlockHitResult) raycastFromPlayer(player, maxDistance);

        switch (traceResult.getType()) {
            case HitResult.Type.BLOCK:
                // Handle block hit
                /*
                Iterate through the ranges in the X and Y and then expand it based on where the player is look.
                Expanding up and down, vs side-to-side.
                */

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
            case HitResult.Type.ENTITY:
                // Handle entity hit
                return positions;
            case HitResult.Type.MISS:
                // Handle miss
                return positions;
        }
        return positions;
    }
}
