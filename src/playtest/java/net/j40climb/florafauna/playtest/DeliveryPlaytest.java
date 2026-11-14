package net.j40climb.florafauna.playtest;

import net.j40climb.florafauna.common.entity.frenchie.FrenchieEntity;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.dialog.MultiButtonDialogScreen;
import net.minecraft.client.gui.screens.dialog.SimpleDialogScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
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
    private static final BlockPos START = new BlockPos(0, 240, 0);
    private static final BlockPos BED = START.east(6);

    static void steps(Playtest t, int phase) {
        if (phase == 1) {
            t.server("Set out a named bundle, a Frenchie and an ordinary blue bed", DeliveryPlaytest::prepare);
            useDog(t, true);
            press(t, "assign", BED.toShortString());
            t.awaitClient("Close the assignment dialog with the bundle still in hand", mc ->
                    mc.screen == null && ItemStack.matches(parcel(), mc.player.getMainHandItem()));
            useDog(t, true);
            chooseHome(t);
            t.screenshot("delivery-confirm-destination");
            press(t, "confirm");
            t.awaitServer("Confirmation transfers the one filled bundle out of the owner's inventory", p ->
                    dog(p).isDelivering() && p.getInventory().countItem(Items.BUNDLE) == 0);
            t.awaitServer("Let the courier walk from the owner to the bed", p ->
                    dog(p).position().distanceToSqr(BED.getBottomCenter()) <= 6.25 && dog(p).getNavigation().isDone());
            t.server("Stand in reach of the parcel at the bed", DeliveryPlaytest::standBesideDog);
            awaitParcelIcon(t);
            t.screenshot("delivery-parcel-at-bed");
            return;
        }

        t.awaitServer("Rejoin with the original dog at the bed and no parcel in the inventory", p -> p.level().getEntity(DOG_ID)
                instanceof FrenchieEntity dog && dog.isOwnedBy(p) && dog.isDelivering()
                && dog.position().distanceToSqr(BED.getBottomCenter()) <= 6.25
                && p.getInventory().countItem(Items.BUNDLE) == 0);
        awaitParcelIcon(t);
        useDog(t, false);
        t.awaitServer("Claim the same named bundle and all seven shards once after restart", DeliveryPlaytest::hasReturnedParcel);

        t.server("Bring the owner and sitting dog back to the far end of the path", p -> {
            dog(p).snapTo(START.west(8), 0, 0);
            standBesideDog(p);
        });
        useDog(t, true);
        chooseHome(t);
        press(t, "confirm");
        useDog(t, false);
        t.awaitServer("Cancel the second trip and recover the entire bundle before arrival", DeliveryPlaytest::hasReturnedParcel);
        t.client("Select an empty slot after cancellation", mc -> {
            mc.player.getInventory().setSelectedSlot(1);
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(1));
        });
        useDog(t, false);
        t.awaitServer("The next empty-hand order resumes follow with exactly one parcel", p ->
                !dog(p).isOrderedToSit() && !dog(p).isDelivering() && p.getInventory().countItem(Items.BUNDLE) == 1);
    }

    private static void prepare(ServerPlayer owner) {
        var level = owner.level().getServer().overworld();
        for (var pos : BlockPos.betweenClosed(START.offset(-12, -1, -3), BED.offset(3, -1, 3))) {
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
        return dog(owner).isOrderedToSit() && !dog(owner).isDelivering() && owner.getInventory().countItem(Items.BUNDLE) == 1
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

    private static void chooseHome(Playtest t) {
        t.awaitClient("Open the destination chooser with the parcel still in hand", mc ->
                mc.screen instanceof MultiButtonDialogScreen && ItemStack.matches(mc.player.getMainHandItem(), parcel()));
        press(t, "send_home", BED.toShortString());
        t.awaitClient("Preview the assigned bed's coordinates before giving up the bundle", mc ->
                mc.screen instanceof SimpleDialogScreen<?> && ItemStack.matches(mc.player.getMainHandItem(), parcel())
                        && widgets(mc.screen).anyMatch(w -> w.getMessage().getString().contains(BED.toShortString())));
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
