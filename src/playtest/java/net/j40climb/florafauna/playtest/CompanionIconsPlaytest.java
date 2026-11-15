package net.j40climb.florafauna.playtest;

import net.minecraft.world.entity.EntityTypes;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import javax.imageio.ImageIO;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.entity.frenchie.FrenchieEntity;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
final class CompanionIconsPlaytest {
    private static final BlockPos START = new BlockPos(0, 201, 0);
    private static String flashScreenshot;
    private static Predicate<Minecraft> flashVisible;

    static void steps(Playtest t) {
        int[] dogId = new int[1];
        int[] creeperId = new int[1];
        t.server("Give the owner a compass and record a death at the end of the path", p -> {
            var level = p.level();
            level.getServer().setDifficulty(Difficulty.NORMAL, true);
            level.getServer().clockManager().moveToTimeMarker(level.dimensionType().defaultClock().orElseThrow(), ClockTimeMarkers.NOON);
            for (var floor : BlockPos.betweenClosed(START.offset(-4, -1, -4), START.offset(30, -1, 4))) {
                level.setBlockAndUpdate(floor, Blocks.SMOOTH_STONE.defaultBlockState());
                for (int height = 1; height <= 3; height++) level.setBlockAndUpdate(floor.above(height), Blocks.AIR.defaultBlockState());
            }
            p.teleportTo(level, 0.5, START.getY(), 2.5, Set.of(), 180, 15, true);
            p.setGameMode(GameType.SURVIVAL);
            p.getInventory().clearContent();
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COMPASS));
            p.setLastDeathLocation(Optional.of(GlobalPos.of(level.dimension(), START.east(26))));
            var dog = FloraFaunaRegistry.FRENCHIE.get().spawn(level, START, EntitySpawnReason.COMMAND);
            Playtest.require(dog != null, "Frenchie did not spawn");
            dog.tame(p);
            dog.sitDown();
            dog.setCustomName(Component.literal("Pip"));
            dogId[0] = dog.getId();
        });
        t.client("Frame the dog in first person", mc -> {
            if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        });
        clickDog(t, dogId, false);
        t.awaitServer("Watch Pip lead, then wait for the owner with the compass still in hand", p ->
                p.level().getEntity(dogId[0]) instanceof FrenchieEntity dog && dog.isDeathTracking()
                        && p.getMainHandItem().is(Items.COMPASS) && dog.distanceToSqr(p) > 100 && dog.getNavigation().isDone());
        t.awaitClient("Read the compass before Pip's name while waiting", mc -> {
            if (!(mc.level.getEntity(dogId[0]) instanceof FrenchieEntity dog)) return false;
            mc.player.lookAt(Anchor.EYES, dog.getEyePosition());
            return shows(mc, dog, "compass");
        });
        t.screenshot("compass-named-waiting");
        captureFlash(t, "creeper-named-tracking", mc -> mc.level.getEntity(dogId[0]) instanceof FrenchieEntity dog
                && dog.isDeathTracking() && shows(mc, dog, "creeper"));
        t.server("Expose a visible creeper to the waiting dog", p ->
                creeperId[0] = spawnCreeper(p.level(), p.level().getEntity(dogId[0]).blockPosition().north(3)));
        awaitFlash(t, "creeper-named-tracking");
        t.server("Remove the creeper", p -> p.level().getEntity(creeperId[0]).discard());
        t.awaitClient("Verify the same active trail restored the compass before Pip", mc ->
                mc.level.getEntity(dogId[0]) instanceof FrenchieEntity dog && dog.isDeathTracking() && shows(mc, dog, "compass"));
        t.server("Remove the test name from the waiting dog", p -> {
            var dog = (FrenchieEntity)p.level().getEntity(dogId[0]);
            Playtest.require(Component.literal("Pip").equals(dog.getCustomName()), "Displaying status changed the dog's name");
            dog.setCustomName(null);
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        });
        t.awaitClient("Frame the standalone compass above the unnamed dog", mc -> {
            if (!(mc.level.getEntity(dogId[0]) instanceof FrenchieEntity dog)) return false;
            mc.player.lookAt(Anchor.EYES, dog.getEyePosition());
            return mc.player.getMainHandItem().isEmpty() && !dog.hasCustomName() && shows(mc, dog, "compass");
        });
        t.screenshot("compass-unnamed");
        t.server("Catch up to the companion to cancel the trail", p -> {
            var dog = (FrenchieEntity)p.level().getEntity(dogId[0]);
            p.teleportTo(p.level(), dog.getX() + 1.8, dog.getY(), dog.getZ() + 0.8, Set.of(), 90, 20, true);
        });
        clickDog(t, dogId, false);
        t.awaitClient("Check the sitting unnamed dog has no nameplate", mc ->
                mc.level.getEntity(dogId[0]) instanceof FrenchieEntity dog && dog.isSitting()
                        && mc.getEntityRenderDispatcher().getRenderer(dog).createRenderState(dog, 1).nameTag == null);
        t.server("Give Pip the compass for a second trip", p -> {
            var dog = (FrenchieEntity)p.level().getEntity(dogId[0]);
            dog.setCustomName(Component.literal("Pip"));
            p.setLastDeathLocation(Optional.of(GlobalPos.of(p.level().dimension(), START.west(30))));
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COMPASS));
        });
        t.awaitClient("Wait for the compass to reach the owner's hand", mc -> mc.player.getMainHandItem().is(Items.COMPASS));
        clickDog(t, dogId, false);
        t.awaitClient("Wait for Pip to follow the new trail", mc ->
                mc.level.getEntity(dogId[0]) instanceof FrenchieEntity dog && dog.isDeathTracking());
        t.server("Stand beside Pip to lift the tracking dog", p -> {
            var dog = (FrenchieEntity)p.level().getEntity(dogId[0]);
            p.teleportTo(p.level(), dog.getX() - 1.5, dog.getY(), dog.getZ() + 1, Set.of(), 100, 15, true);
        });
        clickDog(t, dogId, true);
        t.awaitClient("Verify the packed dog kept its tracking icon and name", mc -> {
            var pack = mc.player.getData(FloraFaunaRegistry.FRENCH_FRONTPACK_DATA);
            return pack.hasCarriedFrenchie() && pack.frenchieNBT().contains("FrenchieDeathTrail")
                    && pack.frenchieNBT().contains("CustomName");
        });
        t.client("Frame the carried dog's nameplate", mc -> {
            mc.options.keyShift.setDown(false);
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            mc.player.setXRot(0);
        });
        t.awaitClient("Wait for the carrier to stand", mc -> !mc.player.isCrouching());
        t.screenshot("compass-frontpack");
        t.client("Face the sky above the path for a clear HUD view", mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.player.setXRot(-70);
        });
        captureFlash(t, "creeper-frontpack", mc -> {
            var warning = mc.player.getExistingDataOrNull(FloraFaunaRegistry.FRENCHIE_WARNING);
            return warning != null && warning.visible(mc.level.getGameTime());
        });
        t.server("Bring a second creeper within sight of the carried dog", p -> spawnCreeper(p.level(), p.blockPosition().north(3)));
        awaitFlash(t, "creeper-frontpack");
        t.awaitClient("Check for the green sprite in the first-person hotbar region", CompanionIconsPlaytest::hudPixels);
    }

    static boolean shows(Minecraft mc, FrenchieEntity dog, String sprite) {
        var tag = mc.getEntityRenderDispatcher().getRenderer(dog).createRenderState(dog, 1).nameTag;
        var id = Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "frenchie/" + sprite);
        var atlas = mc.getAtlasManager().getAtlasOrThrow(AtlasIds.GUI);
        if (tag == null || !(tag.getContents() instanceof ObjectContents icon)
                || !icon.contents().equals(new AtlasSprite(AtlasIds.GUI, id)) || atlas.getSprite(id) == atlas.missingSprite()) return false;
        return dog.hasCustomName()
                ? tag.getSiblings().equals(java.util.List.of(Component.literal(" "), dog.getDisplayName()))
                : tag.getSiblings().isEmpty();
    }

    private static void clickDog(Playtest t, int[] id, boolean crouch) {
        t.client("Set the owner's crouch state", mc -> mc.options.keyShift.setDown(crouch));
        t.awaitClient("Aim at the Frenchie within interaction reach", mc -> {
            if (!(mc.level.getEntity(id[0]) instanceof FrenchieEntity dog)) return false;
            mc.player.lookAt(Anchor.EYES, dog.getEyePosition());
            return mc.player.isShiftKeyDown() == crouch && mc.hitResult instanceof EntityHitResult hit && hit.getEntity() == dog;
        });
        t.client("Press use on the dog", mc -> KeyMapping.click(mc.options.keyUse.getKey()));
    }

    private static int spawnCreeper(ServerLevel level, BlockPos pos) {
        var creeper = EntityTypes.CREEPER.spawn(level, pos, EntitySpawnReason.COMMAND);
        Playtest.require(creeper != null, "Creeper did not spawn");
        creeper.setNoAi(true);
        return creeper.getId();
    }

    // Catch a completed frame from a short bark pulse, even if several ticks run before the next frame.
    private static void captureFlash(Playtest t, String name, Predicate<Minecraft> when) {
        t.client("Watch for " + name, mc -> {
            flashScreenshot = name;
            flashVisible = when;
        });
    }

    @SubscribeEvent
    public static void afterFrame(RenderFrameEvent.Post event) {
        if (flashVisible == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (flashVisible.test(mc)) {
            Screenshot.grab(mc.gameDirectory, flashScreenshot + ".png", mc.gameRenderer.mainRenderTarget(), 1, ignored -> {});
            flashVisible = null;
        }
    }

    private static void awaitFlash(Playtest t, String name) {
        t.awaitClient("Save " + name, mc -> Files.isRegularFile(mc.gameDirectory.toPath().resolve("screenshots/" + name + ".png")));
    }

    private static boolean hudPixels(Minecraft mc) {
        BufferedImage image;
        try {
            image = ImageIO.read(mc.gameDirectory.toPath().resolve("screenshots/creeper-frontpack.png").toFile());
        } catch (IOException e) {
            return false;
        }
        if (image == null) return false;
        int width = mc.getWindow().getGuiScaledWidth(), height = mc.getWindow().getGuiScaledHeight();
        int green = 0;
        for (int y = height - 51; y < height - 35; y++) {
            for (int x = width / 2 - 8; x < width / 2 + 8; x++) {
                int rgb = image.getRGB(x * image.getWidth() / width, y * image.getHeight() / height);
                int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
                if (g > 100 && g > r + 35 && g > b + 35) green++;
            }
        }
        Playtest.require(green >= 28, "Expected green creeper pixels over the hotbar, found " + green);
        return true;
    }
}
