package net.j40climb.florafauna.playtest;

import com.mojang.blaze3d.platform.InputConstants;
import net.j40climb.florafauna.common.block.mobbarrier.MobBarrierBlockEntity;
import net.j40climb.florafauna.common.block.mobbarrier.data.MobBarrierConfig;
import net.j40climb.florafauna.common.block.mobbarrier.menu.MobBarrierConfigScreen;
import net.j40climb.florafauna.common.entity.gecko.GeckoEntity;
import net.j40climb.florafauna.common.entity.gecko.GeckoModel;
import net.j40climb.florafauna.common.entity.gecko.GeckoRenderer;
import net.j40climb.florafauna.common.entity.gecko.GeckoVariant;
import net.j40climb.florafauna.common.entity.lizard.LizardEntity;
import net.j40climb.florafauna.common.entity.lizard.LizardModel;
import net.j40climb.florafauna.common.entity.lizard.LizardRenderer;
import net.j40climb.florafauna.setup.ClientSetup;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
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
    private static final BlockPos EDITED_BARRIER = BARRIER.east(2);
    //? if <26.3 {
    private static final Block SUPPORT = Blocks.GRAY_CONCRETE;
    private static final Block BACKDROP = Blocks.BLACK_CONCRETE;
    //?} else {
    /*private static final Block SUPPORT = Blocks.CONCRETE.pick(DyeColor.GRAY);
    private static final Block BACKDROP = Blocks.CONCRETE.pick(DyeColor.BLACK);
    *///?}
    private static final MobBarrierConfig CONFIG = new MobBarrierConfig(List.of("minecraft:zombie"), List.of("#minecraft:undead"), true);
    private static final MobBarrierConfig EDITED_CONFIG = CONFIG.withBlockVision(false);

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
            level.setBlockAndUpdate(EDITED_BARRIER.below(), SUPPORT.defaultBlockState());
            p.teleportTo(level, 0.5, 201, 0.5, Set.of(), 0, 10, true);
            p.setGameMode(GameType.SURVIVAL);
            p.getInventory().clearContent();
            var stack = new ItemStack(FloraFaunaRegistry.MOB_BARRIER.asItem(), 2);
            stack.set(FloraFaunaRegistry.MOB_BARRIER_CONFIG.get(), CONFIG);
            p.getInventory().setItem(0, stack);
            p.getInventory().setSelectedSlot(0);
            p.inventoryMenu.broadcastChanges();
        });
        t.awaitClient("Receive both configured barrier items", mc -> mc.player.getInventory().getItem(0).getCount() == 2
                && CONFIG.equals(mc.player.getInventory().getItem(0).get(FloraFaunaRegistry.MOB_BARRIER_CONFIG.get()))
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
        editPresetAndPlace(t);
        renderCreatures(t);
    }

    private static void editPresetAndPlace(Playtest t) {
        t.client("Hold the remaining preset and look at the original barrier", mc -> {
            mc.player.getInventory().setSelectedSlot(0);
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(0));
            mc.player.lookAt(Anchor.EYES, Vec3.atCenterOf(BARRIER));
        });
        t.awaitServer("Confirm the original preset is held before pressing B", p -> p.getInventory().getSelectedSlot() == 0
                && p.getMainHandItem().getCount() == 1 && CONFIG.equals(p.getMainHandItem().get(FloraFaunaRegistry.MOB_BARRIER_CONFIG.get())));
        // Queue the bound key and let the regular shared KeyInputEvents callback consume it.
        t.client("Tap the mapped B key to open the barrier preset editor", mc ->
                KeyMapping.click(InputConstants.getKey(ClientSetup.MOB_BARRIER_CONFIG_KEY.get().saveString())));
        t.awaitClient("See the barrier editor with Block vision ON", mc -> mc.screen instanceof MobBarrierConfigScreen
                && enabledButton(mc.screen, "gui.florafauna.mob_barrier.block_vision.on") != null);
        t.screenshot("barrier-editor-vision-on");
        t.client("Click the visible vision toggle to commit the changed preset", mc ->
                clickButton(mc, "gui.florafauna.mob_barrier.block_vision.on"));
        t.awaitServer("Verify the editor committed only the held preset to the server", p ->
                EDITED_CONFIG.equals(p.getMainHandItem().get(FloraFaunaRegistry.MOB_BARRIER_CONFIG.get()))
                        && p.level().getBlockEntity(BARRIER) instanceof MobBarrierBlockEntity original
                        && CONFIG.equals(original.getConfig()));
        t.awaitClient("Receive the changed item component and Block vision OFF label", mc ->
                EDITED_CONFIG.equals(mc.player.getMainHandItem().get(FloraFaunaRegistry.MOB_BARRIER_CONFIG.get()))
                        && mc.screen instanceof MobBarrierConfigScreen
                        && enabledButton(mc.screen, "gui.florafauna.mob_barrier.block_vision.off") != null);
        t.screenshot("barrier-editor-vision-off");
        t.client("Close the editor with Escape", mc -> Playtest.require(
                mc.screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0)), "Barrier editor ignored Escape"));
        t.awaitClient("Return to the world with the edited barrier item", mc -> mc.screen == null);
        t.client("Aim at the support for the edited barrier", mc ->
                mc.player.lookAt(Anchor.EYES, Vec3.atBottomCenterOf(EDITED_BARRIER)));
        t.awaitClient("Raycast the edited barrier's support", mc -> mc.hitResult instanceof BlockHitResult hit
                && hit.getBlockPos().equals(EDITED_BARRIER.below()) && hit.getDirection() == Direction.UP);
        t.client("Place the barrier carrying the edited preset", mc -> Playtest.require(
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, (BlockHitResult) mc.hitResult).consumesAction(),
                "Edited barrier placement was rejected"));
        t.awaitServer("Verify the new barrier inherited the editor change and consumed the last item", p ->
                p.getMainHandItem().isEmpty()
                        && p.level().getBlockEntity(EDITED_BARRIER) instanceof MobBarrierBlockEntity edited
                        && EDITED_CONFIG.equals(edited.getConfig())
                        && p.level().getBlockEntity(BARRIER) instanceof MobBarrierBlockEntity original
                        && CONFIG.equals(original.getConfig()));
        t.awaitClient("Receive both placed configurations after the edit", mc ->
                mc.level.getBlockEntity(EDITED_BARRIER) instanceof MobBarrierBlockEntity edited
                        && EDITED_CONFIG.equals(edited.getConfig())
                        && mc.level.getBlockEntity(BARRIER) instanceof MobBarrierBlockEntity original
                        && CONFIG.equals(original.getConfig()) && mc.player.getMainHandItem().isEmpty());
    }

    private static void renderCreatures(Playtest t) {
        int[] actors = new int[2];
        t.server("Place an adult gecko and lizard on lit display plinths", p -> {
            var level = p.level();
            level.getServer().clockManager().moveToTimeMarker(level.dimensionType().defaultClock().orElseThrow(), ClockTimeMarkers.NOON);
            level.getServer().setWeatherParameters(6000, 0, false, false);
            for (int x : new int[]{-2, -1, 1, 2}) {
                for (int z = 3; z <= 4; z++) level.setBlockAndUpdate(new BlockPos(x, 201, z), Blocks.SEA_LANTERN.defaultBlockState());
            }
            var gecko = FloraFaunaRegistry.GECKO.get().create(level, EntitySpawnReason.COMMAND);
            var lizard = FloraFaunaRegistry.LIZARD.get().create(level, EntitySpawnReason.COMMAND);
            Playtest.require(gecko != null && lizard != null, "Creature registration could not create both actors");
            gecko.setAge(0);
            lizard.setAge(0);
            gecko.setNoAi(true);
            lizard.setNoAi(true);
            gecko.setNoGravity(true);
            lizard.setNoGravity(true);
            gecko.setPersistenceRequired();
            lizard.setPersistenceRequired();
            gecko.snapTo(-1.3, 202, 3.6, 90, 0);
            lizard.snapTo(1.9, 202, 3.6, 90, 0);
            Playtest.require(level.addFreshEntity(gecko) && level.addFreshEntity(lizard), "Could not spawn the display creatures");
            actors[0] = gecko.getId();
            actors[1] = lizard.getId();
            p.teleportTo(level, 0.3, 201, 0.5, Set.of(), 0, 0, true);
        });
        t.awaitClient("Receive visible adult creatures with their expected models and textures", mc -> {
            if (!(mc.level.getEntity(actors[0]) instanceof GeckoEntity gecko)
                    || !(mc.level.getEntity(actors[1]) instanceof LizardEntity lizard)
                    || gecko.tickCount < 5 || lizard.tickCount < 5 || Math.abs(mc.player.getZ() - 0.5) > 0.1) return false;
            Playtest.require(!gecko.isBaby() && !lizard.isBaby() && gecko.getVariant() == GeckoVariant.BLUE,
                    "Display actors changed their expected adult appearance");
            var dispatcher = mc.getEntityRenderDispatcher();
            Playtest.require(dispatcher.getRenderer(gecko) instanceof GeckoRenderer, "Gecko renderer was not registered");
            Playtest.require(dispatcher.getRenderer(lizard) instanceof LizardRenderer, "Lizard renderer was not registered");
            var geckoRenderer = (GeckoRenderer) dispatcher.getRenderer(gecko);
            var lizardRenderer = (LizardRenderer) dispatcher.getRenderer(lizard);
            var geckoTexture = Identifier.fromNamespaceAndPath("florafauna", "textures/entity/gecko/gecko.png");
            var lizardTexture = Identifier.fromNamespaceAndPath("florafauna", "textures/entity/lizard/lizard.png");
            Playtest.require(geckoRenderer.getModel().getClass() == GeckoModel.class
                            && lizardRenderer.getModel().getClass() == LizardModel.class,
                    "Creature renderer used an unexpected model");
            Playtest.require(geckoTexture.equals(geckoRenderer.getTextureLocation(geckoRenderer.createRenderState(gecko, 1)))
                            && lizardTexture.equals(lizardRenderer.getTextureLocation(lizardRenderer.createRenderState(lizard, 1)))
                            && mc.getResourceManager().getResource(geckoTexture).isPresent()
                            && mc.getResourceManager().getResource(lizardTexture).isPresent(),
                    "Creature texture selection or resources were missing");
            return true;
        });
        t.client("Frame both lit creatures from the player's first-person view", mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.player.lookAt(Anchor.EYES, new Vec3(0.3, 202.5, 3.6));
        });
        t.screenshot("barrier-gecko-and-lizard-player-view");
    }

    private static Button enabledButton(Screen screen, String key) {
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> button.active && button.visible && button.getMessage().equals(Component.translatable(key)))
                .findFirst().orElse(null);
    }

    private static void clickButton(Minecraft mc, String key) {
        var button = enabledButton(mc.screen, key);
        Playtest.require(button != null, "Missing visible barrier editor button " + key);
        var click = new MouseButtonEvent(button.getX() + button.getWidth() / 2.0,
                button.getY() + button.getHeight() / 2.0, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
        Playtest.require(mc.screen.mouseClicked(click, false), "Barrier editor ignored click on " + key);
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
