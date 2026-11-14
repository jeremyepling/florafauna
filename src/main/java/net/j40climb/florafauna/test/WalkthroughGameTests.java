package net.j40climb.florafauna.test;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.common.block.iteminput.rootiteminput.ItemInputBlockEntity;
import net.j40climb.florafauna.common.block.iteminput.storageanchor.StorageAnchorBlockEntity;
import net.j40climb.florafauna.common.block.vacuum.AbstractVacuumBlockEntity;
import net.j40climb.florafauna.common.block.vacuum.VacuumState;
import net.j40climb.florafauna.common.block.mininganchor.pod.Tier2PodBlockEntity;
import net.j40climb.florafauna.common.mobsymbiote.MobSymbioteHelper;
import net.j40climb.florafauna.common.mobsymbiote.fear.FearHelper;
import net.j40climb.florafauna.common.mobsymbiote.fear.FearState;
import net.j40climb.florafauna.dev.DevPlot;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import static net.j40climb.florafauna.test.FloraFaunaGameTests.registerStructureTest;

/** Integration checks against the same saved structures used by the walkthrough. */
final class WalkthroughGameTests {
    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env) {
        registerStructureTest(event, env, "creeper_pen_gunpowder", DevPlot.CREEPER_FEAR.structure(), 800, helper -> {
            helper.spawnWithNoFreeWill(EntityTypes.CAT, 3, 1, 6);
            var creeper = helper.spawnWithNoFreeWill(EntityTypes.CREEPER, 3, 1, 12);
            checkFearCycle(helper, creeper, Items.GUNPOWDER, Config.gunpowderDropMin, Config.gunpowderDropMax);
        });
        registerStructureTest(event, env, "blaze_pen_rods", DevPlot.BLAZE_FEAR.structure(), 800, helper -> {
            var blaze = helper.spawnWithNoFreeWill(EntityTypes.BLAZE, 2, 1, 5);
            checkFearCycle(helper, blaze, Items.BLAZE_ROD, Config.blazeRodDropMin, Config.blazeRodDropMax);
        });
        registerStructureTest(event, env, "enderman_pen_pearls", DevPlot.ENDERMAN_FEAR.structure(), 800, helper -> {
            helper.assertBlockPresent(Blocks.POLISHED_DIORITE, 1, 1, 4);
            var enderman = helper.spawnWithNoFreeWill(EntityTypes.ENDERMAN, 2, 1, 5);
            checkFearCycle(helper, enderman, Items.ENDER_PEARL, Config.enderPearlDropMin, Config.enderPearlDropMax);
        });
        registerStructureTest(event, env, "saved_barrier_pen_filters", DevPlot.BARRIER_PENS.structure(), 80,
                WalkthroughGameTests::checkBarrierPens);
        registerStructureTest(event, env, "anchor_bench_hopper_payout", DevPlot.ANCHOR_BENCH.structure(), 150,
                WalkthroughGameTests::checkHopperPayout);
        registerStructureTest(event, env, "intake_retries_full_chest", DevPlot.ITEM_INPUT.structure(), 800,
                WalkthroughGameTests::checkFullChestRetry);
        registerStructureTest(event, env, "iron_garden_cycle", DevPlot.IRON_GARDEN.structure(), 10000,
                IronGardenGameTest::run);
    }

    private static void checkFearCycle(GameTestHelper helper, Mob mob, Item drop, int minimum, int maximum) {
        MobSymbioteHelper.applyMobSymbioteLevel1(mob, helper.getLevel().getGameTime());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(FearHelper.getFearState(mob) == FearState.PANICKED,
                        "Bonded mob did not panic in its pen"))
                .thenWaitUntil(() -> helper.assertTrue(FearHelper.getFearState(mob) == FearState.EXHAUSTED,
                        "Mob did not reach its cooldown"))
                .thenExecute(() -> {
                    int count = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds(),
                                    entity -> entity.getItem().is(drop))
                            .stream().mapToInt(entity -> entity.getItem().getCount()).sum();
                    helper.assertTrue(count >= minimum && count <= maximum, "Unexpected " + drop + " payout: " + count);
                    helper.assertTrue(mob.isAlive() && FearHelper.getLeakCount(mob) == 1, "Expected one leak from a living mob");
                })
                .thenSucceed();
    }

    private static void checkBarrierPens(GameTestHelper helper) {
        var cow = helper.spawnWithNoFreeWill(EntityTypes.COW, 2, 1, 5);
        var frenchie = helper.spawnWithNoFreeWill(FloraFaunaRegistry.FRENCHIE.get(), 3, 1, 5);
        var golem = helper.spawnWithNoFreeWill(EntityTypes.COPPER_GOLEM, 10, 1, 5);
        var west = helper.absolutePos(new BlockPos(1, 1, 5));
        var east = helper.absolutePos(new BlockPos(9, 1, 5));
        var level = helper.getLevel();
        var wall = level.getBlockState(west);
        helper.assertTrue(!wall.getCollisionShape(level, west, CollisionContext.of(frenchie)).isEmpty(), "Frenchie crossed its wall");
        helper.assertTrue(wall.getCollisionShape(level, west, CollisionContext.of(cow)).isEmpty(), "Frenchie wall blocked the cow");
        wall = level.getBlockState(east);
        helper.assertTrue(!wall.getCollisionShape(level, east, CollisionContext.of(cow)).isEmpty(), "Tag failed to block the cow");
        helper.assertTrue(!wall.getCollisionShape(level, east, CollisionContext.of(golem)).isEmpty(), "ID failed to block the copper golem");
        helper.succeed();
    }

    private static void checkHopperPayout(GameTestHelper helper) {
        var hopper = helper.getBlockEntity(new BlockPos(2, 1, 5), HopperBlockEntity.class);
        var pod = helper.getBlockEntity(new BlockPos(2, 2, 5), Tier2PodBlockEntity.class);
        helper.assertTrue(pod.getParentAnchorPos() == null, "Detached sample retained a world-coordinate parent");
        helper.succeedWhen(() -> {
            helper.assertTrue(pod.isEmpty(), "Detached pod still has stock");
            helper.assertTrue(hopper.countItem(Items.COBBLESTONE) == 3, "Hopper must receive exactly three cobblestone");
        });
    }

    private static void checkFullChestRetry(GameTestHelper helper) {
        BlockPos inputPos = new BlockPos(0, 1, 6);
        var input = helper.getBlockEntity(inputPos, ItemInputBlockEntity.class);
        var anchor = helper.getBlockEntity(new BlockPos(2, 1, 6), StorageAnchorBlockEntity.class);
        var chest = helper.getBlockEntity(new BlockPos(3, 1, 6), ChestBlockEntity.class);
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, new ItemStack(Items.STONE, 64));
        }
        input.pairWithAnchor(anchor.getBlockPos());
        anchor.linkContainer(chest.getBlockPos());
        helper.spawnItem(Items.DIAMOND, inputPos.above());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(input.getBuffer().getTotalItemCount() == 1
                        && helper.getBlockState(inputPos).getValue(AbstractVacuumBlockEntity.STATE) == VacuumState.BLOCKED,
                        "Full destination did not leave the collected diamond buffered"))
                .thenExecute(() -> chest.removeItem(0, 64))
                .thenWaitUntil(() -> helper.assertTrue(chest.countItem(Items.DIAMOND) == 1 && input.getBuffer().isEmpty(),
                        "Input did not retry after space became available"))
                .thenIdle(40)
                .thenExecute(() -> helper.assertTrue(chest.countItem(Items.DIAMOND) == 1 && input.getBuffer().isEmpty(),
                        "Retry duplicated the diamond"))
                .thenSucceed();
    }

    private WalkthroughGameTests() {}
}
