package net.j40climb.florafauna.playtest;

import net.j40climb.florafauna.common.entity.frenchie.FrenchieEntity;
import net.j40climb.florafauna.common.entity.frenchie.FrenchieRenderer;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.clock.ClockTimeMarker;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Set;

final class FrenchiePlaytest {
    private static final BlockPos PEN = new BlockPos(0, 201, 0);

    static void steps(Playtest t, int phase) {
        Playtest.require(phase == 1, "The Frenchie scenario runs in a single session");
        int[] id = new int[1];
        t.server("Spawn a standing Frenchie in the dry Overworld at noon", p -> {
            ServerLevel level = p.level().getServer().overworld();
            setTime(level, ClockTimeMarkers.NOON);
            level.getServer().setWeatherParameters(6000, 0, false, false);
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 4; z++) {
                    BlockPos feet = PEN.offset(x, 0, z);
                    level.setBlockAndUpdate(feet.below(), Blocks.SEA_LANTERN.defaultBlockState());
                    boolean edge = Math.abs(x) == 3 || z == -3 || z == 4;
                    level.setBlockAndUpdate(feet, (edge ? Blocks.OAK_FENCE : Blocks.AIR).defaultBlockState());
                    level.setBlockAndUpdate(feet.above(), Blocks.AIR.defaultBlockState());
                }
            }
            p.teleportTo(level, 0.5, 201, 2.5, Set.of(), 180, 19, true);
            p.setGameMode(GameType.SURVIVAL);
            p.getInventory().clearContent();
            FrenchieEntity dog = FloraFaunaRegistry.FRENCHIE.get().spawn(level, PEN, EntitySpawnReason.COMMAND);
            Playtest.require(dog != null, "Frenchie spawn failed");
            dog.tame(p);
            id[0] = dog.getId();
        });
        t.awaitServer("Confirm noon bed rules and an awake dog", p -> {
            FrenchieEntity dog = dog(p, id[0]);
            return !bedsAllowSleep(dog) && !dog.isOrderedToSit() && !dog.isInWater() && !dog.isFrenchieSleeping();
        });
        t.awaitClient("Confirm the tracked dog uses the Frenchie renderer", mc ->
                mc.level.getEntity(id[0]) instanceof FrenchieEntity dog
                        && mc.getEntityRenderDispatcher().getRenderer(dog) instanceof FrenchieRenderer);

        t.server("Move the Overworld clock to midnight", p -> setTime(p.level(), ClockTimeMarkers.MIDNIGHT));
        t.awaitServer("Wait for the dry standing dog to fall asleep naturally", p -> {
            FrenchieEntity dog = dog(p, id[0]);
            return bedsAllowSleep(dog) && !dog.isInWater() && !dog.isOrderedToSit() && dog.isFrenchieSleeping();
        });
        t.awaitClient("Wait for the client to play the looping sleep pose", mc -> {
            FrenchieEntity dog = (FrenchieEntity) mc.level.getEntity(id[0]);
            mc.player.lookAt(Anchor.EYES, dog.getEyePosition());
            return dog.isFrenchieSleeping() && dog.sleepAnimationState.isStarted();
        });
        t.screenshot("frenchie-sleeping");
        t.server("Move the Overworld clock to noon", p -> setTime(p.level(), ClockTimeMarkers.NOON));
        t.awaitServer("Wait for the dog to wake in daylight", p -> {
            FrenchieEntity dog = dog(p, id[0]);
            return !bedsAllowSleep(dog) && !dog.isFrenchieSleeping();
        });
        t.awaitClient("Wait for the wake transition to finish", mc -> {
            FrenchieEntity dog = (FrenchieEntity) mc.level.getEntity(id[0]);
            mc.player.lookAt(Anchor.EYES, dog.getEyePosition());
            return !dog.isFrenchieSleeping() && dog.getSleepTime() >= 20 && !dog.sleepAnimationState.isStarted();
        });
        t.screenshot("frenchie-awake");

        t.server("Return to midnight and stand beside the tamed dog", p -> {
            setTime(p.level(), ClockTimeMarkers.MIDNIGHT);
            FrenchieEntity dog = dog(p, id[0]);
            p.teleportTo(p.level(), dog.getX(), dog.getY(), dog.getZ() + 1.6, Set.of(), 180, 15, true);
        });
        t.awaitClient("Wait for the nearby Frenchie to fall asleep again", mc ->
                mc.level.getEntity(id[0]) instanceof FrenchieEntity dog && dog.isFrenchieSleeping()
                        && mc.player.distanceToSqr(dog) < 9);
        t.client("Right-click to order the sleeping Frenchie to sit", mc -> toggleSit(mc, id[0]));
        t.awaitServer("Verify sitting suppresses sleep while beds still allow it", p -> {
            FrenchieEntity dog = dog(p, id[0]);
            return bedsAllowSleep(dog) && dog.isOrderedToSit() && !dog.isFrenchieSleeping();
        });
        t.awaitClient("Wait for the client's sitting animation", mc -> {
            FrenchieEntity dog = (FrenchieEntity) mc.level.getEntity(id[0]);
            return dog.isSitting() && !dog.isFrenchieSleeping() && dog.sitPoseAnimationState.isStarted();
        });
        t.client("Right-click to release the sit order", mc -> toggleSit(mc, id[0]));
        t.awaitServer("Verify the standing dog falls asleep again", p -> {
            FrenchieEntity dog = dog(p, id[0]);
            return !dog.isOrderedToSit() && dog.isFrenchieSleeping();
        });
        t.server("Place the dog in water at the same time of night", p -> {
            FrenchieEntity dog = dog(p, id[0]);
            p.level().setBlockAndUpdate(dog.blockPosition(), Blocks.WATER.defaultBlockState());
        });
        t.awaitServer("Verify water wakes the dog while beds still allow sleep", p -> {
            FrenchieEntity dog = dog(p, id[0]);
            return dog.isInWater() && bedsAllowSleep(dog) && !dog.isFrenchieSleeping();
        });
        t.awaitClient("Wait for the water wake-up to reach the client", mc ->
                mc.level.getEntity(id[0]) instanceof FrenchieEntity dog && !dog.isFrenchieSleeping());
    }

    private static FrenchieEntity dog(ServerPlayer p, int id) {
        return (FrenchieEntity) p.level().getEntity(id);
    }

    private static boolean bedsAllowSleep(FrenchieEntity dog) {
        return dog.level().environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, dog.blockPosition()).canSleep(dog.level());
    }

    private static void setTime(ServerLevel level, ResourceKey<ClockTimeMarker> marker) {
        var clock = level.dimensionType().defaultClock().orElseThrow();
        Playtest.require(level.getServer().clockManager().moveToTimeMarker(clock, marker), "Overworld lacks time marker " + marker);
    }

    private static void toggleSit(Minecraft mc, int id) {
        FrenchieEntity dog = (FrenchieEntity) mc.level.getEntity(id);
        mc.player.lookAt(Anchor.EYES, dog.getEyePosition());
        mc.gameMode.interact(mc.player, dog, new EntityHitResult(dog, dog.getEyePosition()), InteractionHand.MAIN_HAND);
    }
}
