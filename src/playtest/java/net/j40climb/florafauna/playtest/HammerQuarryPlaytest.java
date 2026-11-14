package net.j40climb.florafauna.playtest;

import net.j40climb.florafauna.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna.common.item.abilities.data.MiningSpeed;
import net.j40climb.florafauna.setup.ClientSetup;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Set;

/** Deterministic rock, including an entrance and a chunk boundary. Runs only in the disposable copy. */
final class HammerQuarryPlaytest {
    private static final BlockPos FIRST_FACE = new BlockPos(8, 196, 12);
    private static final int FLIGHT = 5;

    static void start(Playtest t) {
        t.server("Build the 28×28 quarry with 48 layers of rock and an entrance", player -> {
            ServerLevel level = player.level();
            for (BlockPos pos : BlockPos.betweenClosed(0, 151, 4, 27, 198, 31)) {
                boolean entrance = pos.getX() == FIRST_FACE.getX() && pos.getZ() < FIRST_FACE.getZ()
                        && pos.getY() >= FIRST_FACE.getY() - 1;
                level.setBlock(pos, (entrance ? Blocks.AIR : Blocks.STONE).defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            player.teleportTo(level, 8.5, 195, 9.5, Set.of(), 0f, 0f, true);
        });
        for (MiningShape shape : new MiningShape[]{MiningShape.FLAT_7X7,
                MiningShape.SHAPELESS, MiningShape.TUNNEL_UP, MiningShape.TUNNEL_DOWN}) {
            t.client("Cycle to " + shape, mc -> KeyMapping.click(ClientSetup.CYCLE_MINING_MODE_KEY.get().getKey()));
            t.awaitClient("Receive " + shape + " from the server", mc ->
                    mc.player.getMainHandItem().get(FloraFaunaRegistry.MULTI_BLOCK_MINING).shape() == shape);
        }
        cutThenWalk(t, 0);
        cutThenWalk(t, 1);
        t.screenshot("hammer-quarry-before-restart");
    }

    static void resume(Playtest t) {
        t.server("Verify both mined flights and the held Hammer configuration after restart", player -> {
            verifyFlight(player.level(), face(0));
            verifyFlight(player.level(), face(1));
            var hammer = player.getMainHandItem();
            Playtest.require(hammer.is(FloraFaunaRegistry.HAMMER.get())
                    && hammer.get(FloraFaunaRegistry.MULTI_BLOCK_MINING).shape() == MiningShape.TUNNEL_DOWN
                    && hammer.get(FloraFaunaRegistry.TOOL_CONFIG).miningSpeed() == MiningSpeed.INSTABREAK,
                    "Lost the selected Hammer or its configuration on rejoin");
        });
        cutThenWalk(t, 2);
        t.screenshot("hammer-quarry-after-restart");
    }

    private static BlockPos face(int flight) {
        return FIRST_FACE.offset(0, -FLIGHT * flight, FLIGHT * flight);
    }

    private static void cutThenWalk(Playtest t, int flight) {
        BlockPos face = face(flight);
        String label = "flight " + (flight + 1);
        t.awaitClient("Aim at the next intact rock face for " + label, mc -> {
            mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, face.getCenter().add(0, 0, -0.5));
            return mc.player.onGround() && mc.hitResult instanceof BlockHitResult hit
                    && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(face)
                    && hit.getDirection() == Direction.NORTH && mc.level.getBlockState(face).is(Blocks.STONE);
        });
        t.client("Strike the wall for " + label, mc ->
                Playtest.require(mc.gameMode.startDestroyBlock(face, Direction.NORTH), "Refused to mine " + face));
        t.awaitServer("Receive the final stair of " + label, p ->
                p.level().getBlockState(face.offset(0, -6, 4)).is(Blocks.COBBLESTONE_STAIRS));
        t.server("Check stair orientation, overhead clearance, and untouched rock for " + label,
                p -> verifyFlight(p.level(), face));
        t.client("Walk down " + label, mc -> {
            mc.player.setYRot(0);
            mc.player.setXRot(0);
            mc.options.keyUp.setDown(true);
        });
        t.awaitClient("Reach the next landing on foot for " + label, mc -> {
            if (mc.player.getZ() < face.getZ() + 3.6) return false;
            mc.options.keyUp.setDown(false);
            return true;
        });
        t.awaitServer("Settle on the lower stairs for " + label, p ->
                p.onGround() && p.getY() <= face.getY() - 3 && p.getY() >= face.getY() - 6
                        && p.getZ() >= face.getZ() + 3.5 && p.getZ() < face.getZ() + 5);
    }

    private static void verifyFlight(ServerLevel level, BlockPos face) {
        for (int step = 0; step < FLIGHT; step++) {
            BlockPos floor = face.offset(0, -2 - step, step);
            var stair = level.getBlockState(floor);
            Playtest.require(stair.is(Blocks.COBBLESTONE_STAIRS) && stair.getValue(StairBlock.FACING) == Direction.NORTH
                    && stair.getValue(StairBlock.HALF) == Half.BOTTOM, "Wrong stair at " + floor + ": " + stair);
            for (int h = 1; h <= 4; h++) {
                Playtest.require(level.getBlockState(floor.above(h)).isAir(), "Blocked headroom at " + floor.above(h));
            }
            for (BlockPos guard : new BlockPos[]{floor.below(), floor.above(5), floor.west(), floor.east()}) {
                if (guard.getY() <= 198) {
                    Playtest.require(level.getBlockState(guard).is(Blocks.STONE), "Damaged quarry boundary at " + guard);
                }
            }
        }
    }
}
