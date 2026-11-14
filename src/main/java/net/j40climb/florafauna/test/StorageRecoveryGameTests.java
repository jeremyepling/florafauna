package net.j40climb.florafauna.test;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.common.block.husk.HuskBlockEntity;
import net.j40climb.florafauna.common.block.husk.HuskPlacementHelper;
import net.j40climb.florafauna.common.block.husk.HuskDeathHandler;
import net.j40climb.florafauna.common.block.iteminput.rootiteminput.ItemInputBlockEntity;
import net.j40climb.florafauna.common.block.iteminput.storageanchor.StorageAnchorBlockEntity;
import net.j40climb.florafauna.common.block.mininganchor.Tier1MiningAnchorBlockEntity;
import net.j40climb.florafauna.common.block.mininganchor.pod.Tier2PodBlockEntity;
import net.j40climb.florafauna.common.block.vacuum.ClaimedItemData;
import net.j40climb.florafauna.common.symbiote.abilities.DashPayload;
import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.common.symbiote.data.SymbioteState;
import net.j40climb.florafauna.common.symbiote.observation.ChaosSuppressor;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import static net.j40climb.florafauna.test.FloraFaunaGameTests.createTestPlayer;
import static net.j40climb.florafauna.test.FloraFaunaGameTests.registerTest;

