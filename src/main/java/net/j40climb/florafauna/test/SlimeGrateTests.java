package net.j40climb.florafauna.test;

import net.j40climb.florafauna.common.block.slimegrate.SlimeGrateBlock;
import net.j40climb.florafauna.common.mobsymbiote.MobSymbioteHelper;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

final class SlimeGrateTests {
    static void harvestAndReverse(GameTestHelper helper) {
        BlockPos grate = new BlockPos(2, 2, 2);
        BlockPos near = grate.west(2).below();
        BlockPos far = grate.east(2).below();
        for (BlockPos floor : BlockPos.betweenClosed(near.north(), far.south())) helper.setBlock(floor, Blocks.STONE);
        helper.setBlock(grate.below(), Blocks.HOPPER);
        helper.setBlock(grate, FloraFaunaRegistry.SLIME_GRATE.get().defaultBlockState()
                .setValue(SlimeGrateBlock.AXIS, Direction.Axis.X));
        helper.setBlock(near, Blocks.NOTE_BLOCK);
        helper.setBlock(far, Blocks.NOTE_BLOCK);
        Slime farmed = helper.spawn(EntityType.SLIME, grate.west(2));
        farmed.setSize(2, true);
        MobSymbioteHelper.applyMobSymbioteLevel1(farmed, helper.getLevel().getGameTime());
        Slime wild = helper.spawn(EntityType.SLIME, near.above().north());
        wild.setNoAi(true);
        helper.setBlock(far.south(), Blocks.REDSTONE_BLOCK);
        helper.runAtTickTime(26, () -> {
            helper.assertTrue(farmed.getData(FloraFaunaRegistry.SLIME_LAST_SHED) >= 0, "A played note must lure the slime through the grate");
            helper.assertTrue(farmed.getX() > helper.absolutePos(grate).getX() + 0.5, "Slime must reach far side");
            helper.assertTrue(!wild.hasData(FloraFaunaRegistry.SLIME_NOTE), "Ordinary slimes must ignore the note");
            helper.setBlock(near.south(), Blocks.REDSTONE_BLOCK);
        });
        helper.runAtTickTime(58, () -> {
            helper.assertTrue(farmed.getX() < helper.absolutePos(grate).getX() + 0.5, "Return note must lure the slime back");
            HopperBlockEntity hopper = helper.getBlockEntity(grate.below(), HopperBlockEntity.class);
            helper.assertTrue(hopper.countItem(Items.SLIME_BALL) == 1, "Hopper must collect exactly one ball across both crossings");
            helper.assertTrue(farmed.getSize() == 2 && farmed.getHealth() == 4 && farmed.isPersistenceRequired(),
                    "The same bonded animal must retain size, health and persistence");
            helper.succeed();
        });
    }
}
