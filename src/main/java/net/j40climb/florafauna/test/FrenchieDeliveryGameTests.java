package net.j40climb.florafauna.test;

import net.j40climb.florafauna.common.entity.frenchie.FrenchieEntity;
import net.j40climb.florafauna.common.entity.frenchie.frontpack.FrontpackEvents;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
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
        var dog = h.spawn(FloraFaunaRegistry.FRENCHIE.get(), 3, 1, 2);
        var expected = prepareDelivery(owner, recipient, dog).copy();
        var id = dog.getUUID();
        var level = h.getLevel();
        var start = dog.position();
        platform(h, dog.blockPosition());
        // The landings are high above the departure point, with no walkable connection.
        BlockPos address = dog.blockPosition().above(96).south(12);
        BlockPos moved = address.south(12);
        platform(h, address);
        platform(h, moved);
        recipient.snapTo(address, 0, 0);
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            recipient.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        }
        confirm(owner, recipient, dog);
        h.assertTrue(dog.isDelivering() && owner.getMainHandItem().isEmpty(),
                "The chooser should accept a player 96 blocks away");
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(!dog.isRemoved() && dog.position().distanceToSqr(start) >= 1,
                        "The dog never walked away from the owner"))
                .thenWaitUntil(() -> h.assertTrue(level.getEntity(id) == null, "Courier never departed"))
                .thenExecute(() -> recipient.snapTo(moved, 0, 0))
                // Even after a short walk-off, this distance needs substantially more than the three-second base time.
                .thenExecuteFor(120, () -> h.assertTrue(level.getEntity(id) == null
                        && recipient.getInventory().countItem(Items.BUNDLE) == 0,
                        "Courier arrived before the distant destination's timer"))
                .thenWaitUntil(() -> h.assertTrue(level.getEntity(id) instanceof FrenchieEntity arrived
                                && arrived.isDelivering() && arrived.distanceToSqr(recipient) < 36,
                        "Original Frenchie did not arrive at the recipient's new position"))
                .thenExecuteAfter(25, () -> {
                    h.assertTrue(recipient.getInventory().countItem(Items.BUNDLE) == 0
                                    && level.getEntity(id) instanceof FrenchieEntity arrived && arrived.isDelivering(),
                            "A full inventory should leave the named parcel on the waiting dog");
                    recipient.getInventory().setItem(7, ItemStack.EMPTY);
                })
                .thenWaitUntil(() -> h.assertTrue(ItemStack.matches(recipient.getInventory().getItem(7), expected)
                                && recipient.getInventory().countItem(Items.BUNDLE) == 1,
                        "A free slot should receive the original filled bundle exactly once"))
                .thenWaitUntil(() -> h.assertTrue(level.getEntity(id) == null, "Courier never departed for home"))
                .thenExecuteFor(80, () -> h.assertTrue(level.getEntity(id) == null, "Return journey skipped the timer"))
                .thenWaitUntil(() -> h.assertTrue(level.getEntity(id) instanceof FrenchieEntity returned
                                && !returned.isDelivering() && returned.isOwnedBy(owner) && returned.distanceToSqr(owner) < 36,
                        "The same dog should return to its owner after the handoff"))
                .thenExecute(() -> {
                    h.assertTrue(recipient.getInventory().countItem(Items.BUNDLE) == 1
                                    && owner.getInventory().countItem(Items.BUNDLE) == 0,
                            "Returning the dog must not transfer the delivered bundle again");
                    disconnect(recipient);
                    disconnect(owner);
                }).thenSucceed();
    }

    static void disconnectedRecipient(GameTestHelper h) {
        var owner = h.makeMockServerPlayerInLevel();
        var recipient = h.makeMockServerPlayerInLevel();
        var dog = h.spawn(FloraFaunaRegistry.FRENCHIE.get(), 2, 1, 2);
        var expected = prepareDelivery(owner, recipient, dog).copy();
        var id = dog.getUUID();
        confirm(owner, recipient, dog);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(h.getLevel().getEntity(id) == null, "Courier never entered transit"))
                .thenExecute(() -> disconnect(recipient))
                .thenWaitUntil(() -> h.assertTrue(h.getLevel().getEntity(id) instanceof FrenchieEntity returned
                                && returned.isOwnedBy(owner) && returned.distanceToSqr(owner) < 36,
                        "A disconnect in transit should send the original courier back"))
                .thenExecute(() -> {
                    var returned = (FrenchieEntity) h.getLevel().getEntity(id);
                    if (returned.isDelivering()) click(owner, returned);
                    h.assertTrue(owner.getInventory().countItem(Items.BUNDLE) == 1
                                    && ItemStack.matches(owner.getMainHandItem(), expected)
                                    && !returned.isDelivering(),
                            "Missing recipient lost the parcel or duplicated the handoff");
                    disconnect(owner);
                }).thenSucceed();
    }

    static void blockedDeparture(GameTestHelper h) {
        // A glass enclosure lets the owner click from inside but prevents the departure walk.
        for (BlockPos pos : BlockPos.betweenClosed(1, 0, 1, 5, 3, 5)) {
            boolean wall = pos.getX() == 1 || pos.getX() == 5 || pos.getZ() == 1 || pos.getZ() == 5;
            h.setBlock(pos, wall || pos.getY() == 0 || pos.getY() == 3 ? Blocks.GLASS : Blocks.AIR);
        }
        var owner = h.makeMockServerPlayerInLevel();
        var recipient = h.makeMockServerPlayerInLevel();
        var dog = h.spawn(FloraFaunaRegistry.FRENCHIE.get(), 3, 1, 3);
        var expected = prepareDelivery(owner, recipient, dog).copy();
        var landing = dog.blockPosition().above(40);
        platform(h, landing);
        recipient.snapTo(landing, 0, 0);
        var id = dog.getUUID();
        confirm(owner, recipient, dog);
        h.runAfterDelay(100, () -> h.assertTrue(h.getLevel().getEntity(id) == null,
                "An enclosed dog must be able to leave without finding a route"));
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(ItemStack.matches(recipient.getMainHandItem(), expected),
                        "Courier never arrived across the blocked departure route"))
                .thenWaitUntil(() -> h.assertTrue(h.getLevel().getEntity(id) instanceof FrenchieEntity returned
                                && !returned.isDelivering() && returned.distanceToSqr(owner) < 36,
                        "Courier never returned after completing its blocked-route delivery"))
                .thenExecute(() -> {
                    h.assertTrue(recipient.getInventory().countItem(Items.BUNDLE) == 1
                                    && owner.getInventory().countItem(Items.BUNDLE) == 0,
                            "Blocked departure must produce one completed delivery");
                    disconnect(recipient);
                    disconnect(owner);
                }).thenSucceed();
    }

    static void unloadedHomeBed(GameTestHelper h) {
        var level = h.getLevel();
        var owner = h.makeMockServerPlayerInLevel();
        var dog = h.spawn(FloraFaunaRegistry.FRENCHIE.get(), 2, 1, 2);
        var id = dog.getUUID();
        BlockPos start = dog.blockPosition();
        // Keep both the bed and its landing floor in a previously untouched chunk.
        BlockPos bed = ChunkPos.containing(start.east(640)).getMiddleBlockPosition(start.getY() + 12);
        platform(h, bed);
        var bedState = Blocks.BLUE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.EAST);
        level.setBlock(bed.west(), bedState.setValue(BedBlock.PART, BedPart.FOOT), Block.UPDATE_CLIENTS);
        level.setBlock(bed, bedState.setValue(BedBlock.PART, BedPart.HEAD), Block.UPDATE_CLIENTS);
        owner.getInventory().clearContent();
        owner.setItemInHand(HAND, parcel());
        dog.tame(owner);
        dog.sitDown();
        dog.snapTo(bed.north(), 0, 0);
        owner.snapTo(dog.position().add(1, 0, 0), 0, 0);
        click(owner, dog);
        order(owner, dog, "bed");
        dog.snapTo(start, 0, 0);
        owner.snapTo(dog.position().add(0, 0, -1), 0, 0);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(!level.hasChunkAt(bed), "Bed chunk never unloaded"))
                .thenExecute(() -> {
                    click(owner, dog);
                    order(owner, dog, "choose home");
                    order(owner, dog, "send home");
                    h.assertTrue(dog.isDelivering() && owner.getMainHandItem().isEmpty(),
                            "The owner could not confirm an unloaded bed");
                })
                .thenWaitUntil(() -> h.assertTrue(level.getEntity(id) == null, "Bed delivery never departed"))
                .thenExecuteAfter(200, () -> h.assertTrue(!level.hasChunkAt(bed) && level.getEntity(id) == null,
                        "The dog or destination chunk was simulated during long-distance transit"))
                .thenExecuteAfter(800, () -> {
                    // The owner visits after arrival, when the bed and waiting courier may have unloaded again.
                    level.getChunkSource().addTicketWithRadius(TicketType.PLAYER_SPAWN, ChunkPos.containing(bed), 2);
                    level.getChunkAt(bed);
                    owner.snapTo(bed.north(2), 0, 0);
                })
                .thenWaitUntil(() -> h.assertTrue(level.getEntity(id) instanceof FrenchieEntity waiting
                                && waiting.isDelivering() && waiting.position().distanceToSqr(bed.getBottomCenter()) < 36,
                        "No waiting courier at the distant bed after the owner's visit"))
                .thenExecute(() -> {
                    var waiting = (FrenchieEntity) level.getEntity(id);
                    owner.snapTo(waiting.position().add(1, 0, 0), 0, 0);
                    click(owner, waiting);
                    h.assertTrue(!waiting.isDelivering() && waiting.isOrderedToSit()
                                    && ItemStack.matches(parcel(), owner.getMainHandItem())
                                    && owner.getInventory().countItem(Items.BUNDLE) == 1,
                            "The distant-bed delivery should return the original parcel once");
                    level.getChunkSource().removeTicketWithRadius(TicketType.PLAYER_SPAWN, ChunkPos.containing(bed), 2);
                    disconnect(owner);
                }).thenSucceed();
    }

    static void legacyBedPickup(GameTestHelper h) {
        var owner = h.makeMockServerPlayerInLevel();
        try {
            var level = h.getLevel();
            var dog = h.spawn(FloraFaunaRegistry.FRENCHIE.get(), 3, 1, 2);
            owner.getInventory().clearContent();
            dog.tame(owner);
            dog.standUp();
            var id = dog.getUUID();
            BlockPos bed = dog.blockPosition().east();
            var bedState = Blocks.BLUE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.EAST);
            level.setBlock(bed.west(), bedState.setValue(BedBlock.PART, BedPart.FOOT), Block.UPDATE_CLIENTS);
            level.setBlock(bed, bedState.setValue(BedBlock.PART, BedPart.HEAD), Block.UPDATE_CLIENTS);
            dog.snapTo(bed.north(), 0, 0);
            var expected = parcel();
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            dog.saveWithoutId(output);
            output.store("FrenchieHomeBed", GlobalPos.CODEC, GlobalPos.of(level.dimension(), bed));
            output.store("FrenchieParcel", ItemStack.CODEC, expected);
            output.putBoolean("FrenchieAtHandoff", true);
            var restored = FloraFaunaRegistry.FRENCHIE.get().create(level, EntitySpawnReason.LOAD);
            h.assertTrue(restored != null, "Could not create legacy courier");
            dog.discard();
            restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
            h.assertTrue(level.addFreshEntity(restored) && id.equals(restored.getUUID()) && restored.isDelivering(),
                    "Loading a pre-timer bed delivery lost the courier or bundle");
            owner.snapTo(restored.position().add(0, 0, -1), 0, 0);
            click(owner, restored);
            h.assertTrue(!restored.isDelivering() && restored.isOrderedToSit()
                            && ItemStack.matches(expected, owner.getMainHandItem()) && owner.getInventory().countItem(Items.BUNDLE) == 1,
                    "The owner should still collect exactly one legacy parcel from the waiting dog");
        } finally {
            disconnect(owner);
        }
        h.succeed();
    }

    private static void platform(GameTestHelper h, BlockPos feet) {
        for (var pos : BlockPos.betweenClosed(feet.offset(-3, -1, -3), feet.offset(3, -1, 3))) {
            h.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        }
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
