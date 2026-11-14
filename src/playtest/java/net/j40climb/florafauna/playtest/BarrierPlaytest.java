package net.j40climb.florafauna.playtest;

import net.j40climb.florafauna.common.block.mobbarrier.MobBarrierBlockEntity;
import net.j40climb.florafauna.common.block.mobbarrier.data.MobBarrierConfig;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.CameraType;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
//? if >=26.3 {
/*import net.minecraft.world.item.DyeColor;
*///?}
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

final class BarrierPlaytest {
    private static final BlockPos BARRIER = new BlockPos(0, 202, 3);
    //? if <26.3 {
    private static final Block SUPPORT = Blocks.GRAY_CONCRETE;
    private static final Block BACKDROP = Blocks.BLACK_CONCRETE;
    //?} else {
    /*private static final Block SUPPORT = Blocks.CONCRETE.pick(DyeColor.GRAY);
    private static final Block BACKDROP = Blocks.CONCRETE.pick(DyeColor.BLACK);
    *///?}
    private static final MobBarrierConfig CONFIG = new MobBarrierConfig(List.of("minecraft:zombie"), List.of("#minecraft:undead"), true);

    static void steps(Playtest t, int phase) {
        Playtest.require(phase == 1, "Barrier uses one client launch");
        t.server("Provide two configured barriers and a neutral backdrop for the outline", p -> {
            var level = p.level().getServer().overworld();
            for (int x = -4; x <= 4; x++) {
                for (int z = -1; z <= 5; z++) {
                    level.setBlockAndUpdate(new BlockPos(x, 200, z), SUPPORT.defaultBlockState());
                    for (int y = 201; y <= 206; y++) {
                        level.setBlockAndUpdate(new BlockPos(x, y, z), (z == 5 ? BACKDROP : Blocks.AIR).defaultBlockState());
                    }
                }
            }
            level.setBlockAndUpdate(BARRIER.below(), SUPPORT.defaultBlockState());
            p.teleportTo(level, 0.5, 201, 0.5, Set.of(), 0, 10, true);
            p.setGameMode(GameType.SURVIVAL);
            p.getInventory().clearContent();
            var stack = new ItemStack(FloraFaunaRegistry.MOB_BARRIER.asItem(), 2);
            stack.set(FloraFaunaRegistry.MOB_BARRIER_CONFIG, CONFIG);
            p.getInventory().setItem(0, stack);
            p.getInventory().setSelectedSlot(0);
            p.inventoryMenu.broadcastChanges();
        });
        t.awaitClient("Receive both configured barrier items", mc -> mc.player.getInventory().getItem(0).getCount() == 2
                && CONFIG.equals(mc.player.getInventory().getItem(0).get(FloraFaunaRegistry.MOB_BARRIER_CONFIG))
                && mc.level.getBlockState(BARRIER.below()).is(SUPPORT));
        t.client("Aim at the top face of the support", mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.player.getInventory().setSelectedSlot(0);
            mc.player.lookAt(Anchor.EYES, Vec3.atBottomCenterOf(BARRIER));
        });
        t.awaitClient("Raycast the support under the new barrier", mc -> mc.hitResult instanceof BlockHitResult hit
                && hit.getBlockPos().equals(BARRIER.below()) && hit.getDirection() == Direction.UP);
        t.client("Place one barrier with normal right-click", mc -> Playtest.require(
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, (BlockHitResult) mc.hitResult).consumesAction(),
                "Placement was rejected"));
        t.awaitServer("Verify placement transferred the item configuration and consumed one item", p ->
                p.level().getBlockEntity(BARRIER) instanceof MobBarrierBlockEntity barrier && CONFIG.equals(barrier.getConfig())
                        && p.getMainHandItem().getCount() == 1);
        t.awaitClient("Verify the placed block's configuration synchronized", mc ->
                mc.level.getBlockEntity(BARRIER) instanceof MobBarrierBlockEntity barrier && CONFIG.equals(barrier.getConfig()));
        t.client("Center the stationary camera on the nearby barrier", mc -> mc.player.lookAt(Anchor.EYES, Vec3.atCenterOf(BARRIER)));
        t.screenshot("barrier-held");
        // Rebuilding the platform can drop items from an earlier scenario into the hotbar.
        t.server("Prepare an empty comparison slot", p -> {
            p.getInventory().setItem(1, ItemStack.EMPTY);
            p.inventoryMenu.broadcastChanges();
        });
        t.awaitClient("Receive the empty comparison slot", mc -> mc.player.getInventory().getItem(1).isEmpty());
        t.client("Switch to the empty hotbar slot", mc -> {
            mc.player.getInventory().setSelectedSlot(1);
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(1));
        });
        t.awaitServer("Confirm the bare hand is selected", p -> p.getInventory().getSelectedSlot() == 1 && p.getMainHandItem().isEmpty());
        t.screenshot("barrier-hidden");
        t.awaitClient("Verify the orange barrier outline was visible only with the item held", mc ->
                compareFrames(mc.gameDirectory.toPath().resolve("screenshots")));
    }

    private static boolean compareFrames(Path screenshots) {
        BufferedImage held;
        BufferedImage hidden;
        try {
            held = ImageIO.read(screenshots.resolve("barrier-held.png").toFile());
            hidden = ImageIO.read(screenshots.resolve("barrier-hidden.png").toFile());
        } catch (IOException e) {
            return false; // The screenshot writer can still be encoding the file.
        }
        if (held == null || hidden == null) return false;
        Playtest.require(held.getWidth() == hidden.getWidth() && held.getHeight() == hidden.getHeight(), "Viewport changed between screenshots");
        int vanished = 0;
        int remaining = 0;
        // Only inspect the barrier and neutral backdrop, away from the held item and hotbar.
        for (int y = held.getHeight() / 4; y < held.getHeight() * 7 / 10; y++) {
            for (int x = held.getWidth() * 3 / 10; x < held.getWidth() * 7 / 10; x++) {
                if (orange(hidden.getRGB(x, y))) remaining++;
                else if (orange(held.getRGB(x, y))) vanished++;
            }
        }
        Playtest.require(vanished >= 30 && remaining < vanished / 8,
                "Expected an orange outline to disappear; vanished pixels=" + vanished + ", still orange=" + remaining);
        return true;
    }

    private static boolean orange(int rgb) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        return r > g + 10 && g > b + 10 && r > b + 45;
    }
}
