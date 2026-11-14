package net.j40climb.florafauna.test;

import net.j40climb.florafauna.common.entity.frenchie.FrenchieEntity;
import net.j40climb.florafauna.common.entity.frenchie.frontpack.FrontpackEvents;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

final class FrenchieDeliveryGameTests {
    private static final InteractionHand HAND = InteractionHand.MAIN_HAND;

    static void compassOrders(GameTestHelper h) {
        var owner = h.makeMockServerPlayerInLevel();
        try {
            var dog = h.spawnWithNoFreeWill(FloraFaunaRegistry.FRENCHIE.get(), 1, 1, 1);
            owner.snapTo(dog.position(), 0, 0);
            owner.setGameMode(GameType.SURVIVAL);
            owner.setItemInHand(HAND, new ItemStack(Items.BONE));
            click(owner, dog);
            h.assertTrue(dog.isOwnedBy(owner) && dog.isOrderedToSit() && owner.getMainHandItem().isEmpty(),
                    "Using a bone should tame the dog and order it to sit");
            owner.setItemInHand(HAND, new ItemStack(Items.GLOW_BERRIES));
            click(owner, dog);
            h.assertTrue(dog.isInLove() && owner.getMainHandItem().isEmpty() && dog.isOrderedToSit(),
                    "Glow berries should feed the dog and preserve the sit order");
            var compass = new ItemStack(Items.COMPASS);
            owner.setItemInHand(HAND, compass);
            owner.setLastDeathLocation(Optional.empty());
            click(owner, dog);
            h.assertTrue(!dog.isDeathTracking() && dog.isOrderedToSit(), "No recorded death changed the sit order");
            owner.setLastDeathLocation(Optional.of(GlobalPos.of(owner.level().dimension(), dog.blockPosition().east(12))));
            click(owner, dog);
            h.assertTrue(dog.isDeathTracking() && !dog.isOrderedToSit() && owner.getMainHandItem() == compass,
                    "Compass interaction should start tracking without taking the compass");
            var parcel = parcel();
            owner.setItemInHand(HAND, parcel);
            click(owner, dog);
            h.assertTrue(dog.isDeathTracking() && !dog.isDelivering() && owner.getMainHandItem() == parcel,
                    "The bundle interrupted the active trail");
            owner.setItemInHand(HAND, ItemStack.EMPTY);
            click(owner, dog);
            h.assertTrue(!dog.isDeathTracking() && dog.isOrderedToSit(), "Empty hand should cancel tracking and sit");
        } finally {
            disconnect(owner);
        }
        h.succeed();
    }

