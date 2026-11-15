package net.j40climb.florafauna.playtest;

import com.mojang.logging.LogUtils;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.entity.frenchie.FrenchieEntity;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.DEDICATED_SERVER)
public final class DeliveryMultiplayerServer {
    static final UUID DOG = UUID.fromString("c2033ce5-c816-4705-933c-1d71b3b728c0");
    static final String SEND = "Courier playtest: open chooser";
    static final String ACCEPT = "Courier playtest: receive bundle";
    static final String DONE = "Courier playtest: disconnect";
    static final int ETA = 13; // 96 blocks at 16 blocks per second + 3 seconds travel + 4 seconds walk-off
    private static final BlockPos START = new BlockPos(0, 200, 0);
    private static final BlockPos END = START.east(96);
    private enum Step { JOIN, CONFIRM, DEPART, LAND, ACCEPT, RETURN, EXIT }
    private static Step step = Step.JOIN;
    private static Path root;
    private static long confirmTick;
    private static boolean recipientClicked;

    @SubscribeEvent
    public static void start(ServerStartedEvent event) throws IOException {
        String path = System.getProperty("florafauna.deliveryTest.root");
        if (path == null) return;
        root = Path.of(path).toRealPath();
        require(event.getServer().getWorldPath(LevelResource.ROOT).toRealPath().equals(root.resolve("server/world")), "Wrong server world");
        Files.writeString(root.resolve("ready-1"), "ready");
    }

    @SubscribeEvent
    public static void click(PlayerInteractEvent.EntityInteract event) {
        if (root != null && step == Step.ACCEPT && event.getTarget().getUUID().equals(DOG)
                && event.getEntity().getGameProfile().name().equals("FFVisitor")) recipientClicked = true;
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) throws IOException {
        if (root == null) return;
        var server = event.getServer();
        if (step == Step.EXIT) {
            if (server.getPlayerList().getPlayerCount() == 0) server.halt(false);
            return;
        }
        var owner = server.getPlayerList().getPlayerByName("FFOwner");
        var recipient = server.getPlayerList().getPlayerByName("FFVisitor");
        if (owner == null || recipient == null || !owner.connection.hasClientLoaded() || !recipient.connection.hasClientLoaded()) return;
        try {
            advance(owner, recipient);
        } catch (AssertionError | RuntimeException e) {
            LogUtils.getLogger().error("Delivery failed during {}", step, e);
            finish(owner, recipient, "FAIL " + step + ": " + e);
        }
    }

    private static void advance(ServerPlayer owner, ServerPlayer recipient) throws IOException {
        var level = owner.level();
        var dog = (FrenchieEntity) level.getEntity(DOG);
        switch (step) {
            case JOIN -> {
                require(!owner.getUUID().equals(recipient.getUUID()), "Distinct client identities required");
                level.getServer().setDifficulty(Difficulty.PEACEFUL, true);
                level.getServer().clockManager().moveToTimeMarker(level.dimensionType().defaultClock().orElseThrow(), ClockTimeMarkers.NOON);
                for (var point : BlockPos.betweenClosed(START.offset(-4, -1, -4), END.offset(4, -1, 4))) {
                    level.setBlockAndUpdate(point, Blocks.SMOOTH_STONE.defaultBlockState());
                }
                prepare(owner, START.south(2));
                prepare(recipient, END);
                owner.setItemInHand(InteractionHand.MAIN_HAND, CourierParcel.parcel());
                dog = FloraFaunaRegistry.FRENCHIE.get().create(level, EntitySpawnReason.COMMAND);
                require(dog != null, "Frenchie creation failed");
                dog.setUUID(DOG);
                dog.tame(owner);
                dog.sitDown();
                dog.setCustomName(Component.literal("Violet"));
                dog.snapTo(START, 0, 0);
                require(level.addFreshEntity(dog), "Frenchie spawn failed");
                owner.sendSystemMessage(Component.literal(SEND));
                step = Step.CONFIRM;
            }
            case CONFIRM -> {
                if (dog == null || !dog.isDelivering()) return;
                require(owner.getInventory().isEmpty() && recipient.getInventory().isEmpty(), "Transfer before confirmed arrival");
                confirmTick = level.getGameTime();
                step = Step.DEPART;
            }
            case DEPART -> {
                if (dog == null) step = Step.LAND;
            }
            case LAND -> {
                if (dog == null || dog.distanceToSqr(recipient) > 16) return;
                long elapsed = level.getGameTime() - confirmTick;
                require(elapsed >= ETA * 20 - 1 && elapsed <= ETA * 20 + 20, "Expected " + ETA + " s from confirmation, got " + elapsed + " ticks");
                require(dog.isDelivering() && dog.isOwnedBy(owner) && recipient.getInventory().isEmpty(), "Expected a visible courier holding the parcel");
                recipient.sendSystemMessage(Component.literal(ACCEPT));
                step = Step.ACCEPT;
            }
            case ACCEPT -> {
                if (!recipientClicked) {
                    require(recipient.getInventory().isEmpty(), "Parcel transferred without a recipient click");
                    return;
                }
                if (recipient.getInventory().countItem(Items.BUNDLE) == 0) return;
                require(ItemStack.matches(recipient.getMainHandItem(), CourierParcel.parcel()), "The recipient's parcel changed");
                step = Step.RETURN;
            }
            case RETURN -> {
                if (dog == null || dog.isDelivering() || dog.distanceToSqr(owner) > 36) return;
                require(dog.isOwnedBy(owner) && owner.getInventory().isEmpty(), "The courier or sender's inventory changed");
                require(recipient.getInventory().countItem(Items.BUNDLE) == 1
                        && ItemStack.matches(recipient.getMainHandItem(), CourierParcel.parcel()), "Handoff lost or duplicated the bundle");
                require(Files.isRegularFile(root.resolve("Visitor/screenshots/courier-arrival.png")), "Missing recipient's screenshot");
                finish(owner, recipient, "PASS");
            }
        }
    }

    private static void prepare(ServerPlayer player, BlockPos pos) {
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.getInventory().setSelectedSlot(0);
        player.teleportTo(player.level(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Set.of(), 180, 15, true);
    }

    private static void finish(ServerPlayer owner, ServerPlayer recipient, String result) throws IOException {
        Files.writeString(root.resolve("phase-1.result"), result);
        owner.sendSystemMessage(Component.literal(DONE));
        recipient.sendSystemMessage(Component.literal(DONE));
        step = Step.EXIT;
    }

    private static void require(boolean condition, String details) {
        if (!condition) throw new AssertionError(details);
    }
}
