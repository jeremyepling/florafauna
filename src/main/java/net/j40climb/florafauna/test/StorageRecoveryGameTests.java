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
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
//? if neoforge {
import net.neoforged.neoforge.common.NeoForge;
//?}
//? if neoforge {
import net.neoforged.neoforge.common.damagesource.DamageContainer;
//?}
//? if neoforge {
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
//?} else {
/*import net.j40climb.florafauna.test.fabric.RegisterGameTestsEvent;
*///?}
//? if neoforge {
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
//?}
//? if neoforge {
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
//?}
//? if neoforge {
import net.neoforged.neoforge.transfer.item.ItemResource;
//?} else {
/*import net.j40climb.florafauna.platform.transfer.item.ItemResource;
*///?}
//? if neoforge {
import net.neoforged.neoforge.transfer.transaction.Transaction;
//?} else {
/*import net.j40climb.florafauna.platform.transfer.transaction.Transaction;
*///?}

import static net.j40climb.florafauna.test.FloraFaunaGameTests.createTestPlayer;
import static net.j40climb.florafauna.test.FloraFaunaGameTests.registerTest;
import static net.j40climb.florafauna.test.FloraFaunaGameTests.registerStructureTest;
import static net.j40climb.florafauna.test.FloraFaunaGameTests.SMALL_TEST_ROOM;