    static void packAndCancel(GameTestHelper h) {
        var owner = h.makeMockServerPlayerInLevel();
        var recipient = h.makeMockServerPlayerInLevel();
        try {
            var dog = h.spawn(FloraFaunaRegistry.FRENCHIE.get(), 1, 1, 1);
            var expected = prepareDelivery(owner, recipient, dog);
            String target = recipient.getStringUUID();
            order(owner, dog, "send " + target);
            h.assertTrue(!dog.isDelivering() && owner.getMainHandItem() == expected, "Unsolicited command took cargo");
            click(owner, dog);
            order(owner, dog, "choose " + target);
            h.assertTrue(!dog.isDelivering() && owner.getMainHandItem() == expected, "Preview took cargo");
            owner.setItemInHand(HAND, new ItemStack(Items.BUNDLE));
            order(owner, dog, "send " + target);
            h.assertTrue(!dog.isDelivering() && owner.getMainHandItem().is(Items.BUNDLE),
                    "A confirmation for the full bundle accepted a different bundle");
            owner.setItemInHand(HAND, expected.copy());
            confirm(owner, recipient, dog);
            h.assertTrue(dog.isDelivering() && owner.getMainHandItem().isEmpty(), "Confirmation did not transfer custody");

            owner.setItemInHand(HAND, new ItemStack(Items.COMPASS));
            owner.setLastDeathLocation(Optional.of(GlobalPos.of(owner.level().dimension(), dog.blockPosition().north(7))));
            click(owner, dog);
            h.assertTrue(dog.isDelivering() && !dog.isDeathTracking(), "Compass interrupted the active delivery");

            owner.setItemInHand(HAND, ItemStack.EMPTY);
            owner.setShiftKeyDown(true);
            click(owner, dog);
            var pack = FloraFaunaRegistry.FRENCH_FRONTPACK_DATA;
            h.assertTrue(owner.getData(pack).hasCarriedFrenchie() && dog.isRemoved(), "Pickup left a second courier in the world");
            var release = BlockPos.containing(owner.position().add(0, 0, 2));
            h.getLevel().setBlockAndUpdate(release.below(), Blocks.STONE.defaultBlockState());
            h.getLevel().setBlockAndUpdate(release, Blocks.AIR.defaultBlockState());
            FrontpackEvents.putDown(owner);
            owner.setShiftKeyDown(false);
            var restored = (FrenchieEntity) h.getLevel().getEntity(dog.getUUID());
            h.assertTrue(restored != null && restored.isDelivering() && !owner.getData(pack).hasCarriedFrenchie(),
                    "Release lost the delivery or left a second copy in the pack");
            click(owner, restored);
            h.assertTrue(!restored.isDelivering() && restored.isOrderedToSit()
                            && ItemStack.isSameItemSameComponents(expected, owner.getMainHandItem()),
                    "Cancel after packing did not return the original parcel and sit");
            order(owner, restored, "send " + target);
            h.assertTrue(!restored.isDelivering() && owner.getInventory().countItem(Items.BUNDLE) == 1
                            && recipient.getInventory().countItem(Items.BUNDLE) == 0, "Repeated confirmation duplicated the parcel");
        } finally {
            disconnect(recipient);
            disconnect(owner);
        }
        h.succeed();
    }

