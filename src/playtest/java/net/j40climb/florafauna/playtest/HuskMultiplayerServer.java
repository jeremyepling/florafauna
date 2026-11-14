package net.j40climb.florafauna.playtest;

import com.mojang.logging.LogUtils;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.husk.HuskBlock;
import net.j40climb.florafauna.common.block.husk.HuskBlockEntity;
import net.j40climb.florafauna.common.block.husk.HuskType;
import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.common.symbiote.data.SymbioteState;
import net.j40climb.florafauna.dev.DevPlot;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
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
public final class HuskMultiplayerServer {
    static final BlockPos HUSK = new BlockPos(0, 200, 8);
    static final String CLICK = "FloraFauna playtest: click recovery husk";
    static final String DONE = "FloraFauna playtest: disconnect";
    private static final int SHARD_SLOT = 4;
    private static final BlockPos BARREL = new BlockPos(1, 200, -4);
    private enum Step { JOIN, RESPAWN, VISITOR, FULL, PARTIAL, REJOIN_VISITOR, FINISH, REPEAT_OWNER, REPEAT_VISITOR, EXIT }
    private static Step step = Step.JOIN;
    private static Path root;
    private static int phase;
    private static UUID pendingClick;
    private static boolean sawClick;

    @SubscribeEvent
    public static void start(ServerStartedEvent event) throws IOException {
        String path = System.getProperty("florafauna.huskTest.root");
        if (path == null) return;
        Path directory = Path.of(path).toRealPath();
        require(event.getServer().getWorldPath(LevelResource.ROOT).toRealPath()
                        .equals(directory.resolve("server/world")), "Unexpected dedicated test world");
        phase = Integer.parseInt(Files.readString(directory.resolve("phase")).trim());
        require(phase == 1 || phase == 2, "Invalid test phase");
        root = directory;
        Files.writeString(root.resolve("ready-" + phase), "ready");
    }

    @SubscribeEvent
    public static void onClick(PlayerInteractEvent.RightClickBlock event) {
        if (root != null && HUSK.equals(event.getPos()) && event.getEntity().getUUID().equals(pendingClick)) {
            sawClick = true;
        }
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) throws IOException {
        if (root == null) return;
        MinecraftServer server = event.getServer();
        if (step == Step.EXIT) {
            if (server.getPlayerList().getPlayerCount() == 0) server.halt(false);
            return;
        }
        ServerPlayer owner = server.getPlayerList().getPlayerByName("FFOwner");
        ServerPlayer visitor = server.getPlayerList().getPlayerByName("FFVisitor");
        if (owner == null || visitor == null || !owner.connection.hasClientLoaded() || !visitor.connection.hasClientLoaded()) return;
        if (pendingClick != null) {
            if (!sawClick) return;
            pendingClick = null;
            sawClick = false;
        }
        try {
            advance(server, owner, visitor);
        } catch (RuntimeException | AssertionError e) {
            LogUtils.getLogger().error("Multiplayer recovery failed at {}", step, e);
            finish(owner, visitor, "FAIL " + step + ": " + e);
        }
    }

