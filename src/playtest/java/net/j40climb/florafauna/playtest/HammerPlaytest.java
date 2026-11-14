package net.j40climb.florafauna.playtest;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.wood.WoodType;
import net.j40climb.florafauna.common.item.abilities.data.MultiToolAbilityData;
import net.j40climb.florafauna.common.item.abilities.data.RightClickAction;
import net.j40climb.florafauna.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna.common.item.abilities.data.MiningSpeed;
import net.j40climb.florafauna.common.item.abilities.menu.ToolConfigScreen;
import net.j40climb.florafauna.common.item.abilities.networking.UpdateToolConfigPayload;
import net.j40climb.florafauna.setup.ClientSetup;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.j40climb.florafauna.dev.DevPlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;

import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
public final class HammerPlaytest {
    private static final BlockPos WALL = new BlockPos(0, 202, 3);
    private static final BlockPos FLOOR = new BlockPos(0, 200, 0);
    private static BlockPos lastCustomOutline;
    private static boolean observingCracks;
    private static final Set<BlockPos> cracks = new HashSet<>();

    // LevelRenderer resets this state at the end of each frame, before the playtest's next client tick.
    @SubscribeEvent
    public static void captureOutline(ExtractLevelRenderStateEvent event) {
        if (observingCracks) {
            cracks.clear();
            event.getRenderState().blockBreakingRenderStates.forEach(state -> cracks.add(state.blockPos()));
        }
        var outline = event.getRenderState().blockOutlineRenderState;
        lastCustomOutline = outline != null && !outline.customRenderers().isEmpty() ? outline.pos() : null;
    }

    static void steps(Playtest t, int phase) {
        if (phase == 2) {
            HammerQuarryPlaytest.resume(t);
            ChargedHammerPlaytest.steps(t);
            return;
        }
        if (phase != 1) {
            throw new IllegalArgumentException("Unknown Hammer phase " + phase);
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
            clickConfigButton(mc, "speed.efficiency");
            mc.screen.onClose();
        });
        t.awaitServer("Receive the speed update on the server", p ->
                p.getMainHandItem().get(FloraFaunaRegistry.TOOL_CONFIG).miningSpeed() == MiningSpeed.INSTABREAK);
        t.awaitClient("Synchronize the server's tool configuration back to the client", mc ->
                mc.player.getMainHandItem().get(FloraFaunaRegistry.TOOL_CONFIG).miningSpeed() == MiningSpeed.INSTABREAK);
        t.client("Press the menu key again", mc -> KeyMapping.click(ClientSetup.HAMMER_CONFIG_KEY.get().getKey()));
        t.awaitClient("Reopen the tool configuration screen", mc -> mc.screen instanceof ToolConfigScreen);
        t.client("Verify the persisted speed label and press Cycle Mining Mode", mc -> {
            configButton(mc, "speed.instabreak");
            mc.screen.onClose();
            KeyMapping.click(ClientSetup.CYCLE_MINING_MODE_KEY.get().getKey());
        });
        t.awaitServer("Select flat 3×3 mining on the server", p ->
                p.getMainHandItem().get(FloraFaunaRegistry.MULTI_BLOCK_MINING).shape() == MiningShape.FLAT_3X3);