    static void fullRecipient(GameTestHelper h) {
        var owner = h.makeMockServerPlayerInLevel();
        var recipient = h.makeMockServerPlayerInLevel();
        var dog = h.spawn(FloraFaunaRegistry.FRENCHIE.get(), 1, 1, 1);
        var expected = prepareDelivery(owner, recipient, dog).copy();
        recipient.snapTo(dog.position().add(1, 0, 0), 0, 0);
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            recipient.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        }
        confirm(owner, recipient, dog);
        h.runAfterDelay(24, () -> {
            h.assertTrue(dog.isDelivering() && recipient.getInventory().countItem(Items.BUNDLE) == 0,
                    "Full recipient inventory should leave the bundle on the dog");
            recipient.getInventory().setItem(7, ItemStack.EMPTY);
        });
        h.runAfterDelay(55, () -> {
            try {
                h.assertTrue(!dog.isDelivering() && owner.getInventory().countItem(Items.BUNDLE) == 0
                                && recipient.getInventory().countItem(Items.BUNDLE) == 1
                                && ItemStack.isSameItemSameComponents(expected, recipient.getInventory().getItem(7)),
                        "Freeing a slot should deliver the original bundle exactly once");
            } finally {
                disconnect(recipient);
                disconnect(owner);
            }
            h.succeed();
        });
    }

    static void disconnectedRecipient(GameTestHelper h) {
        var owner = h.makeMockServerPlayerInLevel();
        var recipient = h.makeMockServerPlayerInLevel();
        var dog = h.spawn(FloraFaunaRegistry.FRENCHIE.get(), 1, 1, 1);
        var expected = prepareDelivery(owner, recipient, dog).copy();
        confirm(owner, recipient, dog);
        disconnect(recipient);
        h.runAfterDelay(30, () -> {
            try {
                owner.snapTo(dog.position().add(1, 0, 0), 0, 0);
                if (dog.isDelivering()) click(owner, dog);
                h.assertTrue(owner.getInventory().countItem(Items.BUNDLE) == 1
                                && ItemStack.isSameItemSameComponents(expected, owner.getMainHandItem())
                                && !dog.isDelivering(), "Missing recipient lost the bundle or left a live delivery");
            } finally {
                disconnect(owner);
            }
            h.succeed();
        });
    }

    static void blockedRoute(GameTestHelper h) {
        // Build an enclosed recipient and a clear approach inside the rectangular pen.
        for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 6, 3, 9)) {
            h.setBlock(pos, pos.getY() == 0 ? Blocks.STONE : Blocks.AIR);
        }
        var cell = new BlockPos(3, 1, 7);
        for (BlockPos pos : BlockPos.betweenClosed(cell.offset(-1, 0, -1), cell.offset(1, 2, 1))) {
            if (pos.getY() == 3 || pos.getX() != cell.getX() || pos.getZ() != cell.getZ()) {
                h.setBlock(pos, Blocks.STONE);
            }
        }

        var owner = h.makeMockServerPlayerInLevel();
        var recipient = h.makeMockServerPlayerInLevel();
        var dog = h.spawn(FloraFaunaRegistry.FRENCHIE.get(), 3, 1, 1);
        var start = dog.position();
        var expected = prepareDelivery(owner, recipient, dog).copy();
        recipient.snapTo(h.absolutePos(cell), 0, 0);
        confirm(owner, recipient, dog);

        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(start.distanceToSqr(dog.position()) >= 4 && dog.isDelivering(),
                        "Dog never advanced toward the recipient"))
                .thenExecuteAfter(350, () -> {
                    try {
                        var level = h.getLevel();
                        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
                        dog.saveWithoutId(output);
                        var tag = output.buildResult();
                        h.assertTrue(dog.isDelivering() && tag.getBooleanOr("FrenchieReturning", false)
                                        && recipient.getInventory().countItem(Items.BUNDLE) == 0,
                                "A blocked route should time out and return with the parcel still on the dog");
                        var restored = FloraFaunaRegistry.FRENCHIE.get().create(level, EntitySpawnReason.LOAD);
                        h.assertTrue(restored != null, "Could not create restored courier");
                        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
                        dog.discard();
                        h.assertTrue(level.addFreshEntity(restored) && restored.isDelivering(), "Reload lost the returning courier");
                        owner.snapTo(restored.position().add(1, 0, 0), 0, 0);
                        click(owner, restored);
                        h.assertTrue(!restored.isDelivering() && restored.isOrderedToSit()
                                        && ItemStack.matches(expected, owner.getMainHandItem())
                                        && owner.getInventory().countItem(Items.BUNDLE) == 1,
                                "Could not recover the original parcel once after the failed route and reload");
                    } finally {
                        disconnect(recipient);
                        disconnect(owner);
                    }
                }).thenSucceed();
    }

    private static ItemStack prepareDelivery(ServerPlayer owner, ServerPlayer recipient, FrenchieEntity dog) {
        dog.tame(owner);
        dog.sitDown();
        owner.setGameMode(GameType.SURVIVAL);
        owner.snapTo(dog.position().add(0, 0, -1), 0, 0);
        recipient.snapTo(dog.position().add(2, 0, 0), 0, 0);
        owner.getInventory().clearContent();
        recipient.getInventory().clearContent();
        var parcel = parcel();
        owner.setItemInHand(HAND, parcel);
        return parcel;
    }

    private static ItemStack parcel() {
        var stack = new ItemStack(Items.BUNDLE);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Violet's delivery"));
        stack.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(new ItemStackTemplate(Items.AMETHYST_SHARD, 7))));
        return stack;
    }

    private static void confirm(ServerPlayer owner, ServerPlayer recipient, FrenchieEntity dog) {
        click(owner, dog);
        order(owner, dog, "choose " + recipient.getStringUUID());
        order(owner, dog, "send " + recipient.getStringUUID());
    }

    private static void click(ServerPlayer player, FrenchieEntity dog) {
        player.interactOn(dog, HAND, Vec3.ZERO);
    }

    private static void order(ServerPlayer owner, FrenchieEntity dog, String choice) {
        owner.level().getServer().getCommands().performPrefixedCommand(owner.createCommandSourceStack(),
                "frenchiedelivery " + dog.getStringUUID() + " " + choice);
    }

    private static void disconnect(ServerPlayer player) {
        player.level().getServer().getPlayerList().remove(player);
    }
}
