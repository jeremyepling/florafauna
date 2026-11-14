package net.j40climb.florafauna.test;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.common.mobsymbiote.irongarden.IronGardenHelper;
import net.j40climb.florafauna.common.mobsymbiote.irongarden.IronGardenState;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
//? if <26.3 {
import net.minecraft.world.entity.EntityType;
//?} else {
/*import net.minecraft.world.entity.EntityTypes;
*///?}
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.HashMap;
import java.util.Map;

final class IronGardenGameTest {
    private final GameTestHelper helper;
    private final IronGolem golem;
    private final ChestBlockEntity chest;
    private final Map<BlockPos, Block> flowers = new HashMap<>();
    private boolean planted;
    private boolean converted;
    private boolean harvested;

    static void run(GameTestHelper helper) {
        new IronGardenGameTest(helper).checkCycle();
    }

    private IronGardenGameTest(GameTestHelper helper) {
        this.helper = helper;
        BlockPos.betweenClosedStream(new BlockPos(3, 1, 13), new BlockPos(7, 1, 17))
                .forEach(pos -> flowers.put(pos.immutable(), helper.getBlockState(pos).getBlock()));
        helper.assertTrue(flowers.containsValue(Blocks.POPPY) && flowers.containsValue(Blocks.AIR)
                && !flowers.containsValue(FloraFaunaRegistry.FERRIC_POPPY.get()), "Expected red poppies and open soil");
        chest = helper.getBlockEntity(new BlockPos(2, 1, 15), ChestBlockEntity.class);
        helper.assertTrue(chest.isEmpty(), "Garden collection chest must start empty");
        golem = helper.spawn(EntityType.IRON_GOLEM, 5, 1, 15);
    }

    private void checkCycle() {
        var kit = helper.getBlockEntity(new BlockPos(4, 1, 1), BarrelBlockEntity.class);
        var symbiote = kit.getItem(1);
        helper.assertTrue(symbiote.is(FloraFaunaRegistry.MOB_SYMBIOTE.get()), "Garden kit needs a Mob Symbiote");
        var gardener = helper.makeMockPlayer(GameType.SURVIVAL);
        symbiote.interactLivingEntity(gardener, golem, InteractionHand.MAIN_HAND);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(IronGardenHelper.getData(golem).ironGardenState().isCalm(),
                        "Using the Mob Symbiote did not calm the golem"))
                .thenWaitUntil(() -> {
                    observeFlowers();
                    helper.assertTrue(planted, "Golem did not plant a red poppy on bare soil");
                    helper.assertTrue(converted, "No red poppy turned ferric");
                    helper.assertTrue(harvested, "No ferric poppy was removed from the bed");
                    helper.assertTrue(chest.countItem(FloraFaunaRegistry.FERRIC_POPPY_ITEM.get()) > 0,
                            "No harvested ferric poppy reached the collection chest");
                })
                .thenExecute(() -> {
                    var level = helper.getLevel();
                    helper.assertTrue(golem.hurtServer(level, level.damageSources().generic(), 1),
                            "Golem must take damage to interrupt gardening");
                    planted = false;
                    harvested = false;
                })
                .thenExecuteFor(Config.ironGardenCalmRequiredTicks, () -> {
                    helper.assertTrue(IronGardenHelper.getData(golem).ironGardenState() == IronGardenState.BONDED_NOT_CALM,
                            "Damaged golem became calm before its cooldown expired");
                    flowers.forEach((pos, block) -> helper.assertBlockPresent(block, pos));
                })
                .thenExecuteAfter(Config.ironGardenCheckIntervalTicks, () ->
                    helper.assertTrue(IronGardenHelper.getData(golem).ironGardenState().isCalm(),
                            "Golem did not regain calm after its combat cooldown"))
                .thenWaitUntil(() -> {
                    observeFlowers();
                    helper.assertTrue(planted || harvested,
                            "Golem did not plant or harvest again after combat");
                })
                .thenSucceed();
    }

    private void observeFlowers() {
        var ferric = FloraFaunaRegistry.FERRIC_POPPY.get();
        flowers.replaceAll((pos, previous) -> {
            Block current = helper.getBlockState(pos).getBlock();
            planted |= previous == Blocks.AIR && current == Blocks.POPPY;
            converted |= previous == Blocks.POPPY && current == ferric;
            harvested |= previous == ferric && current == Blocks.AIR;
            return current;
        });
    }
}