final class StorageRecoveryGameTests {
    private static final BlockPos CENTER = new BlockPos(1, 1, 1);

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        registerTest(event, environment, "pod_transactions", StorageRecoveryGameTests::podTransactions);
        registerTest(event, environment, "intake_claim_overflow", StorageRecoveryGameTests::intakeClaimOverflow);
        registerTest(event, environment, "intake_budgets", StorageRecoveryGameTests::intakeBudgets);
        registerTest(event, environment, "sealed_anchor_preserves_drop", StorageRecoveryGameTests::sealedAnchor);
        registerTest(event, environment, "husk_partial_recovery", StorageRecoveryGameTests::partialRecovery);
        registerTest(event, environment, "husk_rejects_visitor_mining", StorageRecoveryGameTests::huskOwnership);
        registerTest(event, environment, "void_husk_returns_to_cocoon_dimension", StorageRecoveryGameTests::voidHusk);
        registerTest(event, environment, "failed_husk_placement_keeps_inventory", StorageRecoveryGameTests::failedPlacement);
        registerTest(event, environment, "dash_requires_active_grant", StorageRecoveryGameTests::dashGrant);
        registerTest(event, environment, "one_observation_per_damage_event", StorageRecoveryGameTests::damageObservations);
    }

    private static void podTransactions(GameTestHelper h) {
        h.setBlock(CENTER, FloraFaunaRegistry.TIER2_POD.get());
        var pod = h.getBlockEntity(CENTER, Tier2PodBlockEntity.class);
        var handler = h.getLevel().getCapability(Capabilities.Item.BLOCK, pod.getBlockPos(), Direction.DOWN);
        var item = ItemResource.of(Items.AMETHYST_SHARD);
        pod.addItems(new ItemStack(Items.AMETHYST_SHARD, 17));
        try (var outer = Transaction.openRoot()) {
            require(h, handler.extract(0, item, 5, outer) == 5, "Could not extract five");
            try (var inner = Transaction.open(outer)) {
                require(h, handler.insert(1, item, 5, inner) == 5, "Could not insert five");
                inner.commit();
            }
        }
        require(h, handler.getAmountAsLong(0) == 17 && handler.getAmountAsLong(1) == 0,
                "Aborted transfer changed pod contents");
        try (var outer = Transaction.openRoot()) {
            handler.extract(0, item, 5, outer);
            try (var inner = Transaction.open(outer)) {
                handler.insert(1, item, 5, inner);
            }
            outer.commit();
        }
        require(h, pod.getStoredCount() == 12 && handler.getAmountAsLong(1) == 0, "Nested abort failed");
        h.succeed();
    }

    private static ItemEntity loose(GameTestHelper h, BlockPos pos, ItemStack stack) {
        ItemEntity drop = new ItemEntity(h.getLevel(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        h.getLevel().addFreshEntity(drop);
        return drop;
    }

    private static void intakeClaimOverflow(GameTestHelper h) {
        h.setBlock(CENTER, FloraFaunaRegistry.ITEM_INPUT.get());
        var input = h.getBlockEntity(CENTER, ItemInputBlockEntity.class);
        var buffer = input.getBuffer();
        for (int i = 1; i < buffer.getMaxStacks(); i++) {
            buffer.setStack(i, new ItemStack(Items.STONE, 64));
        }
        buffer.setStack(0, new ItemStack(Items.IRON_INGOT, 63));
        var pos = input.getBlockPos();
        var a = loose(h, pos, new ItemStack(Items.IRON_INGOT, 3));
        var b = loose(h, pos, new ItemStack(Items.IRON_INGOT, 2));
        var complete = ClaimedItemData.create(pos, h.getLevel().getGameTime() - 100, 20);
        for (ItemEntity drop : new ItemEntity[]{a, b}) {
            drop.setData(FloraFaunaRegistry.CLAIMED_ITEM_DATA, complete);
            drop.setPickUpDelay(Integer.MAX_VALUE);
        }
        input.tick(h.getLevel(), pos, input.getBlockState());
        require(h, buffer.getStack(0).getCount() == 64, "Expected one ingot stored");
        require(h, a.isAlive() && b.isAlive() && a.getItem().getCount() + b.getItem().getCount() == 4,
                "Expected four collectible excess ingots");
        for (ItemEntity drop : new ItemEntity[]{a, b}) {
            require(h, !drop.getData(FloraFaunaRegistry.CLAIMED_ITEM_DATA).claimed() && !drop.hasPickUpDelay(),
                    "Excess ingots stayed claimed");
        }
        h.succeed();
    }

    private static void intakeBudgets(GameTestHelper h) {
        h.setBlock(CENTER, FloraFaunaRegistry.ITEM_INPUT.get());
        h.setBlock(CENTER.east(), FloraFaunaRegistry.STORAGE_ANCHOR.get());
        h.setBlock(CENTER.west(), Blocks.CHEST);
        var input = h.getBlockEntity(CENTER, ItemInputBlockEntity.class);
        var anchor = h.getBlockEntity(CENTER.east(), StorageAnchorBlockEntity.class);
        var chest = h.getBlockEntity(CENTER.west(), ChestBlockEntity.class);
        input.pairWithAnchor(anchor.getBlockPos());
        anchor.linkContainer(chest.getBlockPos());
        input.getBuffer().add(new ItemStack(Items.COBBLESTONE, 64));
        var drop = loose(h, input.getBlockPos(), new ItemStack(Items.IRON_INGOT, 64));
        int oldTransfer = Config.maxItemsPerTransferTick;
        int oldCollect = Config.maxItemsPerCollect;
        try {
            Config.maxItemsPerTransferTick = 1;
            Config.maxItemsPerCollect = 3;
            input.tick(h.getLevel(), input.getBlockPos(), input.getBlockState());
        } finally {
            Config.maxItemsPerTransferTick = oldTransfer;
            Config.maxItemsPerCollect = oldCollect;
        }
        require(h, input.getBuffer().getStack(0).getCount() == 63 && chest.countItem(Items.COBBLESTONE) == 1,
                "Transfer exceeded the one-item budget");
        var claim = drop.getData(FloraFaunaRegistry.CLAIMED_ITEM_DATA);
        require(h, claim.claimed() && claim.quantity() == 3, "Collection exceeded the three-item budget");
        drop.setData(FloraFaunaRegistry.CLAIMED_ITEM_DATA, ClaimedItemData.create(input.getBlockPos(),
                h.getLevel().getGameTime() - 100, claim.animationDuration(), claim.quantity()));
        input.tick(h.getLevel(), input.getBlockPos(), input.getBlockState());
        require(h, drop.isAlive() && drop.getItem().getCount() == 61 && !drop.hasPickUpDelay(),
                "Capped claim lost its unclaimed remainder");
        h.succeed();
    }

    private static void sealedAnchor(GameTestHelper h) {
        h.setBlock(CENTER, FloraFaunaRegistry.TIER1_MINING_ANCHOR.get());
        var anchor = h.getBlockEntity(CENTER, Tier1MiningAnchorBlockEntity.class);
        var pos = anchor.getBlockPos();
        var drop = loose(h, pos, new ItemStack(Items.DIAMOND, 8));
        drop.setData(FloraFaunaRegistry.CLAIMED_ITEM_DATA, ClaimedItemData.create(pos, h.getLevel().getGameTime() - 100, 20));
        int radius = Config.miningAnchorPodSpawnRadius;
        try {
            Config.miningAnchorPodSpawnRadius = 1;
            for (int dy = 0; dy <= 1; dy++) {
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    BlockPos side = CENTER.above(dy).relative(dir);
                    h.setBlock(side, Blocks.STONE);
                    h.setBlock(side.relative(dir.getClockWise()), Blocks.STONE);
                }
            }
            anchor.tick(h.getLevel(), pos, anchor.getBlockState());
        } finally {
            Config.miningAnchorPodSpawnRadius = radius;
        }
        require(h, anchor.getPodCount() == 0 && drop.isAlive() && drop.getItem().getCount() == 8,
                "Sealed anchor destroyed its mining drop");
        require(h, !drop.getData(FloraFaunaRegistry.CLAIMED_ITEM_DATA).claimed() && !drop.hasPickUpDelay(),
                "Sealed anchor retained its claim");
        h.succeed();
    }

    private static void partialRecovery(GameTestHelper h) {
        h.setBlock(CENTER, FloraFaunaRegistry.HUSK.get());
        var husk = h.getBlockEntity(CENTER, HuskBlockEntity.class);
        var player = createTestPlayer(h);
        var inventory = player.getInventory();
        for (int slot = 0; slot < 36; slot++) {
            inventory.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        inventory.setItem(0, new ItemStack(Items.AMETHYST_SHARD, 63));
        husk.handler.set(7, ItemResource.of(Items.AMETHYST_SHARD), 17);
        husk.transferItemsToPlayer(player);
        require(h, inventory.countItem(Items.AMETHYST_SHARD) == 64 && husk.handler.getAmountAsInt(7) == 16,
                "Partial pickup failed to preserve remainder");
        inventory.setItem(1, ItemStack.EMPTY);
        husk.transferItemsToPlayer(player);
        husk.transferItemsToPlayer(player);
        require(h, inventory.countItem(Items.AMETHYST_SHARD) == 80 && husk.isEmpty(), "Second pickup lost or duplicated items");
        h.succeed();
    }

    private static void huskOwnership(GameTestHelper h) {
        h.setBlock(CENTER, FloraFaunaRegistry.HUSK.get());
        var husk = h.getBlockEntity(CENTER, HuskBlockEntity.class);
        var owner = createTestPlayer(h);
        var visitor = createTestPlayer(h);
        husk.setOwner(owner.getUUID());
        husk.handler.set(0, ItemResource.of(Items.DIAMOND), 3);
        for (GameType mode : new GameType[]{GameType.CREATIVE, GameType.SURVIVAL}) {
            visitor.setGameMode(mode);
            visitor.gameMode.destroyBlock(husk.getBlockPos());
            require(h, h.getLevel().getBlockEntity(husk.getBlockPos()) == husk && husk.handler.getAmountAsInt(0) == 3,
                    "Visitor destroyed a stocked husk in " + mode);
        }
        owner.setGameMode(GameType.SURVIVAL);
        require(h, owner.gameMode.destroyBlock(husk.getBlockPos()) && h.getLevel().isEmptyBlock(husk.getBlockPos()),
                "Owner could not break their husk");
        h.succeed();
    }

    private static void voidHusk(GameTestHelper h) {
        h.setBlock(CENTER, FloraFaunaRegistry.COCOON_CHAMBER.get());
        h.setBlock(CENTER.above(), Blocks.AIR);
        ServerLevel overworld = h.getLevel();
        ServerLevel end = overworld.getServer().getLevel(Level.END);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        BlockPos cocoon = h.absolutePos(CENTER);
        player.setData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA,
                PlayerSymbioteData.DEFAULT.withCocoonSpawn(cocoon, overworld.dimension()));
        var placement = HuskPlacementHelper.findPlacementPosition(player, new BlockPos(0, end.getMinY() - 1, 0), end);
        require(h, placement != null && placement.level() == overworld && placement.pos().equals(cocoon.above()),
                "Void fallback missed the cocoon dimension or overwrote the cocoon");
        overworld.getServer().getPlayerList().remove(player);
        h.succeed();
    }

    private static void failedPlacement(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var player = h.makeMockServerPlayerInLevel();
        player.setData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA,
                PlayerSymbioteData.DEFAULT.withSymbioteState(SymbioteState.BONDED_ACTIVE));
        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 5));
        BlockPos outside = h.absolutePos(CENTER).atY(level.getMinY() - 32);
        var previousSpawn = level.getRespawnData();
        try {
            level.setRespawnData(LevelData.RespawnData.of(level.dimension(), outside, 0, 0));
            player.setPos(Vec3.atCenterOf(outside));
            HuskDeathHandler.onPlayerDeath(new LivingDeathEvent(player, player.damageSources().fellOutOfWorld()));
            require(h, player.getInventory().countItem(Items.DIAMOND) == 5, "Failed placement cleared the inventory");
            var bond = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
            require(h, bond.symbioteState() == SymbioteState.BONDED_ACTIVE && !bond.restorationHuskActive(),
                    "Failed placement committed recovery state");
        } finally {
            level.setRespawnData(previousSpawn);
            level.getServer().getPlayerList().remove(player);
        }
        h.succeed();
    }

    private static void dashGrant(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        for (var state : SymbioteState.values()) {
            for (boolean grant : new boolean[]{false, true}) {
                player.setData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA,
                        PlayerSymbioteData.DEFAULT.withSymbioteState(state).withDash(grant));
                player.setDeltaMovement(Vec3.ZERO);
                player.fallDistance = 9;
                DashPayload.dash(player);
                boolean eligible = state.areAbilitiesActive() && grant;
                require(h, (player.getDeltaMovement().lengthSqr() > 8) == eligible
                        && player.fallDistance == (eligible ? 0 : 9), "Invalid dash result for " + state + ", grant=" + grant);
            }
        }
        h.getLevel().getServer().getPlayerList().remove(player);
        h.succeed();
    }

    private static void damageObservations(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        player.setData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA,
                PlayerSymbioteData.DEFAULT.withSymbioteState(SymbioteState.BONDED_ACTIVE));
        try {
            for (int hit = 1; hit <= 5; hit++) {
                var damage = new DamageContainer(player.damageSources().generic(), 1);
                NeoForge.EVENT_BUS.post(new LivingDamageEvent.Pre(player, damage));
                require(h, ChaosSuppressor.isSuppressed(player) == (hit == 5), "Wrong voice suppression on hit " + hit);
            }
        } finally {
            ChaosSuppressor.clearPlayer(player);
            h.getLevel().getServer().getPlayerList().remove(player);
        }
        h.succeed();
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) throw helper.assertionException(message);
    }
}