        setVeinPreference(t, true);
        setVeinPreference(t, false);
        interruptSwing(t);
        HammerPrecisionPlaytest.steps(t, WALL);
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
        var driftwood = WoodType.DRIFTWOOD.getBlockSet();
        var logX = driftwood.log().get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        var strippedLogX = driftwood.strippedLog().get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        var woodZ = driftwood.wood().get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        var strippedWoodZ = driftwood.strippedWood().get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        t.server("Configure mining-mode cycling as the fallback right-click action", p -> {
            p.getMainHandItem().set(FloraFaunaRegistry.RIGHT_CLICK_ACTION, new RightClickAction(FloraFaunaRegistry.MULTI_BLOCK_MINING.getId()));
            p.inventoryMenu.broadcastChanges();
        });
        // With stripping disabled, cycling from 3x3 to 5x5 acknowledges that the server handled the click.
        checkTransform(t, "Disabled stripping leaves the driftwood intact and runs the fallback", logX,
                MultiToolAbilityData.DEFAULT.withStrip(false), logX);
        checkTransform(t, "Strip driftwood log on its X axis", logX, MultiToolAbilityData.DEFAULT, strippedLogX);
        checkTransform(t, "Strip driftwood wood on its Z axis", woodZ, MultiToolAbilityData.DEFAULT, strippedWoodZ);
        checkTransform(t, "Flatten dirt before tilling when both modes are enabled", Blocks.DIRT.defaultBlockState(),
                MultiToolAbilityData.DEFAULT, Blocks.DIRT_PATH.defaultBlockState());
        checkTransform(t, "Till dirt when flattening is disabled", Blocks.DIRT.defaultBlockState(),
                MultiToolAbilityData.DEFAULT.withFlatten(false), Blocks.FARMLAND.defaultBlockState());
        HammerQuarryPlaytest.start(t);
    }

    private static void setVeinPreference(Playtest t, boolean exact) {
        String current = exact ? "vein.same_ore" : "vein.exact";
        String next = exact ? "vein.exact" : "vein.same_ore";
        t.client("Press the menu key to change vein matching", mc -> KeyMapping.click(ClientSetup.HAMMER_CONFIG_KEY.get().getKey()));
        t.awaitClient("Open the vein matching control", mc -> mc.screen instanceof ToolConfigScreen);
        t.client("Click the vein matching control to select " + next, mc -> {
            clickConfigButton(mc, current);
            configButton(mc, next);
            mc.screen.onClose();
        });
        t.awaitServer("Save vein matching " + next + " on the held hammer", p ->
                p.getMainHandItem().get(FloraFaunaRegistry.TOOL_CONFIG).exactVein() == exact);
        t.awaitClient("Synchronize vein matching " + next + " to the client", mc ->
                mc.player.getMainHandItem().get(FloraFaunaRegistry.TOOL_CONFIG).exactVein() == exact);
    }

    private static void interruptSwing(Playtest t) {
        t.server("Prepare a Standard-speed swing on the 3×3 stone wall", p -> setSpeed(p, MiningSpeed.STANDARD));
        t.awaitClient("Aim at the wall with Standard speed selected", mc -> aimedAt(mc, WALL, Direction.NORTH)
                && mc.player.getMainHandItem().get(FloraFaunaRegistry.TOOL_CONFIG).miningSpeed() == MiningSpeed.STANDARD);
        t.client("Hold attack to begin mining", mc -> {
            observingCracks = true;
            KeyMapping.click(mc.options.keyAttack.getKey());
            mc.options.keyAttack.setDown(true);
        });
        Set<BlockPos> expected = new HashSet<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) expected.add(WALL.offset(x, y, 0));
        }
        t.awaitClient("Render cracks at all nine target positions, then hold precision", mc -> {
            if (!cracks.equals(expected)) return false;
            ClientSetup.PRECISION_MINING_KEY.get().setDown(true);
            return true;
        });
        t.awaitClient("Hold precision mid-swing and show the center crack alone", mc -> {
            if (!cracks.equals(Set.of(WALL))) return false;
            mc.options.keyAttack.setDown(false);
            ClientSetup.PRECISION_MINING_KEY.get().setDown(false);
            return true;
        });
        t.awaitClient("Clear all cracks when the swing is interrupted", mc -> !mc.gameMode.isDestroying() && cracks.isEmpty());
        t.client("Finish observing the interrupted swing", mc -> observingCracks = false);
        t.server("Verify every block survived the interrupted swing", p -> {
            for (BlockPos pos : expected) {
                Playtest.require(p.level().getBlockState(pos).is(Blocks.STONE), "Interrupted swing broke " + pos);
            }
            setSpeed(p, MiningSpeed.INSTABREAK);
        });
        t.awaitClient("Receive Instabreak before the completed wall strike", mc ->
                mc.player.getMainHandItem().get(FloraFaunaRegistry.TOOL_CONFIG).miningSpeed() == MiningSpeed.INSTABREAK);
    }

    private static void setSpeed(ServerPlayer player, MiningSpeed speed) {
        var config = player.getMainHandItem().get(FloraFaunaRegistry.TOOL_CONFIG);
        UpdateToolConfigPayload.applyToHeldItem(player, config.withMiningSpeed(speed));
        player.inventoryMenu.broadcastChanges();
    }

    private static void checkTransform(Playtest t, String label, BlockState before, MultiToolAbilityData modes, BlockState after) {
        t.server("Set up: " + label, p -> {
            p.level().setBlockAndUpdate(WALL.above(), Blocks.AIR.defaultBlockState());
            p.level().setBlockAndUpdate(WALL, before);
            p.getMainHandItem().set(FloraFaunaRegistry.MULTI_TOOL_ABILITY, modes);
            p.inventoryMenu.broadcastChanges();
        });
        t.awaitClient("Receive the target and hammer modes: " + label, mc -> aimedAt(mc, WALL, Direction.NORTH)
                && mc.level.getBlockState(WALL).equals(before)
                && modes.equals(mc.player.getMainHandItem().get(FloraFaunaRegistry.MULTI_TOOL_ABILITY)));
        t.client("Use the hammer: " + label, mc ->
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, (BlockHitResult) mc.hitResult));
        t.awaitServer(label, p -> p.level().getBlockState(WALL).equals(after) && p.getMainHandItem().is(FloraFaunaRegistry.HAMMER.get())
                && p.getMainHandItem().get(FloraFaunaRegistry.MULTI_BLOCK_MINING).shape() == MiningShape.FLAT_5X5);
        t.awaitClient("See the resulting block and mining mode: " + label, mc -> mc.level.getBlockState(WALL).equals(after)
                && mc.player.getMainHandItem().get(FloraFaunaRegistry.MULTI_BLOCK_MINING).shape() == MiningShape.FLAT_5X5);
    }

    private static void prepare(ServerPlayer player) {
        ServerLevel level = player.level().getServer().overworld();
        DevPlot.HAMMER.place(level, new BlockPos(-2, 199, -5));
        player.setGameMode(GameType.SURVIVAL);
        player.removeAllEffects();
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(FloraFaunaRegistry.HAMMER.get()));
        player.getInventory().setSelectedSlot(0);
        player.teleportTo(level, 0.5, 201, 0.5, Set.of(), 0f, 0f, true);
        player.inventoryMenu.broadcastChanges();
    }

    private static void clickConfigButton(Minecraft mc, String setting) {
        Button button = configButton(mc, setting);
        var click = new MouseButtonEvent(button.getX() + button.getWidth() / 2.0,
                button.getY() + button.getHeight() / 2.0, new MouseButtonInfo(0, 0));
        Playtest.require(mc.screen.mouseClicked(click, false), "Button click was ignored: " + setting);
        mc.screen.mouseReleased(click);
    }

    private static Button configButton(Minecraft mc, String setting) {
        String expected = Component.translatable("gui.florafauna.tool_config." + setting).getString();
        return mc.screen.children().stream()
                .filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> button.getMessage().getString().equals(expected))
                .findFirst().orElseThrow(() -> new AssertionError("Missing config button: " + expected));
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
