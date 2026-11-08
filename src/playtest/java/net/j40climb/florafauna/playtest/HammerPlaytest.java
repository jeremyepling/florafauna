package net.j40climb.florafauna.playtest;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna.common.item.abilities.data.MiningSpeed;
import net.j40climb.florafauna.common.item.abilities.menu.ToolConfigScreen;
import net.j40climb.florafauna.setup.ClientSetup;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;

import java.util.Set;

@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
public final class HammerPlaytest {
    private static final BlockPos WALL = new BlockPos(0, 202, 3);
    private static final BlockPos FLOOR = new BlockPos(0, 200, 0);
    private static BlockPos lastCustomOutline;

    // LevelRenderer resets this state at the end of each frame, before the playtest's next client tick.
    @SubscribeEvent
    public static void captureOutline(ExtractLevelRenderStateEvent event) {
        var outline = event.getRenderState().blockOutlineRenderState;
        lastCustomOutline = outline != null && !outline.customRenderers().isEmpty() ? outline.pos() : null;
    }

    static void steps(Playtest t, int phase) {
        if (phase != 1) {
            throw new IllegalArgumentException("The hammer playtest uses one launch");
        }
        t.server("Give the survival player a hammer and build the wall and floor", HammerPlaytest::prepare);
        t.awaitClient("Receive the hammer in the local player's hotbar",
                mc -> mc.player.getInventory().getItem(0).is(FloraFaunaRegistry.HAMMER.get()));
        t.client("Select the hammer and press its configured menu key", mc -> {
            mc.player.getInventory().setSelectedSlot(0);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.hideGui = false;
            KeyMapping.click(ClientSetup.HAMMER_CONFIG_KEY.get().getKey());
        });
        t.awaitClient("Open the tool configuration screen", mc -> mc.screen instanceof ToolConfigScreen);
        t.client("Click the speed control to switch from Efficiency to Instabreak", mc -> {
            Button button = speedButton(mc, "efficiency");
            var click = new MouseButtonEvent(button.getX() + button.getWidth() / 2.0,
                    button.getY() + button.getHeight() / 2.0, new MouseButtonInfo(0, 0));
            Playtest.require(mc.screen.mouseClicked(click, false), "Speed button click was ignored");
            mc.screen.mouseReleased(click);
            mc.screen.onClose();
        });
        t.awaitServer("Receive the speed update on the server", p ->
                p.getMainHandItem().get(FloraFaunaRegistry.TOOL_CONFIG).miningSpeed() == MiningSpeed.INSTABREAK);
        t.awaitClient("Synchronize the server's tool configuration back to the client", mc ->
                mc.player.getMainHandItem().get(FloraFaunaRegistry.TOOL_CONFIG).miningSpeed() == MiningSpeed.INSTABREAK);
        t.client("Press the menu key again", mc -> KeyMapping.click(ClientSetup.HAMMER_CONFIG_KEY.get().getKey()));
        t.awaitClient("Reopen the tool configuration screen", mc -> mc.screen instanceof ToolConfigScreen);
        t.client("Verify the persisted speed label and press Cycle Mining Mode", mc -> {
            speedButton(mc, "instabreak");
            mc.screen.onClose();
            KeyMapping.click(ClientSetup.CYCLE_MINING_MODE_KEY.get().getKey());
        });
        t.awaitServer("Select flat 3×3 mining on the server", p ->
                p.getMainHandItem().get(FloraFaunaRegistry.MULTI_BLOCK_MINING).shape() == MiningShape.FLAT_3X3);

        minePlane(t, WALL, Direction.NORTH, "wall");
        minePlane(t, FLOOR, Direction.UP, "floor");

        t.server("Set an oak log in front of the player", p -> {
            p.level().setBlockAndUpdate(FLOOR, Blocks.STONE.defaultBlockState());
            p.level().setBlockAndUpdate(WALL, Blocks.OAK_LOG.defaultBlockState());
            p.teleportTo(p.level(), 0.5, 201, 0.5, Set.of(), 0f, 0f, true);
        });
        t.awaitClient("Aim at the oak log", mc -> aimedAt(mc, WALL, Direction.NORTH)
                && mc.level.getBlockState(WALL).is(Blocks.OAK_LOG));
        t.client("Use the held hammer on the log", mc ->
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, (BlockHitResult) mc.hitResult));
        t.awaitServer("Verify oak was stripped and the hammer stayed in hand", p ->
                p.level().getBlockState(WALL).is(Blocks.STRIPPED_OAK_LOG)
                        && p.getMainHandItem().is(FloraFaunaRegistry.HAMMER.get()));
    }

    private static void prepare(ServerPlayer player) {
        ServerLevel level = player.level().getServer().overworld();
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int depth = 0; depth <= 1; depth++) {
                    level.setBlockAndUpdate(WALL.offset(x, y, depth), Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(FLOOR.offset(x, -depth, y), Blocks.STONE.defaultBlockState());
                }
                for (int h = 1; h <= 4; h++) {
                    level.setBlockAndUpdate(FLOOR.offset(x, h, y), Blocks.AIR.defaultBlockState());
                }
            }
        }
        player.setGameMode(GameType.SURVIVAL);
        player.removeAllEffects();
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(FloraFaunaRegistry.HAMMER.get()));
        player.getInventory().setSelectedSlot(0);
        player.teleportTo(level, 0.5, 201, 0.5, Set.of(), 0f, 0f, true);
        player.inventoryMenu.broadcastChanges();
    }

    private static Button speedButton(Minecraft mc, String speed) {
        String expected = Component.translatable("gui.florafauna.tool_config.speed." + speed).getString();
        return mc.screen.children().stream()
                .filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> button.getMessage().getString().equals(expected))
                .findFirst().orElseThrow(() -> new AssertionError("Missing speed button: " + expected));
    }

    private static boolean aimedAt(Minecraft mc, BlockPos center, Direction face) {
        return mc.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(center)
                && hit.getDirection() == face && mc.player.onGround();
    }

    private static void minePlane(Playtest t, BlockPos center, Direction face, String label) {
        t.server("Aim at the " + label, p ->
                p.teleportTo(p.level(), 0.5, 201, 0.5, Set.of(), 0f, face == Direction.UP ? 90f : 0f, true));
        t.awaitClient("Target the " + label + " center on the client", mc -> aimedAt(mc, center, face));
        t.awaitClient("Extract the custom mining outline for the " + label,
                mc -> aimedAt(mc, center, face) && center.equals(lastCustomOutline));
        t.screenshot("hammer-" + label);
        t.client("Attack the center of the " + label, mc ->
                Playtest.require(mc.gameMode.startDestroyBlock(center, face), "Client refused to mine " + center));
        t.awaitServer("Mine the " + label + " center", p -> p.level().getBlockState(center).isAir());
        t.server("Verify all nine " + label + " blocks, the stone perimeter and the backing layer", p -> {
            for (int a = -2; a <= 2; a++) {
                for (int b = -2; b <= 2; b++) {
                    BlockPos pos = face == Direction.UP ? center.offset(a, 0, b) : center.offset(a, b, 0);
                    var state = p.level().getBlockState(pos);
                    boolean inside = Math.abs(a) <= 1 && Math.abs(b) <= 1;
                    Playtest.require(inside ? state.isAir() : state.is(Blocks.STONE),
                            "Wrong block at " + pos + ": expected " + (inside ? "air" : "stone") + ", got " + state);
                    BlockPos backing = pos.relative(face.getOpposite());
                    Playtest.require(p.level().getBlockState(backing).is(Blocks.STONE),
                            "Mining broke the backing layer at " + backing);
                }
            }
        });
    }
}
