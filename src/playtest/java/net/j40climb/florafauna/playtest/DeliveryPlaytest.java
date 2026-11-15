package net.j40climb.florafauna.playtest;

import net.minecraft.world.phys.Vec3;
import net.j40climb.florafauna.common.entity.frenchie.DeliveryScreen;
import net.j40climb.florafauna.common.entity.frenchie.FrenchieEntity;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static net.j40climb.florafauna.playtest.CourierParcel.parcel;

final class DeliveryPlaytest {
    static final UUID DOG_ID = UUID.fromString("268d4bee-c265-442c-b07b-562099a21a79");
    private static final BlockPos START = new BlockPos(0, 240, 0);
    private static final BlockPos BED = START.east(6);
    private static final BlockPos DISPATCH = START.west(96);

    static void steps(Playtest t, int phase) {
        if (phase == 1) {
            t.server("Set out a named bundle, a Frenchie and an ordinary blue bed", DeliveryPlaytest::prepare);
            selectSlot(t, 1);
            t.awaitClient("Aim an empty hand at the blue bed", mc -> {
                mc.player.lookAt(Anchor.EYES, Vec3.atBottomCenterOf(BED).add(0, 0.5, 0));
                return mc.player.getMainHandItem().isEmpty() && mc.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(BED);
            });
            t.client("Right-click the bed to set the vanilla respawn point", mc -> KeyMapping.click(mc.options.keyUse.getKey()));
            t.awaitServer("The blue bed is the owner's respawn point", p -> p.getRespawnConfig() != null
                    && BED.equals(p.getRespawnConfig().respawnData().pos()) && !p.getRespawnConfig().forced());
            selectSlot(t, 0);
            t.server("Take the dog well outside the original delivery radius", p -> {
                dog(p).snapTo(DISPATCH, 0, 0);
                standBesideDog(p);
            });
            useDog(t, true);
            t.awaitClient("Escape the chooser without transferring the bundle", mc ->
                    mc.gui.screen() instanceof DeliveryScreen && mc.gui.screen().keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0)));
            useDog(t, true);
            openChooser(t);
            t.screenshot("delivery-choose-destination");
            chooseHome(t, 14);
            t.screenshot("delivery-confirm-destination");
            press(t, "confirm");
            t.awaitServer("Confirmation transfers the one filled bundle out of the owner's inventory", p ->
                    dog(p) != null && dog(p).isDelivering() && p.getInventory().countItem(Items.BUNDLE) == 0);
            t.awaitServer("Save with the courier off-world and the owner's hand empty", p ->
                    dog(p) == null && p.getInventory().countItem(Items.BUNDLE) == 0);
            return;
        }

        t.server("Rejoin and wait by the bed for the timed courier", p ->
                p.teleportTo(p.level(), BED.getX() + 0.5, BED.getY(), BED.getZ() + 2.5, Set.of(), 180, 15, true));
        t.awaitServer("The original dog finishes its saved trip with the parcel", p -> p.level().getEntity(DOG_ID)
                instanceof FrenchieEntity dog && dog.isOwnedBy(p) && dog.isDelivering()
                && dog.position().distanceToSqr(Vec3.atBottomCenterOf(BED)) <= 16
                && p.getInventory().countItem(Items.BUNDLE) == 0);
        t.server("Stand within reach of the waiting dog", DeliveryPlaytest::standBesideDog);
        awaitParcelIcon(t);
        t.screenshot("delivery-parcel-at-bed");
        useDog(t, false);
        t.awaitServer("Claim the same named bundle and all seven shards once after restart", DeliveryPlaytest::hasReturnedParcel);

        t.server("Bring the owner and sitting dog back to the far end of the path", p -> {
            dog(p).snapTo(START.west(8), 0, 0);
            standBesideDog(p);
        });
        useDog(t, true);
        openChooser(t);
        chooseHome(t, 8);
        press(t, "confirm");
        t.server("Stay in interaction range to cancel during the walk-off", DeliveryPlaytest::standBesideDog);
        useDog(t, false);
        t.awaitServer("Cancel the second trip and recover the entire bundle before arrival", DeliveryPlaytest::hasReturnedParcel);
        selectSlot(t, 1);
        useDog(t, false);
        t.awaitServer("The next empty-hand order resumes follow with exactly one parcel", p ->
                !dog(p).isOrderedToSit() && !dog(p).isDelivering() && p.getInventory().countItem(Items.BUNDLE) == 1);
        selectSlot(t, 0);
        useDog(t, true);
        openChooser(t);
        chooseHome(t, 8);
        press(t, "confirm");
        t.awaitServer("The Frenchie leaves with the only parcel", p -> dog(p) == null && p.getInventory().countItem(Items.BUNDLE) == 0);
        t.client("Use the owner's recall command while the dog is off-world", mc -> mc.player.connection.sendCommand("frenchie recall"));
        t.awaitServer("The recalled courier lands beside its sender with the bundle", p ->
                dog(p) != null && dog(p).isDelivering() && dog(p).distanceToSqr(p) <= 36 && p.getInventory().countItem(Items.BUNDLE) == 0);
        t.server("Stand beside the recalled courier", DeliveryPlaytest::standBesideDog);
        useDog(t, false);
        t.awaitServer("Collect the original bundle once after recall", DeliveryPlaytest::hasReturnedParcel);
        t.server("Put the parcel away and stand beside the dog with an empty inventory", p -> {
            p.getInventory().clearContent();
            dog(p).snapTo(DISPATCH, 0, 0);
            standBesideDog(p);
        });
        t.client("Send the dog home without a bundle", mc -> mc.player.connection.sendCommand("frenchie home"));
        t.awaitServer("The unladen Frenchie leaves its owner", p -> dog(p) == null && p.getInventory().isEmpty());
        t.awaitServer("The same dog waits at the respawn bed without a parcel", p -> dog(p) != null
                && dog(p).position().distanceToSqr(Vec3.atBottomCenterOf(BED)) <= 16 && dog(p).isOrderedToSit() && p.getInventory().isEmpty());
        t.client("Recall the Frenchie from its home bed", mc -> mc.player.connection.sendCommand("frenchie recall"));
        t.awaitServer("The unladen dog rejoins the sender and resumes follow", p -> dog(p) != null
                && dog(p).isOwnedBy(p) && !dog(p).isOrderedToSit() && dog(p).distanceToSqr(p) <= 36 && p.getInventory().isEmpty());
    }

    private static void selectSlot(Playtest t, int slot) {
        t.client("Select hotbar slot " + slot, mc -> {
            mc.player.getInventory().setSelectedSlot(slot);
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
        });
    }

    private static void prepare(ServerPlayer owner) {
        var level = owner.level().getServer().overworld();
        for (var pos : BlockPos.betweenClosed(DISPATCH.offset(-4, -1, -4), BED.offset(4, -1, 4))) {
            level.setBlockAndUpdate(pos, Blocks.SMOOTH_STONE.defaultBlockState());
        }
        var bed = Blocks.BED.blue().defaultBlockState().setValue(BedBlock.FACING, Direction.EAST);
        level.setBlock(BED.west(), bed.setValue(BedBlock.PART, BedPart.FOOT), Block.UPDATE_CLIENTS);
        level.setBlock(BED, bed.setValue(BedBlock.PART, BedPart.HEAD), Block.UPDATE_CLIENTS);
        level.getServer().clockManager().moveToTimeMarker(level.dimensionType().defaultClock().orElseThrow(), ClockTimeMarkers.NOON);
        owner.teleportTo(level, BED.getX() + 0.5, START.getY(), BED.getZ() + 2.5, Set.of(), 180, 15, true);
        owner.setGameMode(GameType.SURVIVAL);
        owner.getInventory().clearContent();
        owner.getInventory().setSelectedSlot(0);
        owner.setItemInHand(InteractionHand.MAIN_HAND, parcel());
        var dog = FloraFaunaRegistry.FRENCHIE.get().create(level, EntitySpawnReason.COMMAND);
        Playtest.require(dog != null, "Could not create courier");
        dog.setUUID(DOG_ID);
        dog.snapTo(START, 0, 0);
        dog.tame(owner);
        dog.sitDown();
        Playtest.require(level.addFreshEntity(dog), "Could not add courier");
    }

    private static void standBesideDog(ServerPlayer owner) {
        var dog = dog(owner);
        owner.teleportTo(owner.level(), dog.getX(), START.getY(), dog.getZ() + 1.8, Set.of(), 180, 15, true);
    }

    private static FrenchieEntity dog(ServerPlayer owner) {
        return (FrenchieEntity) owner.level().getEntity(DOG_ID);
    }

    private static boolean hasReturnedParcel(ServerPlayer owner) {
        var courier = dog(owner);
        return courier != null && courier.isOrderedToSit() && !courier.isDelivering() && owner.getInventory().countItem(Items.BUNDLE) == 1
                && ItemStack.matches(owner.getMainHandItem(), parcel());
    }

    private static void useDog(Playtest t, boolean withParcel) {
        t.awaitClient("Aim at the courier with " + (withParcel ? "the filled bundle" : "an empty hand"), mc -> {
            if (mc.gui.screen() != null || !(mc.level.getEntity(DOG_ID) instanceof FrenchieEntity dog)) return false;
            mc.player.lookAt(Anchor.EYES, dog.getEyePosition());
            return (withParcel ? ItemStack.matches(mc.player.getMainHandItem(), parcel()) : mc.player.getMainHandItem().isEmpty())
                    && mc.hitResult instanceof EntityHitResult hit && hit.getEntity() == dog;
        });
        t.client("Right-click the courier", mc -> KeyMapping.click(mc.options.keyUse.getKey()));
    }

    private static void openChooser(Playtest t) {
        t.awaitClient("Open the destination chooser with the parcel still in hand", mc ->
                mc.gui.screen() instanceof DeliveryScreen && mc.gui.screen().getTitle().equals(Component.translatable("frenchie.florafauna.delivery.title"))
                        && ItemStack.matches(mc.player.getMainHandItem(), parcel()));
    }

    private static void chooseHome(Playtest t, int expectedSeconds) {
        press(t, "home");
        t.awaitClient("Preview Your bed and the confirmation-to-arrival ETA with the bundle in hand", mc ->
                mc.gui.screen() instanceof DeliveryScreen && mc.gui.screen().getTitle().equals(Component.translatable("frenchie.florafauna.delivery.confirm_title"))
                        && ItemStack.matches(mc.player.getMainHandItem(), parcel())
                        && mc.gui.screen().getNarrationMessage().getString().contains(Component.translatable("frenchie.florafauna.delivery.home").getString())
                        && mc.gui.screen().getNarrationMessage().getString().contains(Component.translatable("dimension.minecraft.overworld").getString())
                        && mc.gui.screen().getNarrationMessage().getString().contains(eta(expectedSeconds))
                        && !mc.gui.screen().getNarrationMessage().getString().contains(BED.toShortString()));
    }

    static String eta(int seconds) {
        return Component.translatable("frenchie.florafauna.delivery.seconds", seconds).getString();
    }

    private static void press(Playtest t, String action, Object... args) {
        t.awaitClient("Select the " + action + " dialog button", mc -> {
            if (mc.gui.screen() == null) return false;
            String label = Component.translatable("frenchie.florafauna.delivery." + action, args).getString();
            return pressLabel(mc, label);
        });
    }

    static boolean pressLabel(Minecraft mc, String label) {
        if (!(mc.gui.screen() instanceof DeliveryScreen)) return false;
        Optional<Button> button = widgets(mc.gui.screen()).filter(Button.class::isInstance).map(Button.class::cast)
                .filter(b -> b.active && b.visible && b.getMessage().getString().equals(label)).findFirst();
        return button.isPresent() && button.get().keyPressed(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
    }

    private static Stream<AbstractWidget> widgets(GuiEventListener root) {
        Stream<AbstractWidget> widget = root instanceof AbstractWidget w ? Stream.of(w) : Stream.empty();
        if (root instanceof ContainerEventHandler container) {
            return Stream.concat(widget, container.children().stream().flatMap(DeliveryPlaytest::widgets));
        }
        return widget;
    }

    private static void awaitParcelIcon(Playtest t) {
        t.awaitClient("Track the waiting courier's parcel sprite", mc -> {
            if (!(mc.level.getEntity(DOG_ID) instanceof FrenchieEntity dog)) return false;
            mc.player.lookAt(Anchor.EYES, dog.getEyePosition());
            return CompanionIconsPlaytest.shows(mc, dog, "parcel");
        });
    }
}
