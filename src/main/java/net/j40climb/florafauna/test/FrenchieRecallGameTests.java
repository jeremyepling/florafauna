package net.j40climb.florafauna.test;

import net.minecraft.world.phys.Vec3;
import net.j40climb.florafauna.common.entity.frenchie.FrenchieEntity;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.BlockHitResult;

final class FrenchieRecallGameTests {
    static void recallParkedDog(GameTestHelper h) {
        var level = h.getLevel();
        var owner = h.makeMockServerPlayerInLevel();
        var stranger = h.makeMockServerPlayerInLevel();
        var dog = h.spawn(FloraFaunaRegistry.FRENCHIE.get(), 3, 1, 3);
        var id = dog.getUUID();
        dog.tame(owner);
        dog.sitDown();
        owner.getInventory().clearContent();
        owner.snapTo(dog.position().add(1, 0, 0), 0, 0);
        owner.setRespawnPosition(null, false);
        command(owner, "home");
        h.assertTrue(!dog.hasCompanionJob() && dog.isOrderedToSit(), "Missing bed must preserve the sit order");

        var bed = ChunkPos.containing(dog.blockPosition().west(640)).getMiddleBlockPosition(dog.getBlockY() + 12);
        level.getChunkAt(bed);
        for (var floor : BlockPos.betweenClosed(bed.offset(-2, -1, -2), bed.offset(2, -1, 2))) {
            level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        }
        var state = Blocks.BED.blue().defaultBlockState().setValue(BedBlock.FACING, Direction.EAST);
        level.setBlock(bed.west(), state.setValue(BedBlock.PART, BedPart.FOOT), Block.UPDATE_CLIENTS);
        level.setBlock(bed, state.setValue(BedBlock.PART, BedPart.HEAD), Block.UPDATE_CLIENTS);
        owner.setRespawnPosition(new ServerPlayer.RespawnConfig(
                LevelData.RespawnData.of(level.dimension(), bed, 0, 0), false), false);
        command(owner, "home");
        h.assertTrue(dog.isGoingHome() && !dog.isDelivering(), "Send home required a parcel");
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(level.getEntity(id) == null, "Dog never departed"))
                .thenWaitUntil(() -> h.assertTrue(level.getEntity(id) instanceof FrenchieEntity parked
                                && parked.isGoingHome() && parked.isOrderedToSit()
                                && parked.position().distanceToSqr(Vec3.atBottomCenterOf(bed)) < 16,
                        "Dog did not park beside the owner's bed"))
                .thenWaitUntil(() -> h.assertTrue(!level.hasChunkAt(bed) && level.getEntity(id) == null,
                        "Waiting dog kept its bed loaded"))
                .thenExecute(() -> command(stranger, "recall"))
                .thenExecuteAfter(30, () -> {
                    h.assertTrue(!level.hasChunkAt(bed), "Another player recalled the dog");
                    command(owner, "recall");
                    command(owner, "recall");
                })
                .thenWaitUntil(() -> h.assertTrue(level.getEntity(id) instanceof FrenchieEntity back
                                && back.isOwnedBy(owner) && !back.hasCompanionJob() && !back.isOrderedToSit()
                                && back.distanceToSqr(owner) < 36 && owner.getInventory().isEmpty(),
                        "Recall did not return the original empty dog from its unloaded bed"))
                .thenExecute(() -> {
                    level.getServer().getPlayerList().remove(owner);
                    level.getServer().getPlayerList().remove(stranger);
                }).thenSucceed();
    }

    static void sendUsingBed(GameTestHelper h) {
        var level = h.getLevel();
        var owner = h.makeMockServerPlayerInLevel();
        try {
            var dog = h.spawn(FloraFaunaRegistry.FRENCHIE.get(), 3, 1, 3);
            dog.tame(owner);
            dog.sitDown();
            var bed = dog.blockPosition().east(2);
            var state = Blocks.BED.red().defaultBlockState().setValue(BedBlock.FACING, Direction.EAST);
            level.setBlock(bed.west(), state.setValue(BedBlock.PART, BedPart.FOOT), Block.UPDATE_CLIENTS);
            level.setBlock(bed, state.setValue(BedBlock.PART, BedPart.HEAD), Block.UPDATE_CLIENTS);
            owner.setRespawnPosition(new ServerPlayer.RespawnConfig(
                    LevelData.RespawnData.of(level.dimension(), bed, 0, 0), false), false);
            owner.snapTo(dog.position().add(1, 0, -1), 0, 0);
            owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            owner.setShiftKeyDown(true);
            var hit = new BlockHitResult(Vec3.atCenterOf(bed.west()), Direction.UP, bed.west(), false);
            owner.gameMode.useItemOn(owner, level, owner.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            h.assertTrue(dog.isGoingHome() && !dog.isDelivering() && !owner.isSleeping(),
                    "Sneak-use on the foot of the owner's bed did not send the unladen Frenchie home");
            h.succeed();
        } finally {
            level.getServer().getPlayerList().remove(owner);
        }
    }

    private static void command(ServerPlayer owner, String order) {
        owner.level().getServer().getCommands().performPrefixedCommand(owner.createCommandSourceStack(), "frenchie " + order);
    }
}