    private static void advance(MinecraftServer server, ServerPlayer owner, ServerPlayer visitor) throws IOException {
        switch (step) {
            case JOIN -> {
                require(!owner.getUUID().equals(visitor.getUUID()), "Both connections used the same UUID");
                if (phase == 2) {
                    check(owner, visitor, 64, 16);
                    require(data(owner).symbioteState() == SymbioteState.BONDED_ACTIVE, "Restored bond lost on restart");
                    requestClick(visitor, Step.REJOIN_VISITOR);
                    return;
                }
                DevPlot.RECOVERY.place(owner.level(), new BlockPos(-3, 199, -5));
                owner.level().getGameRules().set(GameRules.KEEP_INVENTORY, false, server);
                owner.setGameMode(GameType.SURVIVAL);
                visitor.setGameMode(GameType.SURVIVAL);
                move(owner, HUSK.getZ() + 0.5);
                move(visitor, HUSK.getZ() - 1.5);
                owner.getInventory().clearContent();
                visitor.getInventory().clearContent();
                owner.setData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA, PlayerSymbioteData.DEFAULT.withSymbioteState(SymbioteState.BONDED_ACTIVE));
                owner.getInventory().setItem(SHARD_SLOT, barrel(owner).removeItem(2, 17));
                owner.kill(owner.level());
                step = Step.RESPAWN;
            }
            case RESPAWN -> {
                if (!owner.isAlive()) return;
                require(HUSK.equals(data(owner).restorationHuskPos()) && owner.getInventory().isEmpty(), "Death/respawn lost recovery state");
                move(owner, HUSK.getZ() - 1.5);
                check(owner, visitor, 0, 17);
                requestClick(visitor, Step.VISITOR);
            }
            case VISITOR -> {
                check(owner, visitor, 0, 17);
                require(data(owner).symbioteState() == SymbioteState.BONDED_WEAKENED, "Visitor restored owner's bond");
                BarrelBlockEntity kit = barrel(owner);
                for (int i = 1; i < Inventory.INVENTORY_SIZE; i++) {
                    ItemStack filler = kit.removeItem(10, 1);
                    require(filler.is(Items.COBBLESTONE), "Missing recovery kit filler");
                    owner.getInventory().setItem(i, filler);
                }
                owner.getInventory().setItem(0, kit.removeItem(9, 64));
                owner.getInventory().setSelectedSlot(0);
                requestClick(owner, Step.FULL);
            }
            case FULL -> {
                check(owner, visitor, 64, 17);
                require(data(owner).symbioteState() == SymbioteState.BONDED_ACTIVE && !data(owner).restorationHuskActive(),
                        "Full inventory blocked bond restoration");
                barrel(owner).setItem(9, owner.getInventory().removeItem(0, 1));
                requestClick(owner, Step.PARTIAL);
            }
            case PARTIAL -> {
                check(owner, visitor, 64, 16);
                finish(owner, visitor, "PASS");
            }
            case REJOIN_VISITOR -> {
                check(owner, visitor, 64, 16);
                require(owner.getInventory().getItem(1).is(Items.COBBLESTONE), "Expected occupied destination after restart");
                barrel(owner).setItem(10, owner.getInventory().removeItem(1, 1));
                requestClick(owner, Step.FINISH);
            }
            case FINISH -> {
                check(owner, visitor, 80, 0);
                requestClick(owner, Step.REPEAT_OWNER);
            }
            case REPEAT_OWNER -> {
                check(owner, visitor, 80, 0);
                requestClick(visitor, Step.REPEAT_VISITOR);
            }
            case REPEAT_VISITOR -> {
                check(owner, visitor, 80, 0);
                finish(owner, visitor, "PASS");
            }
        }
    }

    private static PlayerSymbioteData data(ServerPlayer player) {
        return player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
    }

    private static BarrelBlockEntity barrel(ServerPlayer player) {
        return (BarrelBlockEntity) player.level().getBlockEntity(BARREL);
    }

    private static void move(ServerPlayer player, double z) {
        require(player.teleportTo(player.level(), 0.5, 200, z, Set.of(), 0, 0, true), "Could not reach the recovery plot");
    }

    private static void requestClick(ServerPlayer player, Step next) {
        step = next;
        pendingClick = player.getUUID();
        LogUtils.getLogger().info("Requesting {} interaction from {}", next, player.getGameProfile().name());
        player.sendSystemMessage(Component.literal(CLICK));
    }

    private static void check(ServerPlayer owner, ServerPlayer visitor, int inventory, int stored) {
        var level = owner.level();
        require(level.getBlockEntity(HUSK) instanceof HuskBlockEntity, "Missing husk on recovery plot");
        HuskBlockEntity husk = (HuskBlockEntity) level.getBlockEntity(HUSK);
        require(husk.isOwner(owner) && !husk.isOwner(visitor), "Wrong husk owner");
        require(owner.getInventory().countItem(Items.AMETHYST_SHARD) == inventory, "Owner should have " + inventory + " shards");
        require(inventory + stored + barrel(owner).countItem(Items.AMETHYST_SHARD) == 81, "Expected all 81 shards across owner, husk and kit");
        require(visitor.getInventory().isEmpty(), "Visitor received items");
        for (int slot = 0; slot < HuskBlockEntity.TOTAL_SLOTS; slot++) {
            int expected = slot == SHARD_SLOT ? stored : 0;
            require(husk.handler.getAmountAsInt(slot) == expected, "Expected " + expected + " items in husk slot " + slot);
            if (expected > 0) require(husk.handler.getResource(slot).toStack().is(Items.AMETHYST_SHARD), "Wrong stored item");
        }
        require(level.getBlockState(HUSK).getValue(HuskBlock.HUSK_TYPE) == (stored == 0 ? HuskType.BROKEN : HuskType.RESTORATION), "Wrong husk type");
        require(level.getEntitiesOfClass(ItemEntity.class, new AABB(HUSK).inflate(5)).isEmpty(), "Unexpected loose drops near husk");
    }

    private static void finish(ServerPlayer owner, ServerPlayer visitor, String result) throws IOException {
        Files.writeString(root.resolve("phase-" + phase + ".result"), result);
        owner.sendSystemMessage(Component.literal(DONE));
        visitor.sendSystemMessage(Component.literal(DONE));
        step = Step.EXIT;
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
