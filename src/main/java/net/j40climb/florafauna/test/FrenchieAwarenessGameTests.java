package net.j40climb.florafauna.test;

import net.j40climb.florafauna.common.entity.frenchie.FrenchieAwareness;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;

final class FrenchieAwarenessGameTests {
    static void warningSight(GameTestHelper h) {
        for (BlockPos pos : BlockPos.betweenClosed(1, 1, 1, 3, 2, 1)) h.setBlock(pos, Blocks.AIR);
        var dog = h.spawnWithNoFreeWill(FloraFaunaRegistry.FRENCHIE.get(), 1, 1, 1);
        dog.tame(h.makeMockPlayer(GameType.SURVIVAL));
        dog.sitDown();
        var creeper = h.spawnWithNoFreeWill(EntityType.CREEPER, 3, 1, 1);
        h.setBlock(2, 1, 1, Blocks.STONE);
        h.setBlock(2, 2, 1, Blocks.STONE);
        h.assertTrue(FrenchieAwareness.findCreeper(h.getLevel(), dog) == null, "A solid wall must block the warning");
        h.setBlock(2, 1, 1, Blocks.AIR);
        h.setBlock(2, 2, 1, Blocks.AIR);
        h.succeedWhen(() -> {
            h.assertTrue(FrenchieAwareness.findCreeper(h.getLevel(), dog) == creeper, "A nearby visible creeper should trigger the alarm");
            h.assertTrue(dog.isWarningFlash(), "The visible creeper should trigger a warning");
        });
    }

    static void bedtime(GameTestHelper h) {
        var owner = h.makeMockPlayer(GameType.SURVIVAL);
        var dog = h.spawnWithNoFreeWill(FloraFaunaRegistry.FRENCHIE.get(), 1, 1, 1);
        dog.tame(owner);
        dog.sitDown();
        h.assertTrue(!dog.isBedtimeBuddy(), "The icon should require an occupied owner bed");
        var bed = new BlockPos(2, 1, 1);
        var head = h.absolutePos(bed);
        var state = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH);
        h.getLevel().setBlock(head.south(), state.setValue(BedBlock.PART, BedPart.FOOT), Block.UPDATE_CLIENTS);
        h.getLevel().setBlock(head, state.setValue(BedBlock.PART, BedPart.HEAD), Block.UPDATE_CLIENTS);
        owner.startSleeping(head);
        h.assertTrue(h.getLevel().getBlockState(head).is(Blocks.RED_BED), "Bed disappeared: " + h.getLevel().getBlockState(head));
        h.assertTrue(h.getLevel().getBlockState(head).getValue(BedBlock.OCCUPIED), "Bed isn't occupied");
        h.assertTrue(dog.getOwner() == owner && owner.isSleeping(), "Dog did not resolve its sleeping owner");
        dog.tick();
        h.assertTrue(dog.isBedtimeBuddy() && dog.isFrenchieSleeping(), "A seated Frenchie near its owner's bed should qualify");
        owner.stopSleepInBed(true, true);
        h.assertTrue(!owner.hasEffect(FloraFaunaRegistry.WELL_RESTED), "Leaving the bed early granted the bonus");
        dog.tick();
        h.assertTrue(!dog.isBedtimeBuddy() && dog.isOrderedToSit(), "Waking should restore the sit order");
        h.succeed();
    }
}

