package net.j40climb.florafauna.test;

import net.j40climb.florafauna.common.entity.frenchie.FrenchieAwareness;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

final class FrenchieAwarenessGameTests {
    static void warningSight(GameTestHelper h) {
        var dog = h.spawnWithNoFreeWill(FloraFaunaRegistry.FRENCHIE.get(), 1, 1, 1);
        dog.tame(h.makeMockServerPlayerInLevel());
        var creeper = h.spawnWithNoFreeWill(EntityType.CREEPER, 3, 1, 1);
        h.setBlock(2, 1, 1, Blocks.STONE);
        h.setBlock(2, 2, 1, Blocks.STONE);
        h.assertTrue(FrenchieAwareness.findCreeper(h.getLevel(), dog) == null, "A solid wall must block the warning");
        h.setBlock(2, 1, 1, Blocks.AIR);
        h.setBlock(2, 2, 1, Blocks.AIR);
        h.assertTrue(FrenchieAwareness.findCreeper(h.getLevel(), dog) == creeper, "A nearby visible creeper should trigger the alarm");
        h.succeed();
    }

    static void bedtime(GameTestHelper h) {
        var owner = h.makeMockServerPlayerInLevel();
        var dog = h.spawnWithNoFreeWill(FloraFaunaRegistry.FRENCHIE.get(), 1, 1, 1);
        dog.tame(owner);
        dog.sitDown();
        var awareness = new FrenchieAwareness(dog);
        h.assertTrue(!awareness.isBedtimeBuddy(), "The icon should require an occupied owner bed");
        var bed = new BlockPos(2, 1, 1);
        h.setBlock(bed, Blocks.RED_BED);
        owner.startSleeping(h.absolutePos(bed));
        h.assertTrue(awareness.isBedtimeBuddy(), "A seated Frenchie near its owner's bed should qualify");
        dog.setFrenchieSleeping(true);
        FrenchieAwareness.onWake(new PlayerWakeUpEvent(owner, true, true));
        h.assertTrue(!owner.hasEffect(FloraFaunaRegistry.WELL_RESTED), "Leaving the bed early granted the bonus");
        owner.stopSleepInBed(true, true);
        h.assertTrue(!awareness.isBedtimeBuddy(), "The bedtime icon remained after waking");
        h.succeed();
    }
}

