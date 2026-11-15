package net.j40climb.florafauna.test;

import net.j40climb.florafauna.common.entity.frenchie.FrenchieEntity;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

final class FrenchieBedPreviewGameTest {
    static void respawnPreview(GameTestHelper h) {
        var owner = h.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(owner.connection.getConnection());
        try {
            var dog = h.spawnWithNoFreeWill(FloraFaunaRegistry.FRENCHIE.get(), 2, 1, 2);
            dog.tame(owner);
            dog.sitDown();
            var bundle = new ItemStack(Items.BUNDLE);
            owner.setGameMode(GameType.SURVIVAL);
            owner.setItemInHand(InteractionHand.MAIN_HAND, bundle);
            owner.snapTo(dog.position().add(0, 0, -1), 0, 0);
            BlockPos first = dog.blockPosition().east();
            BlockPos second = first.south();
            var level = owner.level();
            var bed = Blocks.BED.blue().defaultBlockState().setValue(BedBlock.FACING, Direction.EAST);
            for (var head : new BlockPos[]{first, second}) {
                level.setBlock(head.west(), bed.setValue(BedBlock.PART, BedPart.FOOT), Block.UPDATE_CLIENTS);
                level.setBlock(head, bed.setValue(BedBlock.PART, BedPart.HEAD), Block.UPDATE_CLIENTS);
            }
            setSpawn(owner, first, true);
            preview(owner, dog);
            send(owner, dog);
            assertStillHeld(h, owner, dog, bundle, "A forced spawn on a bed was offered as a home");

            BlockPos anchor = second.east();
            level.setBlockAndUpdate(anchor, Blocks.RESPAWN_ANCHOR.defaultBlockState());
            setSpawn(owner, anchor, false);
            preview(owner, dog);
            send(owner, dog);
            assertStillHeld(h, owner, dog, bundle, "A respawn anchor was accepted as a bed");

            setSpawn(owner, first, false);
            preview(owner, dog);
            setSpawn(owner, second, false);
            send(owner, dog);
            assertStillHeld(h, owner, dog, bundle, "The preview silently switched to another bed");

            preview(owner, dog);
            level.removeBlock(second, false);
            send(owner, dog);
            assertStillHeld(h, owner, dog, bundle, "A broken bed took the owner's bundle");

            setSpawn(owner, first, false);
            preview(owner, dog);
            assertStillHeld(h, owner, dog, bundle, "Choosing a valid bed took the bundle before confirmation");
            send(owner, dog);
            h.assertTrue(dog.isDelivering() && owner.getMainHandItem().isEmpty(),
                    "A fresh confirmation for the current bed should send the bundle");
            h.succeed();
        } finally {
            owner.level().getServer().getPlayerList().remove(owner);
        }
    }

    private static void setSpawn(ServerPlayer owner, BlockPos head, boolean forced) {
        var point = LevelData.RespawnData.of(owner.level().dimension(), head, 0, 0);
        owner.setRespawnPosition(new ServerPlayer.RespawnConfig(point, forced), false);
    }

    private static void preview(ServerPlayer owner, FrenchieEntity dog) {
        owner.interactOn(dog, InteractionHand.MAIN_HAND, Vec3.ZERO);
        order(owner, dog, "choose home");
    }

    private static void send(ServerPlayer owner, FrenchieEntity dog) {
        order(owner, dog, "send home");
    }

    private static void order(ServerPlayer owner, FrenchieEntity dog, String action) {
        owner.level().getServer().getCommands().performPrefixedCommand(owner.createCommandSourceStack(),
                "frenchiedelivery " + dog.getStringUUID() + " " + action);
    }

    private static void assertStillHeld(GameTestHelper h, ServerPlayer owner, FrenchieEntity dog, ItemStack original, String message) {
        h.assertTrue(!dog.hasCompanionJob() && owner.getMainHandItem() == original && original.getCount() == 1, message);
    }
}