final class StorageRecoveryGameTests {
    private static final BlockPos CENTER = new BlockPos(1, 1, 1);

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        registerTest(event, environment, "pod_transactions", StorageRecoveryGameTests::podTransactions);
        registerTest(event, environment, "intake_claim_overflow", StorageRecoveryGameTests::intakeClaimOverflow);
        registerTest(event, environment, "intake_budgets", StorageRecoveryGameTests::intakeBudgets);
        registerTest(event, environment, "sealed_anchor_preserves_drop", StorageRecoveryGameTests::sealedAnchor);
        registerStructureTest(event, environment, "anchor_distinguishes_mining_from_berry_harvest", SMALL_TEST_ROOM, 100,
                StorageRecoveryGameTests::blockDropOrigin);
        registerTest(event, environment, "husk_partial_recovery", StorageRecoveryGameTests::partialRecovery);
        registerTest(event, environment, "husk_rejects_visitor_mining", StorageRecoveryGameTests::huskOwnership);
        registerTest(event, environment, "void_husk_returns_to_cocoon_dimension", StorageRecoveryGameTests::voidHusk);
        registerTest(event, environment, "failed_husk_placement_keeps_inventory", StorageRecoveryGameTests::failedPlacement);
        registerTest(event, environment, "husk_death_veto_preserves_inventory", StorageRecoveryGameTests::deathVeto);
        registerTest(event, environment, "dash_requires_active_grant", StorageRecoveryGameTests::dashGrant);
        registerTest(event, environment, "one_observation_per_damage_event", StorageRecoveryGameTests::damageObservations);
        registerTest(event, environment, "player_fall_damage_uses_symbiote_ability", StorageRecoveryGameTests::fallDamage);
        registerTest(event, environment, "containment_shift_click_preserves_counted_stacks", StorageRecoveryGameTests::containmentShiftClick);
    }

    private static void podTransactions(GameTestHelper h) {
        h.setBlock(CENTER, FloraFaunaRegistry.TIER2_POD.get());
        var pod = h.getBlockEntity(CENTER, Tier2PodBlockEntity.class);
        var handler = net.j40climb.florafauna.platform.StorageAccess.get(h.getLevel(), pod.getBlockPos(), Direction.DOWN);
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
            net.j40climb.florafauna.platform.AttachmentAccess.set(drop, FloraFaunaRegistry.CLAIMED_ITEM_DATA, complete);
            drop.setPickUpDelay(Integer.MAX_VALUE);
        }
        input.tick(h.getLevel(), pos, input.getBlockState());
        require(h, buffer.getStack(0).getCount() == 64, "Expected one ingot stored");
        require(h, a.isAlive() && b.isAlive() && a.getItem().getCount() + b.getItem().getCount() == 4,
                "Expected four collectible excess ingots");
        for (ItemEntity drop : new ItemEntity[]{a, b}) {
            require(h, !net.j40climb.florafauna.platform.AttachmentAccess.get(drop, FloraFaunaRegistry.CLAIMED_ITEM_DATA).claimed() && !drop.hasPickUpDelay(),
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
        var claim = net.j40climb.florafauna.platform.AttachmentAccess.get(drop, FloraFaunaRegistry.CLAIMED_ITEM_DATA);
        require(h, claim.claimed() && claim.quantity() == 3, "Collection exceeded the three-item budget");
        net.j40climb.florafauna.platform.AttachmentAccess.set(drop, FloraFaunaRegistry.CLAIMED_ITEM_DATA, ClaimedItemData.create(input.getBlockPos(),
                h.getLevel().getGameTime() - 100, claim.animationDuration(), claim.quantity()));
        input.tick(h.getLevel(), input.getBlockPos(), input.getBlockState());
        require(h, drop.isAlive() && drop.getItem().getCount() == 61 && !drop.hasPickUpDelay(),
                "Capped claim lost its unclaimed remainder");
        h.succeed();
    }

    private static void blockDropOrigin(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos stone = CENTER.east(2);
        BlockPos bush = stone.south(2);
        h.setBlock(CENTER, FloraFaunaRegistry.TIER1_MINING_ANCHOR.get());
        h.setBlock(stone, Blocks.STONE);
        h.setBlock(bush.below(), Blocks.DIRT);
        h.setBlock(bush, Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3));
        var player = createTestPlayer(h);
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        require(h, player.gameMode.destroyBlock(h.absolutePos(stone)), "Survival stone break was rejected");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        var absoluteBush = h.absolutePos(bush);
        var harvest = level.getBlockState(absoluteBush).useWithoutItem(level, player,
                new BlockHitResult(Vec3.atCenterOf(absoluteBush), Direction.UP, absoluteBush, false));
        require(h, harvest.consumesAction(), "Ripe berry harvest was rejected");

        var area = new AABB(h.absolutePos(CENTER)).inflate(5);
        var stones = level.getEntitiesOfClass(ItemEntity.class, area, item -> item.getItem().is(Items.COBBLESTONE));
        var berries = level.getEntitiesOfClass(ItemEntity.class, area, item -> item.getItem().is(Items.SWEET_BERRIES));
        require(h, stones.size() == 1 && stones.getFirst().getItem().getCount() == 1,
                "Breaking one stone did not yield exactly one cobblestone entity");
        int berryCount = berries.stream().mapToInt(item -> item.getItem().getCount()).sum();
        require(h, berryCount >= 2 && berryCount <= 3,
                "A ripe bush did not yield its vanilla two-or-three berries, got " + berryCount);
        var mined = stones.getFirst();
        // Vanilla harvest uses two loot pools and can emit separate entities. Freeze merging so each original stack is traceable.
        var harvestedStacks = berries.stream().map(item -> item.getItem().copy()).toList();
        berries.forEach(ItemEntity::setNeverPickUp);
        require(h, net.j40climb.florafauna.platform.AttachmentAccess.get(mined, FloraFaunaRegistry.BLOCK_DROP_DATA).isBlockDrop(),
                "Stone mining loot was not marked as a block drop");
        require(h, berries.stream().noneMatch(item -> net.j40climb.florafauna.platform.AttachmentAccess.get(item, FloraFaunaRegistry.BLOCK_DROP_DATA).isBlockDrop()),
                "Right-click berry harvest was incorrectly marked as block-breaking loot");
        require(h, ItemStack.isSameItemSameComponents(new ItemStack(Items.COBBLESTONE), mined.getItem()),
                "Origin tagging changed the mined cobblestone components");

        var anchor = h.getBlockEntity(CENTER, Tier1MiningAnchorBlockEntity.class);
        anchor.tick(level, anchor.getBlockPos(), anchor.getBlockState());
        require(h, net.j40climb.florafauna.platform.AttachmentAccess.get(mined, FloraFaunaRegistry.CLAIMED_ITEM_DATA).claimed()
                        && berries.stream().noneMatch(item -> net.j40climb.florafauna.platform.AttachmentAccess.get(item, FloraFaunaRegistry.CLAIMED_ITEM_DATA).claimed()),
                "Block-drop-only anchor did not distinguish mining from right-click harvesting");
        h.startSequence().thenWaitUntil(() -> require(h, anchor.getStoredCount() == 1 && !mined.isAlive(),
                        "Anchor did not absorb exactly the one mined cobblestone"))
                .thenExecute(() -> {
                    for (int index = 0; index < berries.size(); index++) {
                        var harvested = berries.get(index);
                        var original = harvestedStacks.get(index);
                        require(h, harvested.isAlive() && harvested.getItem().getCount() == original.getCount()
                                        && ItemStack.isSameItemSameComponents(original, harvested.getItem())
                                        && !net.j40climb.florafauna.platform.AttachmentAccess.get(harvested, FloraFaunaRegistry.CLAIMED_ITEM_DATA).claimed(),
                                "Anchor consumed or changed a non-mining berry harvest stack");
                    }
                })
                .thenSucceed();
    }

    private static void sealedAnchor(GameTestHelper h) {
        h.setBlock(CENTER, FloraFaunaRegistry.TIER1_MINING_ANCHOR.get());
        var anchor = h.getBlockEntity(CENTER, Tier1MiningAnchorBlockEntity.class);
        var pos = anchor.getBlockPos();
        var drop = loose(h, pos, new ItemStack(Items.DIAMOND, 8));
        net.j40climb.florafauna.platform.AttachmentAccess.set(drop, FloraFaunaRegistry.CLAIMED_ITEM_DATA, ClaimedItemData.create(pos, h.getLevel().getGameTime() - 100, 20));
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
        require(h, !net.j40climb.florafauna.platform.AttachmentAccess.get(drop, FloraFaunaRegistry.CLAIMED_ITEM_DATA).claimed() && !drop.hasPickUpDelay(),
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
        net.j40climb.florafauna.platform.AttachmentAccess.set(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA,
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
        net.j40climb.florafauna.platform.AttachmentAccess.set(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA,
                PlayerSymbioteData.DEFAULT.withSymbioteState(SymbioteState.BONDED_ACTIVE));
        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 5));
        BlockPos outside = h.absolutePos(CENTER).atY(level.getMinY() - 32);
        var previousSpawn = level.getRespawnData();
        try {
            level.setRespawnData(LevelData.RespawnData.of(level.dimension(), outside, 0, 0));
            player.setPos(Vec3.atCenterOf(outside));
            HuskDeathHandler.onPlayerDeath(player);
            require(h, player.getInventory().countItem(Items.DIAMOND) == 5, "Failed placement cleared the inventory");
            var bond = net.j40climb.florafauna.platform.AttachmentAccess.get(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
            require(h, bond.symbioteState() == SymbioteState.BONDED_ACTIVE && !bond.restorationHuskActive(),
                    "Failed placement committed recovery state");
        } finally {
            level.setRespawnData(previousSpawn);
            level.getServer().getPlayerList().remove(player);
        }
        h.succeed();
    }

    private static void deathVeto(GameTestHelper h) {
        var player = connectSurvivalPlayer(h);
        player.setGameMode(GameType.SURVIVAL);
        var stack = new ItemStack(Items.AMETHYST_SHARD, 17);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                net.minecraft.network.chat.Component.literal("Protected shards"));
        player.getInventory().setItem(4, stack.copy());
        var bond = PlayerSymbioteData.DEFAULT.withSymbioteState(SymbioteState.BONDED_ACTIVE);
        net.j40climb.florafauna.platform.AttachmentAccess.set(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA, bond);
        Runnable removeVeto;
        //? if neoforge {
        java.util.function.Consumer<LivingDeathEvent> veto = event -> {
            if (event.getEntity() == player) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, LivingDeathEvent.class, veto);
        removeVeto = () -> NeoForge.EVENT_BUS.unregister(veto);
        //?} else {
        /*removeVeto = net.j40climb.florafauna.test.fabric.FabricDeathVeto.block(player);
        *///?}
        try {
            clearDamageCooldown(player);
            player.hurtServer(h.getLevel(), player.damageSources().generic(), Float.MAX_VALUE);
            var retained = player.getInventory().getItem(4);
            require(h, retained.getCount() == 17 && ItemStack.isSameItemSameComponents(stack, retained),
                    "Vetoed death moved, changed or lost the named seventeen-shard stack");
            var retainedBond = net.j40climb.florafauna.platform.AttachmentAccess.get(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
            require(h, retainedBond.symbioteState() == SymbioteState.BONDED_ACTIVE && !retainedBond.restorationHuskActive(),
                    "Vetoed death created a husk or changed the active bond");
        } finally {
            removeVeto.run();
            player.setHealth(20);
            h.getLevel().getServer().getPlayerList().remove(player);
        }
        h.succeed();
    }

    private static void dashGrant(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        for (var state : SymbioteState.values()) {
            for (boolean grant : new boolean[]{false, true}) {
                net.j40climb.florafauna.platform.AttachmentAccess.set(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA,
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
        var player = connectSurvivalPlayer(h);
        var attacker = (net.minecraft.world.entity.LivingEntity) net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .getValue(net.minecraft.resources.Identifier.withDefaultNamespace("zombie"))
                .create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        player.setGameMode(GameType.SURVIVAL);
        // A living attacker exercises combat observations; an unscaled type keeps the fixture independent of difficulty.
        var source = new net.minecraft.world.damagesource.DamageSource(player.damageSources().generic().typeHolder(), attacker);
        net.j40climb.florafauna.platform.AttachmentAccess.set(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA,
                PlayerSymbioteData.DEFAULT.withSymbioteState(SymbioteState.BONDED_ACTIVE));
        try {
            for (int hit = 1; hit <= 5; hit++) {
                clearDamageCooldown(player);
                float previousHealth = player.getHealth();
                player.hurtServer(h.getLevel(), source, 1);
                require(h, player.getHealth() == previousHealth - 1, "Combat fixture did not deliver damage on hit " + hit);
                require(h, ChaosSuppressor.isSuppressed(player) == (hit == 5), "Wrong voice suppression on hit " + hit);
            }
        } finally {
            ChaosSuppressor.clearPlayer(player);
            h.getLevel().getServer().getPlayerList().remove(player);
        }
        h.succeed();
    }

    private static void fallDamage(GameTestHelper h) {
        var player = connectSurvivalPlayer(h);
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.setHealth(20);
        player.setAbsorptionAmount(0);
        var bonded = PlayerSymbioteData.DEFAULT.withSymbioteState(SymbioteState.BONDED_ACTIVE);
        try {
            net.j40climb.florafauna.platform.AttachmentAccess.set(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA,
                    bonded.withFeatherFalling(true));
            clearDamageCooldown(player);
            player.hurtServer(h.getLevel(), player.damageSources().fall(), 6);
            require(h, player.getHealth() == 20, "Actual player fall damage bypassed the granted feather-falling ability");
            net.j40climb.florafauna.platform.AttachmentAccess.set(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA,
                    bonded.withFeatherFalling(false));
            clearDamageCooldown(player);
            player.hurtServer(h.getLevel(), player.damageSources().fall(), 4);
            require(h, player.getHealth() == 16, "Fall damage remained canceled after the ability was removed");
        } finally {
            ChaosSuppressor.clearPlayer(player);
            h.getLevel().getServer().getPlayerList().remove(player);
        }
        h.succeed();
    }

    private static void containmentShiftClick(GameTestHelper h) {
        h.setBlock(CENTER, FloraFaunaRegistry.SYMBIOTE_CONTAINMENT_CHAMBER.get());
        var chamber = h.getBlockEntity(CENTER,
                net.j40climb.florafauna.common.block.containmentchamber.ContainmentChamberBlockEntity.class);
        var player = createTestPlayer(h);
        var inventory = player.getInventory();
        var countedCopper = new ItemStack(Items.COPPER_INGOT);
        countedCopper.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                net.minecraft.network.chat.Component.literal("Counted copper"));
        chamber.handler.set(1, ItemResource.of(countedCopper), 32);
        inventory.setItem(9, countedCopper.copyWithCount(32));
        var menu = new net.j40climb.florafauna.common.block.containmentchamber.ContainmentChamberMenu(
                1, inventory, chamber, new net.minecraft.world.inventory.SimpleContainerData(2));
        require(h, !menu.quickMoveStack(player, 0).isEmpty(), "Player-to-feed shift-click failed");
        require(h, inventory.getItem(9).isEmpty() && chamber.handler.getAmountAsInt(1) == 64
                        && inventory.countItem(Items.COPPER_INGOT) + chamber.handler.getAmountAsInt(1) == 64
                        && ItemStack.isSameItemSameComponents(countedCopper, chamber.handler.getResource(1).toStack(64)),
                "Merging matching feed stacks lost count or the custom name");

        for (int slot = 0; slot < 36; slot++) inventory.setItem(slot, new ItemStack(Items.STONE, 64));
        inventory.setItem(9, countedCopper.copyWithCount(60));
        chamber.handler.set(1, ItemResource.of(countedCopper), 12);
        require(h, !menu.quickMoveStack(player, 37).isEmpty(), "Feed-to-player partial shift-click failed");
        require(h, chamber.handler.getAmountAsInt(1) == 8 && inventory.getItem(9).getCount() == 64
                        && inventory.countItem(Items.COPPER_INGOT) + chamber.handler.getAmountAsInt(1) == 72,
                "Partial feed extraction did not retain eight and deliver exactly four copper");
        require(h, ItemStack.isSameItemSameComponents(countedCopper, chamber.handler.getResource(1).toStack(8))
                        && ItemStack.isSameItemSameComponents(countedCopper, inventory.getItem(9)),
                "Partial shift-click changed named stack components");
        h.succeed();
    }

    /** Real Survival semantics and an acknowledged embedded connection are required to exercise player damage. */
    private static ServerPlayer connectSurvivalPlayer(GameTestHelper h) {
        var level = h.getLevel();
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "survival-fixture"), false);
        var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {};
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        //? if neoforge {
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        //?}
        player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        player.setGameMode(GameType.SURVIVAL);
        player.snapTo(h.absoluteVec(new Vec3(2.5, 1, 2.5)), 0, 0);
        require(h, player.connection.hasClientLoaded() && player.gameMode() == GameType.SURVIVAL,
                "Embedded damage fixture did not enter loaded Survival mode");
        return player;
    }

    private static void clearDamageCooldown(ServerPlayer player) {
        //? if <26.3 {
        player.invulnerableTime = 0;
        //?} else {
        /*player.damageCooldownTime = 0;
        *///?}
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) throw helper.assertionException(message);
    }
}
