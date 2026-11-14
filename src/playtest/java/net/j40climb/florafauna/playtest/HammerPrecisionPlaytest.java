package net.j40climb.florafauna.playtest;

import net.j40climb.florafauna.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna.common.item.abilities.multiblock.MultiBlockPatterns;
import net.j40climb.florafauna.setup.ClientSetup;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

final class HammerPrecisionPlaytest {
    static void steps(Playtest t, BlockPos wall) {
        t.client("Hold precision and strike the wall in the same tick", mc -> {
            ClientSetup.PRECISION_MINING_KEY.get().setDown(true);
            Playtest.require(mc.gameMode.startDestroyBlock(wall, Direction.NORTH), "Precision strike refused");
        });
        t.awaitServer("Verify precision broke only the center without changing the 3×3 setting", p -> {
            if (!p.level().getBlockState(wall).isAir()) return false;
            Playtest.require(p.getMainHandItem().get(FloraFaunaRegistry.MULTI_BLOCK_MINING).shape() == MiningShape.FLAT_3X3,
                    "Precision overwrote the selected mining mode");
            for (BlockPos pos : BlockPos.betweenClosed(wall.offset(-1, -1, 0), wall.offset(1, 1, 0))) {
                Playtest.require(pos.equals(wall) || p.level().getBlockState(pos).is(Blocks.STONE), "Precision mined " + pos);
            }
            return true;
        });
        t.server("Replace the center for the restored pattern", p -> p.level().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState()));
        t.awaitClient("Preview the single target while precision is held", mc -> mc.level.getBlockState(wall).is(Blocks.STONE)
                && MultiBlockPatterns.plan(mc.player, mc.player.getMainHandItem(), wall, Direction.NORTH).blocks().equals(List.of(wall)));
        t.client("Release precision and attack in the same tick", mc -> {
            ClientSetup.PRECISION_MINING_KEY.get().setDown(false);
            Playtest.require(mc.gameMode.startDestroyBlock(wall, Direction.NORTH), "3×3 strike refused after release");
        });
        t.awaitServer("Restore the full nine-block pattern on key release", p -> {
            for (BlockPos pos : BlockPos.betweenClosed(wall.offset(-1, -1, 0), wall.offset(1, 1, 0))) {
                if (!p.level().isEmptyBlock(pos)) return false;
            }
            return true;
        });
        t.server("Rebuild the wall for the outline and perimeter checks", p -> {
            for (BlockPos pos : BlockPos.betweenClosed(wall.offset(-1, -1, 0), wall.offset(1, 1, 0))) {
                p.level().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            }
        });
    }
}
