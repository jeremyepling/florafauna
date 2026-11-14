package net.j40climb.florafauna.test;

import io.netty.buffer.Unpooled;
import net.j40climb.florafauna.common.entity.frenchie.FrenchieEntity;
import net.j40climb.florafauna.common.entity.frenchie.FrenchieVariant;
import net.j40climb.florafauna.common.entity.frenchie.frontpack.FrontpackData;
import net.j40climb.florafauna.common.entity.frenchie.frontpack.FrontpackEvents;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.UUID;

final class FrenchieGameTests {
    static void ownerOrders(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = helper.makeMockPlayer(GameType.SURVIVAL);
        var visitor = helper.makeMockPlayer(GameType.SURVIVAL);
        var dog = helper.spawnWithNoFreeWill(FloraFaunaRegistry.FRENCHIE.get(), 1, 1, 1);
        var oldTag = new CompoundTag();
        oldTag.putString("Pose", "unknown_pose");
        oldTag.putInt("Variant", -1);
        dog.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), oldTag));
        helper.assertTrue(dog.getPose() == Pose.STANDING && dog.getVariant() == FrenchieVariant.FAWN,
                "Unsupported saved pose and coat should fall back to standing and fawn");
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE));
        dog.mobInteract(owner, InteractionHand.MAIN_HAND);
        helper.assertTrue(dog.isOwnedBy(owner) && dog.isOrderedToSit(), "Taming should make the new owner's dog sit");
        dog.mobInteract(visitor, InteractionHand.MAIN_HAND);
        helper.assertTrue(dog.isOrderedToSit() && dog.isSitting(), "A visitor changed the owner's sit order");
        dog.mobInteract(owner, InteractionHand.MAIN_HAND);
        helper.assertTrue(!dog.isOrderedToSit() && !dog.isSitting(), "Owner could not release the sit order");
        helper.succeed();
    }

    static void frontpackRespawnRelease(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = helper.makeMockPlayer(GameType.SURVIVAL);
        var dog = helper.spawnWithNoFreeWill(FloraFaunaRegistry.FRENCHIE.get(), 1, 1, 1);
        dog.tame(owner);
        dog.setCustomName(Component.literal("Pip"));
        UUID id = dog.getUUID();
        owner.setShiftKeyDown(true);
        FrontpackEvents.onEntityInteract(new PlayerInteractEvent.EntityInteract(owner, InteractionHand.MAIN_HAND, dog));
        helper.assertTrue(dog.isRemoved(), "Pickup left the original dog in the level");

        var respawned = helper.makeMockPlayer(GameType.SURVIVAL);
        respawned.setUUID(owner.getUUID());
        respawned.copyAttachmentsFrom(owner, true);
        var pack = FloraFaunaRegistry.FRENCH_FRONTPACK_DATA;
        helper.assertTrue(respawned.getData(pack).hasCarriedFrenchie(), "Death dropped the packed dog");

        BlockPos feet = helper.absolutePos(new BlockPos(0, 1, 0));
        respawned.snapTo(feet, 0, 80);
        BlockPos target = feet.south(2);
        level.setBlockAndUpdate(target.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(target, Blocks.STONE.defaultBlockState());
        FrontpackEvents.putDown(respawned);
        helper.assertTrue(respawned.getData(pack).hasCarriedFrenchie(), "An obstructed release consumed the dog");
        level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());

        // A UUID collision also makes ServerLevel.addFreshEntity return false.
        var conflicting = FloraFaunaRegistry.FRENCHIE.get().create(level, EntitySpawnReason.COMMAND);
        helper.assertTrue(conflicting != null, "Could not create a dog for the duplicate UUID fixture");
        conflicting.setUUID(id);
        conflicting.snapTo(feet, 0, 0);
        helper.assertTrue(level.addFreshEntity(conflicting), "Could not set up duplicate UUID fixture");
        FrontpackEvents.putDown(respawned);
        helper.assertTrue(respawned.getData(pack).hasCarriedFrenchie(), "A rejected entity spawn consumed the dog");
        conflicting.discard();

        FrontpackEvents.putDown(respawned);
        helper.assertTrue(!respawned.getData(pack).hasCarriedFrenchie(), "Successful release left a second copy in the pack");
        var restored = level.getEntity(id);
        helper.assertTrue(restored instanceof FrenchieEntity pet && pet.getOwnerReference() != null
                && pet.getOwnerReference().getUUID().equals(owner.getUUID()) && pet.getName().getString().equals("Pip")
                && pet.blockPosition().equals(target), "Released dog lost its identity or landed inside the floor");
        FrontpackEvents.putDown(respawned);
        helper.assertTrue(level.getEntity(id) == restored, "Repeat release replaced the dog");
        helper.succeed();
    }

    static void frontpackAppearance(GameTestHelper helper) {
        var saved = new CompoundTag();
        saved.putInt("Variant", FrenchieVariant.BRINDLE.getId());
        saved.putString("ServerOnly", "test data");
        var full = new FrontpackData(true, saved, 123);
        var bytes = Unpooled.buffer();
        try {
            FrontpackData.STREAM_CODEC.encode(bytes, full);
            var visual = FrontpackData.STREAM_CODEC.decode(bytes);
            helper.assertTrue(visual.carriedCoat() == FrenchieVariant.BRINDLE && visual.pickupTimestamp() == 123
                    && !visual.frenchieNBT().contains("ServerOnly"), "Tracking clients should receive the coat without the saved entity data");
            helper.assertTrue(full.frenchieNBT().contains("ServerOnly"), "Visual sync changed the stored dog");
        } finally {
            bytes.release();
        }
        helper.succeed();
    }
}
