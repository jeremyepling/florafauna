package net.j40climb.florafauna.test;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.common.mobsymbiote.MobSymbioteHelper;
import net.j40climb.florafauna.common.mobsymbiote.fear.FearHelper;
import net.j40climb.florafauna.common.mobsymbiote.fear.FearState;
import net.j40climb.florafauna.dev.DevPlot;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
//? if >=26.3 {
/*import net.minecraft.world.entity.EntityTypes;
*///?}
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.stream.Stream;

import static net.j40climb.florafauna.test.FloraFaunaGameTests.registerStructureTest;
import static net.j40climb.florafauna.test.FloraFaunaGameTests.registerTest;

final class FearHabitatGameTests {
    private static final BlockPos BLAZE_PEN = new BlockPos(3, 1, 12);

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env) {
        registerStructureTest(event, env, "fear_habitat_uninterrupted_rest", DevPlot.CREEPER_FEAR.structure(),
                15000, FearHabitatGameTests::uninterruptedRest);
        registerStructureTest(event, env, "fear_habitat_overstress", DevPlot.CREEPER_FEAR.structure(),
                1000, FearHabitatGameTests::overstress);
        registerTest(event, env, "fear_habitat_bond_persistence", FearHabitatGameTests::bondPersistence);
    }

    private static void setColdBlocks(GameTestHelper helper, Block block) {
        helper.setBlock(BLAZE_PEN.west(), block);
        helper.setBlock(BLAZE_PEN.west().above(), block);
    }

    private static void uninterruptedRest(GameTestHelper helper) {
        // Panic adds an avoidance goal even to a fixture spawned without free will.
        // Keep the blaze in range of the cold wall and sheltered through its entire rest.
        helper.setBlock(BLAZE_PEN.below(), Blocks.STONE);
        helper.setBlock(BLAZE_PEN.above(2), Blocks.STONE);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            helper.setBlock(BLAZE_PEN.relative(side), Blocks.STONE);
            helper.setBlock(BLAZE_PEN.relative(side).above(), Blocks.STONE);
        }
        var blaze = helper.spawnWithNoFreeWill(EntityType.BLAZE, BLAZE_PEN);
        bond(helper, blaze);
        setColdBlocks(helper, Blocks.SNOW_BLOCK);
        int checkInterval = Config.fearCheckIntervalTicks;
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(FearHelper.getFearState(blaze) == FearState.EXHAUSTED
                        && FearHelper.getLeakCount(blaze) == 1, "Blaze did not exhaust itself producing its first rods"))
                .thenExecuteAfter(2 * checkInterval, () -> setColdBlocks(helper, Blocks.STONE))
                .thenExecuteAfter(8 * checkInterval, () -> setColdBlocks(helper, Blocks.SNOW_BLOCK))
                .thenExecuteAfter(2 * checkInterval, () -> setColdBlocks(helper, Blocks.STONE))
                .thenExecuteFor(Config.exhaustedCooldownTicks - 2 * checkInterval, () ->
                        helper.assertTrue(FearHelper.getLeakCount(blaze) == 1,
                                "Brief or interrupted rest cleared stress before a full quiet period"))
                .thenExecuteAfter(4 * checkInterval, () -> helper.assertTrue(FearHelper.getLeakCount(blaze) == 0
                        && FearHelper.getFearState(blaze) == FearState.CALM, "Full uninterrupted rest did not clear stress"))
                .thenExecute(() -> setColdBlocks(helper, Blocks.SNOW_BLOCK))
                .thenWaitUntil(() -> helper.assertTrue(blaze.isAlive() && FearHelper.getLeakCount(blaze) == 1
                        && FearHelper.getFearState(blaze) == FearState.EXHAUSTED,
                        "Rested blaze failed to harvest again: " + FearHelper.getFearData(blaze)
                                + ", health=" + blaze.getHealth() + ", pos=" + blaze.blockPosition()))
                .thenSucceed();
    }

    private static void overstress(GameTestHelper helper) {
        var cat = helper.spawnWithNoFreeWill(EntityType.CAT, 3, 1, 6);
        var creeper = helper.spawnWithNoFreeWill(EntityType.CREEPER, 3, 1, 12);
        bond(helper, creeper);
        // Resume an occupied pen at the end of its last safe cooldown.
        int priorLeaks = Config.maxLeaksBeforeOverstress - 1;
        for (int i = 0; i < priorLeaks; i++) FearHelper.incrementLeakCount(creeper);
        FearHelper.setFearState(creeper, FearState.EXHAUSTED, helper.getLevel().getGameTime() - Config.exhaustedCooldownTicks);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(FearHelper.getFearState(creeper) == FearState.PANICKED,
                        "A cat kept in the pen did not cause renewed panic"))
                .thenExecute(cat::discard)
                .thenWaitUntil(() -> helper.assertTrue(FearHelper.getFearState(creeper) == FearState.CALM,
                        "Removing the cat did not stop panic"))
                .thenExecute(() -> {
                    helper.assertTrue(FearHelper.getLeakCount(creeper) == priorLeaks, "Brief relief erased earlier leaks");
                    helper.spawnWithNoFreeWill(EntityType.CAT, 3, 1, 6);
                })
                .thenWaitUntil(() -> helper.assertTrue(creeper.isIgnited() && FearHelper.getFearState(creeper) == FearState.OVERSTRESS,
                        "Repeated fear without a full rest did not trigger overstress"))
                .thenExecute(creeper::discard)
                .thenSucceed();
    }

    private static void bond(GameTestHelper helper, Mob mob) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var symbiote = new ItemStack(FloraFaunaRegistry.MOB_SYMBIOTE.get());
        symbiote.interactLivingEntity(player, mob, InteractionHand.MAIN_HAND);
        helper.assertTrue(MobSymbioteHelper.hasMobSymbiote(mob), "Failed to bond " + mob.getType());
    }

    private static void bondPersistence(GameTestHelper helper) {
        checkPersistence(helper, EntityType.CREEPER);
        checkPersistence(helper, EntityType.BLAZE);
        checkPersistence(helper, EntityType.ENDERMAN);
        helper.succeed();
    }

    private static void checkPersistence(GameTestHelper helper, EntityType<? extends Mob> type) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        // GameTestHelper.spawn marks every mob persistent, hiding the original regression.
        var mob = type.create(level, EntitySpawnReason.NATURAL);
        mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        helper.assertTrue(level.addFreshEntity(mob) && !mob.isPersistenceRequired(), "Unbonded mob already persistent: " + type);
        bond(helper, mob);
        helper.assertTrue(mob.isPersistenceRequired(), "New bond did not protect " + type + " from despawning");

        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        mob.saveWithoutId(output);
        var oldSave = output.buildResult();
        oldSave.putBoolean("PersistenceRequired", false);
        mob.discard();
        var reloaded = type.create(level, EntitySpawnReason.LOAD);
        reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), oldSave));
        helper.assertTrue(MobSymbioteHelper.hasMobSymbiote(reloaded), "Saved bond failed to reload on " + type);
        level.addLegacyChunkEntities(Stream.of(reloaded));
        helper.assertTrue(level.getEntity(reloaded.getUUID()) == reloaded && reloaded.isPersistenceRequired(),
                "Previously bonded " + type + " still despawns after loading from disk");
    }
}
