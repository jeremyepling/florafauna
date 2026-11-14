package net.j40climb.florafauna.playtest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

final class DeliveryPlaytest {
    private static final UUID DOG_ID = UUID.fromString("268d4bee-c265-442c-b07b-562099a21a79");
    private static final UUID RECIPIENT_ID = UUID.fromString("98368ad4-5a18-4b67-90e6-ce57e2d76d87");
    private static final String RECIPIENT = "Amelie";
    private static final BlockPos START = new BlockPos(0, 240, 0);
    private static final BlockPos BED = START.east(6);
    private static final BlockPos DISPATCH = START.west(96);

    static void steps(Playtest t, int phase) {
        if (phase == 1) {
            t.server("Set out a named bundle, a Frenchie and an ordinary blue bed", DeliveryPlaytest::prepare);
            useDog(t, true);
            press(t, "assign", BED.toShortString());
            t.awaitClient("Close the assignment dialog with the bundle still in hand", mc ->
                    mc.screen == null && ItemStack.matches(parcel(), mc.player.getMainHandItem()));
            t.server("Take the dog well outside the original delivery radius", p -> {
                dog(p).snapTo(DISPATCH, 0, 0);
                standBesideDog(p);
            });
            useDog(t, true);
            t.awaitClient("Escape the chooser without transferring the bundle", mc ->
                    mc.screen instanceof DeliveryScreen && mc.screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_ESCAPE, 0, 0)));
            useDog(t, true);
            openChooser(t);
            t.screenshot("delivery-choose-destination");
            chooseHome(t);
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
                && dog.position().distanceToSqr(BED.getBottomCenter()) <= 16
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
        chooseHome(t);
        press(t, "confirm");
        t.server("Stay in interaction range to cancel during the walk-off", DeliveryPlaytest::standBesideDog);
        useDog(t, false);
        t.awaitServer("Cancel the second trip and recover the entire bundle before arrival", DeliveryPlaytest::hasReturnedParcel);
        selectSlot(t, 1);
        useDog(t, false);
        t.awaitServer("The next empty-hand order resumes follow with exactly one parcel", p ->
                !dog(p).isOrderedToSit() && !dog(p).isDelivering() && p.getInventory().countItem(Items.BUNDLE) == 1);
        t.server("Connect an online recipient 88 blocks from the sender", DeliveryPlaytest::connectRecipient);
        selectSlot(t, 0);
        useDog(t, true);
        openChooser(t);
        t.screenshot("delivery-choose-player");
        press(t, "send_player", RECIPIENT);
        t.awaitClient("Preview the online recipient with the bundle still in hand", mc ->
                mc.screen instanceof DeliveryScreen && mc.screen.getNarrationMessage().getString().contains(RECIPIENT)
                        && ItemStack.matches(mc.player.getMainHandItem(), parcel()));
        t.screenshot("delivery-confirm-player");
        press(t, "confirm");
        t.awaitServer("The chosen player receives the original filled bundle", p -> {
            var recipient = p.level().getServer().getPlayerList().getPlayer(RECIPIENT_ID);
            return ItemStack.matches(recipient.getMainHandItem(), parcel()) && recipient.getInventory().countItem(Items.BUNDLE) == 1;
        });
        t.awaitServer("The same Frenchie returns to the sender after the timed round trip", p ->
                dog(p) != null && dog(p).isOwnedBy(p) && !dog(p).isDelivering() && dog(p).distanceToSqr(p) <= 36
                        && p.getInventory().countItem(Items.BUNDLE) == 0);
        t.server("Disconnect the test recipient", p -> {
            var list = p.level().getServer().getPlayerList();
            list.remove(list.getPlayer(RECIPIENT_ID));
        });
    }

    private static void selectSlot(Playtest t, int slot) {
        t.client("Select hotbar slot " + slot, mc -> {
            mc.player.getInventory().setSelectedSlot(slot);
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
        });
    }

    private static void connectRecipient(ServerPlayer owner) {
        var server = owner.level().getServer();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(RECIPIENT_ID, RECIPIENT), false);
        var recipient = new ServerPlayer(server, owner.level(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, recipient, cookie);
        recipient.setGameMode(GameType.SURVIVAL);
        recipient.getInventory().clearContent();
        recipient.teleportTo(owner.level(), DISPATCH.getX() + 0.5, DISPATCH.getY(), DISPATCH.getZ() + 0.5, Set.of(), 0, 0, true);
    }

    private static void prepare(ServerPlayer owner) {
        var level = owner.level().getServer().overworld();
        for (var pos : BlockPos.betweenClosed(DISPATCH.offset(-4, -1, -4), BED.offset(4, -1, 4))) {
            level.setBlockAndUpdate(pos, Blocks.SMOOTH_STONE.defaultBlockState());
        }
        var bed = Blocks.BLUE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.EAST);
        level.setBlockAndUpdate(BED.west(), bed.setValue(BedBlock.PART, BedPart.FOOT));
        level.setBlockAndUpdate(BED, bed.setValue(BedBlock.PART, BedPart.HEAD));
        owner.teleportTo(level, START.getX() + 0.5, START.getY(), START.getZ() + 2.5, Set.of(), 180, 15, true);
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

    private static ItemStack parcel() {
        var stack = new ItemStack(Items.BUNDLE);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Violet's delivery"));
        stack.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(new ItemStackTemplate(Items.AMETHYST_SHARD, 7))));
        return stack;
    }

    private static boolean hasReturnedParcel(ServerPlayer owner) {
        var courier = dog(owner);
        return courier != null && courier.isOrderedToSit() && !courier.isDelivering() && owner.getInventory().countItem(Items.BUNDLE) == 1
                && ItemStack.matches(owner.getMainHandItem(), parcel());
    }

    private static void useDog(Playtest t, boolean withParcel) {
        t.awaitClient("Aim at the courier with " + (withParcel ? "the filled bundle" : "an empty hand"), mc -> {
            if (mc.screen != null || !(mc.level.getEntity(DOG_ID) instanceof FrenchieEntity dog)) return false;
            mc.player.lookAt(Anchor.EYES, dog.getEyePosition());
            return (withParcel ? ItemStack.matches(mc.player.getMainHandItem(), parcel()) : mc.player.getMainHandItem().isEmpty())
                    && mc.hitResult instanceof EntityHitResult hit && hit.getEntity() == dog;
        });
        t.client("Right-click the courier", mc -> KeyMapping.click(mc.options.keyUse.getKey()));
    }

    private static void openChooser(Playtest t) {
        t.awaitClient("Open the destination chooser with the parcel still in hand", mc ->
                mc.screen instanceof DeliveryScreen && mc.screen.getTitle().equals(Component.translatable("frenchie.florafauna.delivery.title"))
                        && ItemStack.matches(mc.player.getMainHandItem(), parcel()));
    }

    private static void chooseHome(Playtest t) {
        press(t, "send_home", BED.toShortString());
        t.awaitClient("Preview the assigned bed's coordinates before giving up the bundle", mc ->
                mc.screen instanceof DeliveryScreen && mc.screen.getTitle().equals(Component.translatable("frenchie.florafauna.delivery.confirm_title"))
                        && ItemStack.matches(mc.player.getMainHandItem(), parcel())
                        && mc.screen.getNarrationMessage().getString().contains(BED.toShortString()));
    }

    private static void press(Playtest t, String action, Object... args) {
        t.awaitClient("Select the " + action + " dialog button", mc -> {
            if (mc.screen == null) return false;
            String label = Component.translatable("frenchie.florafauna.delivery." + action, args).getString();
            Optional<Button> button = widgets(mc.screen).filter(Button.class::isInstance).map(Button.class::cast)
                    .filter(b -> b.active && b.visible && b.getMessage().getString().equals(label)).findFirst();
            return button.isPresent() && button.get().keyPressed(new KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0));
        });
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
