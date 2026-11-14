package net.j40climb.florafauna.test;

import net.j40climb.florafauna.common.mobsymbiote.MobSymbioteHelper;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
//? if <26.3 {
import net.minecraft.world.entity.EntityType;
//?} else {
/*import net.minecraft.world.entity.EntityTypes;*/
//?}
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class SpiderSilkTests {
    private static final BlockPos GAP = new BlockPos(2, 1, 2);

    private static Spider habitat(GameTestHelper test) {
        for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 5, 0, 4)) test.setBlock(pos, Blocks.STONE);
        test.setBlock(GAP.west(), Blocks.OAK_FENCE);
        test.setBlock(GAP.east(2), Blocks.OAK_FENCE);
        test.setBlock(GAP.west().above(), Blocks.STONE);
        return test.spawn(EntityType.SPIDER, GAP);
    }

    private static int string(GameTestHelper test) {
        AABB pen = new AABB(test.absolutePos(GAP)).inflate(3);
        return test.getLevel().getEntitiesOfClass(ItemEntity.class, pen, i -> i.getItem().is(Items.STRING))
                .stream().mapToInt(i -> i.getItem().getCount()).sum();
    }

    static void earlyCut(GameTestHelper test) {
        Spider spider = habitat(test);
        MobSymbioteHelper.applyMobSymbioteLevel1(spider, test.getLevel().getGameTime());
        float health = spider.getHealth();
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        ItemStack shears = new ItemStack(Items.SHEARS);
        player.setItemInHand(InteractionHand.MAIN_HAND, shears);
        test.startSequence().thenWaitUntil(() -> {
            test.assertTrue(spider.getData(FloraFaunaRegistry.SILK_HANGING), "Spider did not hang");
            test.assertBlockPresent(FloraFaunaRegistry.SILK_LINE.get(), GAP.east());
        }).thenExecute(() -> {
            BlockPos pos = test.absolutePos(GAP.east());
            var state = test.getLevel().getBlockState(pos);
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
            test.assertTrue(state.useItemOn(shears, test.getLevel(), player, InteractionHand.MAIN_HAND, hit).consumesAction(), "Occupied line rejected shears");
        }).thenExecuteAfter(8, () -> {
            test.assertTrue(spider.isAlive() && spider.getHealth() == health && !spider.getData(FloraFaunaRegistry.SILK_HANGING), "Early shear hurt or stranded spider");
            test.assertBlockPresent(Blocks.AIR, GAP);
            test.assertBlockPresent(Blocks.AIR, GAP.east());
            test.assertTrue(string(test) == 1 && shears.getDamageValue() == 1, "Expected one string and one use of shears");
        }).thenExecuteAfter(50, () -> test.assertBlockPresent(Blocks.AIR, GAP)).thenSucceed();
    }

    static void dispenser(GameTestHelper test) {
        Spider spider = habitat(test);
        MobSymbioteHelper.applyMobSymbioteLevel1(spider, test.getLevel().getGameTime());
        BlockPos dispenser = GAP.north();
        test.startSequence().thenWaitUntil(() -> test.assertTrue(spider.getData(FloraFaunaRegistry.SILK_HANGING), "Spider did not spin"))
                .thenWaitUntil(() -> test.assertTrue(!spider.getData(FloraFaunaRegistry.SILK_HANGING), "Spider did not leave"))
                .thenExecute(() -> {
                    test.assertBlockPresent(FloraFaunaRegistry.SILK_LINE.get(), GAP);
                    test.setBlock(dispenser, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.SOUTH));
                    var be = test.getBlockEntity(dispenser, DispenserBlockEntity.class);
                    be.setItem(0, new ItemStack(Items.SHEARS));
                    test.setBlock(dispenser.north(), Blocks.REDSTONE_BLOCK);
                }).thenExecuteAfter(12, () -> {
                    var shears = test.getBlockEntity(dispenser, DispenserBlockEntity.class).getItem(0);
                    test.assertTrue(shears.is(Items.SHEARS) && shears.getDamageValue() == 1 && string(test) == 1, "Dispenser should shear one intact line");
                    test.assertBlockPresent(Blocks.AIR, GAP.east());
                }).thenSucceed();
    }

    static void blocked(GameTestHelper test) {
        Spider spider = habitat(test);
        test.runAtTickTime(55, () -> {
            test.assertBlockPresent(Blocks.AIR, GAP);
            // Obstruct one cell before bonding; existing player blocks must never be replaced.
            test.setBlock(GAP.east(), Blocks.GLASS);
            MobSymbioteHelper.applyMobSymbioteLevel1(spider, test.getLevel().getGameTime());
        });
        test.runAtTickTime(125, () -> {
            test.assertBlockPresent(Blocks.AIR, GAP);
            test.assertBlockPresent(Blocks.GLASS, GAP.east());
            test.succeed();
        });
    }

    private SpiderSilkTests() {}
}
