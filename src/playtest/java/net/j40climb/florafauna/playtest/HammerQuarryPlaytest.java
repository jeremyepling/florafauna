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
import net.minecraft.world.phys.Vec3;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Path;
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
            level.setBlockAndUpdate(FIRST_FACE.offset(0, -3, 1), Blocks.AIR.defaultBlockState());
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
        walkStraightTunnel(t);
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
        if (flight == 0) {
            t.awaitClient("Receive the empty tread position behind the quarry face", mc ->
                    mc.level.isEmptyBlock(face.offset(0, -3, 1)));
            t.screenshot("hammer-placement-outline");
            t.awaitClient("See the colored placement outline through the unbroken quarry face", mc ->
                    hasPlacementOutline(mc.gameDirectory.toPath().resolve("screenshots/hammer-placement-outline.png")));
        }
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

    private static boolean hasPlacementOutline(Path screenshot) {
        try {
            var image = ImageIO.read(screenshot.toFile());
            if (image == null) return false;
            int pixels = 0;
            // The camera faces bare stone. Exclude the hotbar and held tool from the sample.
            for (int y = image.getHeight() / 2; y < image.getHeight() * 4 / 5; y++) {
                for (int x = image.getWidth() * 2 / 5; x < image.getWidth() * 3 / 5; x++) {
                    int rgb = image.getRGB(x, y);
                    int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
                    if (g > b + 15 && b > r + 50) pixels++;
                }
            }
            Playtest.require(pixels >= 20, "Missing teal stair outline against the quarry wall, pixels=" + pixels);
            return true;
        } catch (IOException e) {
            return false; // The screenshot writer may still be encoding the frame.
        }
    }

    private static void walkStraightTunnel(Playtest t) {
        BlockPos entry = new BlockPos(14, 190, 15);
        t.server("Open a level approach to a stone wall across the chunk boundary", p -> {
            for (BlockPos pos : BlockPos.betweenClosed(entry.north(4), entry.north().above(2))) {
                p.level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            p.teleportTo(p.level(), entry.getX() + 0.5, entry.getY(), entry.getZ() - 2.5, Set.of(), 0f, 0f, true);
        });
        t.client("Select Walkable Tunnel", mc -> KeyMapping.click(ClientSetup.CYCLE_MINING_MODE_KEY.get().getKey()));
        t.awaitClient("Aim at the bottom block with Walkable Tunnel active", mc -> {
            mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(entry).add(0, 0, -0.5));
            return mc.player.getMainHandItem().get(FloraFaunaRegistry.MULTI_BLOCK_MINING).shape() == MiningShape.WALKABLE_TUNNEL
                    && mc.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(entry) && hit.getDirection() == Direction.NORTH;
        });
        t.client("Start the walkable cut", mc -> Playtest.require(mc.gameMode.startDestroyBlock(entry, Direction.NORTH), "Client refused the cut"));
        t.awaitServer("Verify five columns of three-block headroom and the original floor", p -> {
            for (int i = 0; i < 5; i++) {
                BlockPos foot = entry.south(i);
                if (!p.level().isEmptyBlock(foot) || !p.level().isEmptyBlock(foot.above()) || !p.level().isEmptyBlock(foot.above(2))) return false;
                Playtest.require(p.level().getBlockState(foot.below()).is(Blocks.STONE)
                        && p.level().getBlockState(foot.above(3)).is(Blocks.STONE), "Walkable tunnel damaged its floor or ceiling");
            }
            return true;
        });
        t.client("Walk into the tunnel", mc -> {
            mc.player.setXRot(0);
            mc.player.setYRot(0);
            mc.options.keyUp.setDown(true);
        });
        t.awaitClient("Walk to the far end on the original floor", mc -> {
            if (mc.player.getZ() < entry.getZ() + 4) return false;
            mc.options.keyUp.setDown(false);
            return mc.player.onGround() && Math.abs(mc.player.getY() - entry.getY()) < 0.01;
        });
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
