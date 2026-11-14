package net.j40climb.florafauna.common.item.abilities.multiblock;

import net.j40climb.florafauna.common.item.abilities.data.MiningModeData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;

import java.util.HashSet;
import java.util.Set;

/** Completes an accepted center break. Every neighbor goes through the normal player mining pipeline. */
public final class MultiBlockBreaker {
    private MultiBlockBreaker() {}

    public static void finish(ServerPlayer player, ItemStack tool, MiningModeData mode,
                              BlockPos center, MultiBlockPatterns.Plan plan) {
        ServerLevel level = player.level();
        Set<BlockPos> mined = new HashSet<>();
        mined.add(center);
        for (BlockPos pos : plan.blocks()) {
            if (player.getMainHandItem() != tool || tool.isEmpty()) break;
            if (pos.equals(center) || !MultiBlockPatterns.isMineable(level, player, tool, mode, pos)) continue;
            var before = level.getBlockState(pos);
            if (player.gameMode.destroyBlock(pos) && !level.getBlockState(pos).is(before.getBlock())) mined.add(pos);
        }
        for (BlockPos pos : plan.stairs()) {
            if (plan.blocks().contains(pos) && !mined.contains(pos)) continue;
            if (!MultiBlockPatterns.canInteract(level, player, pos) || !level.isEmptyBlock(pos)) continue;
            var snapshot = BlockSnapshot.create(level.dimension(), level, pos);
            if (level.setBlock(pos, plan.stairState(), Block.UPDATE_ALL) && EventHooks.onBlockPlace(player, snapshot, Direction.UP)) {
                snapshot.restore();
            }
        }
    }
}
