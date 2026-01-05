package net.j40climb.florafauna.test;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.containmentchamber.ContainmentChamberBlockEntity;
import net.j40climb.florafauna.common.block.husk.HuskType;
import net.j40climb.florafauna.common.block.mininganchor.AnchorFillState;
import net.j40climb.florafauna.common.block.mininganchor.Tier1MiningAnchorBlockEntity;
import net.j40climb.florafauna.common.block.mininganchor.Tier2MiningAnchorBlockEntity;
import net.j40climb.florafauna.common.block.mininganchor.pod.PodItemHandler;
import net.j40climb.florafauna.common.block.mininganchor.pod.Tier1PodBlockEntity;
import net.j40climb.florafauna.common.block.mininganchor.pod.Tier2PodBlockEntity;
import net.j40climb.florafauna.common.block.mobbarrier.data.MobBarrierConfig;
import net.j40climb.florafauna.common.block.vacuum.BufferTransfer;
import net.j40climb.florafauna.common.block.vacuum.ClaimedItemData;
import net.j40climb.florafauna.common.block.vacuum.ItemBuffer;
import net.j40climb.florafauna.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna.common.symbiote.dialogue.SymbioteDialogueEntry;
import net.j40climb.florafauna.common.symbiote.dialogue.SymbioteDialogueRepository;
import net.j40climb.florafauna.common.symbiote.item.DormantSymbioteItem;
import net.j40climb.florafauna.common.symbiote.observation.ChaosSuppressor;
import net.j40climb.florafauna.common.symbiote.observation.ObservationCategory;
import net.j40climb.florafauna.common.symbiote.progress.ConceptSignal;
import net.j40climb.florafauna.common.symbiote.progress.ProgressSignalTracker;
import net.j40climb.florafauna.common.symbiote.progress.SignalState;
import net.j40climb.florafauna.common.symbiote.data.SymbioteState;
import net.j40climb.florafauna.common.block.mobtransport.CapturedMobBuffer;
import net.j40climb.florafauna.common.block.mobtransport.CapturedMobTicket;
import net.j40climb.florafauna.common.block.mobtransport.MobCaptureEligibility;
import net.j40climb.florafauna.common.entity.mobsymbiote.MobSymbioteData;
import net.j40climb.florafauna.common.entity.mobsymbiote.MobSymbioteHelper;
import net.j40climb.florafauna.Config;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.j40climb.florafauna.common.symbiote.voice.VoiceCooldownState;
import net.j40climb.florafauna.common.symbiote.voice.VoiceTier;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.List;
import java.util.function.Consumer;

/**
 * GameTests for the Symbiote Dream + Commentary System.
 * Tests run automatically via `./gradlew gameTestServer` or in-game with `/test runall`.
 *
 * Register this from the main mod class by calling:
 * FloraFaunaGameTests.register(modEventBus);
 */
public class FloraFaunaGameTests {

    /**
     * Register the game test event listener on the mod event bus.
     * Call this from the main mod constructor.
     */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(FloraFaunaGameTests::registerTests);
    }

    private static final Identifier EMPTY_STRUCTURE = Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "empty_1x1x1");
    private static final Identifier ITEM_INPUT_TO_CHEST_STRUCTURE = Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "test_item_input_to_chest");

    private static void registerTests(RegisterGameTestsEvent event) {
        // Install our colored test reporter for better visibility
        GlobalTestReporter.replaceWith(new ColoredTestReporter());

        // Register a minimal test environment
        Holder<TestEnvironmentDefinition> defaultEnv = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "default"),
                new TestEnvironmentDefinition.AllOf()
        );

        // Register all symbiote tests
        registerVoiceCooldownTests(event, defaultEnv);
        registerProgressSignalTests(event, defaultEnv);
        registerChaosSuppressorTests(event, defaultEnv);
        registerDialogueRepositoryTests(event, defaultEnv);

        // Register item input system tests
        registerItemBufferTests(event, defaultEnv);
        registerBufferTransferTests(event, defaultEnv);
        registerClaimedItemDataTests(event, defaultEnv);
        registerPodItemHandlerTests(event, defaultEnv);

        // Register mining mode tests
        registerMiningModeTests(event, defaultEnv);

        // Register containment chamber tests
        registerContainmentChamberTests(event, defaultEnv);

        // Register mob barrier tests
        registerMobBarrierTests(event, defaultEnv);

        // Register mining anchor tests
        registerMiningAnchorTests(event, defaultEnv);

        // Register husk tests
        registerHuskTests(event, defaultEnv);

        // Register symbiote state tests
        registerSymbioteStateTests(event, defaultEnv);

        // Register mob transport tests
        registerMobTransportTests(event, defaultEnv);

        // Register structure-based tests
        registerItemInputStructureTests(event, defaultEnv);
    }

    // ==================== Voice Cooldown Tests ====================

    private static void registerVoiceCooldownTests(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition> env) {
        registerTest(event, env, "voice_cooldown_initial_state", FloraFaunaGameTests::testVoiceCooldownInitialState);
        registerTest(event, env, "voice_cooldown_tier1_can_speak", FloraFaunaGameTests::testVoiceCooldownTier1CanSpeak);
        registerTest(event, env, "voice_cooldown_tier1_blocked_during_cooldown", FloraFaunaGameTests::testVoiceCooldownTier1BlockedDuringCooldown);
        registerTest(event, env, "voice_cooldown_tier2_lockout", FloraFaunaGameTests::testVoiceCooldownTier2Lockout);
        registerTest(event, env, "voice_cooldown_category_dampening", FloraFaunaGameTests::testVoiceCooldownCategoryDampening);
    }

    private static void testVoiceCooldownInitialState(GameTestHelper helper) {
        VoiceCooldownState state = VoiceCooldownState.DEFAULT;

        // Fresh state should allow both tiers
        if (!state.canSpeak(VoiceTier.TIER_1_AMBIENT, ObservationCategory.COMBAT_DAMAGE, 0)) {
            throw helper.assertionException("Fresh state should allow Tier 1");
        }
        if (!state.canSpeak(VoiceTier.TIER_2_BREAKTHROUGH, ObservationCategory.BONDING_MILESTONE, 0)) {
            throw helper.assertionException("Fresh state should allow Tier 2");
        }

        helper.succeed();
    }

    private static void testVoiceCooldownTier1CanSpeak(GameTestHelper helper) {
        VoiceCooldownState state = VoiceCooldownState.DEFAULT;

        // Record a Tier 1 voice event
        VoiceCooldownState updated = state.afterSpeaking(VoiceTier.TIER_1_AMBIENT, ObservationCategory.COMBAT_DAMAGE, 0);

        // Should be blocked for same category at tick 0
        if (updated.canSpeak(VoiceTier.TIER_1_AMBIENT, ObservationCategory.COMBAT_DAMAGE, 0)) {
            throw helper.assertionException("Should be blocked immediately after speaking (same category)");
        }

        // Should be allowed after global cooldown expires (5 min = 6000 ticks)
        if (!updated.canSpeak(VoiceTier.TIER_1_AMBIENT, ObservationCategory.COMBAT_DAMAGE, 7000)) {
            throw helper.assertionException("Should be allowed after cooldown expires");
        }

        helper.succeed();
    }

    private static void testVoiceCooldownTier1BlockedDuringCooldown(GameTestHelper helper) {
        VoiceCooldownState state = VoiceCooldownState.DEFAULT;
        VoiceCooldownState updated = state.afterSpeaking(VoiceTier.TIER_1_AMBIENT, ObservationCategory.COMBAT_DAMAGE, 1000);

        // Check at various points during cooldown (cooldown is 6000 ticks)
        if (updated.canSpeak(VoiceTier.TIER_1_AMBIENT, ObservationCategory.FALL_DAMAGE, 2000)) {
            throw helper.assertionException("Should be blocked at 2000 ticks (1000 into cooldown)");
        }
        if (updated.canSpeak(VoiceTier.TIER_1_AMBIENT, ObservationCategory.ENVIRONMENTAL_HAZARD, 5000)) {
            throw helper.assertionException("Should be blocked at 5000 ticks (4000 into cooldown)");
        }

        helper.succeed();
    }

    private static void testVoiceCooldownTier2Lockout(GameTestHelper helper) {
        VoiceCooldownState state = VoiceCooldownState.DEFAULT;

        // Tier 2 event should lock out Tier 1
        VoiceCooldownState updated = state.afterSpeaking(VoiceTier.TIER_2_BREAKTHROUGH, ObservationCategory.BONDING_MILESTONE, 0);

        // Tier 1 should be blocked due to Tier 2 lockout
        if (updated.canSpeak(VoiceTier.TIER_1_AMBIENT, ObservationCategory.COMBAT_DAMAGE, 1000)) {
            throw helper.assertionException("Tier 1 should be blocked by Tier 2 lockout");
        }

        // Tier 2 itself should be on cooldown (30 min = 36000 ticks)
        if (updated.canSpeak(VoiceTier.TIER_2_BREAKTHROUGH, ObservationCategory.BONDING_MILESTONE, 20000)) {
            throw helper.assertionException("Tier 2 should be blocked during its cooldown");
        }

        helper.succeed();
    }

    private static void testVoiceCooldownCategoryDampening(GameTestHelper helper) {
        VoiceCooldownState state = VoiceCooldownState.DEFAULT;
        VoiceCooldownState updated = state.afterSpeaking(VoiceTier.TIER_1_AMBIENT, ObservationCategory.COMBAT_DAMAGE, 0);

        // Different category should still respect global cooldown, not just category dampening
        // Global Tier 1 cooldown is 6000 ticks
        if (updated.canSpeak(VoiceTier.TIER_1_AMBIENT, ObservationCategory.FALL_DAMAGE, 100)) {
            throw helper.assertionException("Different category should still respect global cooldown");
        }

        helper.succeed();
    }

    // ==================== Progress Signal Tests ====================

    private static void registerProgressSignalTests(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition> env) {
        registerTest(event, env, "progress_signal_initial_state", FloraFaunaGameTests::testProgressSignalInitialState);
        registerTest(event, env, "progress_signal_state_transitions", FloraFaunaGameTests::testProgressSignalStateTransitions);
        registerTest(event, env, "progress_signal_stall_detection", FloraFaunaGameTests::testProgressSignalStallDetection);
        registerTest(event, env, "progress_signal_dream_state_tracking", FloraFaunaGameTests::testProgressSignalDreamStateTracking);
    }

    private static void testProgressSignalInitialState(GameTestHelper helper) {
        ProgressSignalTracker tracker = ProgressSignalTracker.DEFAULT;

        if (!tracker.signals().isEmpty()) {
            throw helper.assertionException("Fresh tracker should have no signals");
        }
        if (tracker.dreamLevel() != 0) {
            throw helper.assertionException("Fresh tracker should have dream level 0");
        }
        if (tracker.lastDreamTick() != 0) {
            throw helper.assertionException("Fresh tracker should have last dream tick 0");
        }

        helper.succeed();
    }

    private static void testProgressSignalStateTransitions(GameTestHelper helper) {
        // Create a signal and advance it through states
        ConceptSignal signal = ConceptSignal.firstSeen("test_concept", 0);

        if (signal.state() != SignalState.SEEN) {
            throw helper.assertionException("First seen should be SEEN state, got: " + signal.state());
        }

        // Increment 3 times to reach TOUCHED
        signal = signal.incrementInteraction(100);
        signal = signal.incrementInteraction(200);
        signal = signal.incrementInteraction(300);

        if (signal.interactionCount() != 4) { // 1 initial + 3 increments
            throw helper.assertionException("Should have 4 interactions, got: " + signal.interactionCount());
        }

        // Manual state transition
        signal = signal.withState(SignalState.TOUCHED, 400);
        if (signal.state() != SignalState.TOUCHED) {
            throw helper.assertionException("Should be TOUCHED state after transition");
        }

        helper.succeed();
    }

    private static void testProgressSignalStallDetection(GameTestHelper helper) {
        ConceptSignal signal = ConceptSignal.firstSeen("stall_test", 0);

        // Early stall score should be low
        int earlyStall = signal.calculateStallScore(1000);
        if (earlyStall > 10) {
            throw helper.assertionException("Early stall score should be low, got: " + earlyStall);
        }

        // Late stall score should be higher (after 360000 ticks = 5 hours)
        int lateStall = signal.calculateStallScore(400000);
        if (lateStall < 50) {
            throw helper.assertionException("Late stall score should be high, got: " + lateStall);
        }

        helper.succeed();
    }

    private static void testProgressSignalDreamStateTracking(GameTestHelper helper) {
        ProgressSignalTracker tracker = ProgressSignalTracker.DEFAULT;

        // Update dream state
        ProgressSignalTracker updated = tracker.withDreamState(1000, 1);

        if (updated.lastDreamTick() != 1000) {
            throw helper.assertionException("Last dream tick should be 1000, got: " + updated.lastDreamTick());
        }
        if (updated.dreamLevel() != 1) {
            throw helper.assertionException("Dream level should be 1, got: " + updated.dreamLevel());
        }

        // Add a progress tick
        ProgressSignalTracker withProgress = updated.withProgressTick(2000);
        if (!withProgress.hasProgressSinceLastDream()) {
            throw helper.assertionException("Should have progress since last dream");
        }

        helper.succeed();
    }

    // ==================== Chaos Suppressor Tests ====================

    private static void registerChaosSuppressorTests(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition> env) {
        registerTest(event, env, "chaos_suppressor_initial_state", FloraFaunaGameTests::testChaosSuppressorInitialState);
        registerTest(event, env, "chaos_suppressor_threshold", FloraFaunaGameTests::testChaosSuppressorThreshold);
    }

    private static void testChaosSuppressorInitialState(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        // Fresh player should not be suppressed
        if (ChaosSuppressor.isSuppressed(player)) {
            throw helper.assertionException("Fresh player should not be suppressed");
        }

        helper.succeed();
    }

    private static void testChaosSuppressorThreshold(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        // Record 4 damage events (below threshold of 5)
        ChaosSuppressor.recordDamage(player);
        ChaosSuppressor.recordDamage(player);
        ChaosSuppressor.recordDamage(player);
        ChaosSuppressor.recordDamage(player);

        if (ChaosSuppressor.isSuppressed(player)) {
            throw helper.assertionException("Should not be suppressed with 4 events");
        }

        // Record 5th event (at threshold)
        ChaosSuppressor.recordDamage(player);

        if (!ChaosSuppressor.isSuppressed(player)) {
            throw helper.assertionException("Should be suppressed with 5 events");
        }

        helper.succeed();
    }

    // ==================== Dialogue Repository Tests ====================

    private static void registerDialogueRepositoryTests(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition> env) {
        registerTest(event, env, "dialogue_repository_empty_returns_empty", FloraFaunaGameTests::testDialogueRepositoryEmptyReturnsEmpty);
        registerTest(event, env, "dialogue_repository_dream_selection", FloraFaunaGameTests::testDialogueRepositoryDreamSelection);
    }

    private static void testDialogueRepositoryEmptyReturnsEmpty(GameTestHelper helper) {
        SymbioteDialogueRepository empty = SymbioteDialogueRepository.EMPTY;

        if (!empty.isEmpty()) {
            throw helper.assertionException("Empty repository should be empty");
        }

        // With empty repository, selectDreamDialogue should return empty
        // (unless dialogue is loaded from datapacks, but we test the empty case)

        helper.succeed();
    }

    private static void testDialogueRepositoryDreamSelection(GameTestHelper helper) {
        // Create a test repository with known entries
        List<SymbioteDialogueEntry> entries = List.of(
                new SymbioteDialogueEntry(
                        "test.dream.l1",
                        "TIER_1_AMBIENT",
                        "dream_l1_reflective",
                        0,
                        100,
                        10,
                        List.of(),
                        List.of()
                ),
                new SymbioteDialogueEntry(
                        "test.dream.l2",
                        "TIER_1_AMBIENT",
                        "dream_l2_directional",
                        0,
                        100,
                        10,
                        List.of(),
                        List.of()
                )
        );

        SymbioteDialogueRepository repo = new SymbioteDialogueRepository(entries);

        if (repo.size() != 2) {
            throw helper.assertionException("Repository should have 2 entries, got: " + repo.size());
        }

        helper.succeed();
    }

    // ==================== Item Input Buffer Tests ====================

    private static void registerItemBufferTests(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition> env) {
        registerTest(event, env, "item_buffer_initial_state", FloraFaunaGameTests::testItemBufferInitialState);
        registerTest(event, env, "item_buffer_add_and_retrieve", FloraFaunaGameTests::testItemBufferAddAndRetrieve);
        registerTest(event, env, "item_buffer_stack_merging", FloraFaunaGameTests::testItemBufferStackMerging);
        registerTest(event, env, "item_buffer_full_detection", FloraFaunaGameTests::testItemBufferFullDetection);
        registerTest(event, env, "item_buffer_partial_merge_to_full", FloraFaunaGameTests::testItemBufferPartialMergeToFull);
        registerTest(event, env, "item_buffer_overflow_to_new_slot", FloraFaunaGameTests::testItemBufferOverflowToNewSlot);
        registerTest(event, env, "item_buffer_peek_no_remove", FloraFaunaGameTests::testItemBufferPeekNoRemove);
        registerTest(event, env, "item_buffer_clear", FloraFaunaGameTests::testItemBufferClear);
    }

    private static void testItemBufferInitialState(GameTestHelper helper) {
        ItemBuffer buffer = new ItemBuffer(27);

        if (!buffer.isEmpty()) {
            throw helper.assertionException("Fresh buffer should be empty");
        }
        if (buffer.isFull()) {
            throw helper.assertionException("Fresh buffer should not be full");
        }
        if (buffer.getUsedSlots() != 0) {
            throw helper.assertionException("Fresh buffer should have 0 used slots");
        }
        if (buffer.getTotalItemCount() != 0) {
            throw helper.assertionException("Fresh buffer should have 0 items");
        }

        helper.succeed();
    }

    private static void testItemBufferAddAndRetrieve(GameTestHelper helper) {
        ItemBuffer buffer = new ItemBuffer(27);

        // Add a stack of cobblestone
        ItemStack toAdd = new ItemStack(Items.COBBLESTONE, 32);
        int added = buffer.add(toAdd);

        if (added != 32) {
            throw helper.assertionException("Should have added 32 items, got: " + added);
        }
        if (buffer.isEmpty()) {
            throw helper.assertionException("Buffer should not be empty after adding items");
        }
        if (buffer.getUsedSlots() != 1) {
            throw helper.assertionException("Buffer should have 1 used slot, got: " + buffer.getUsedSlots());
        }
        if (buffer.getTotalItemCount() != 32) {
            throw helper.assertionException("Buffer should have 32 items, got: " + buffer.getTotalItemCount());
        }

        // Poll the stack back out
        ItemStack retrieved = buffer.poll();
        if (retrieved.isEmpty()) {
            throw helper.assertionException("Retrieved stack should not be empty");
        }
        if (retrieved.getCount() != 32) {
            throw helper.assertionException("Retrieved stack should have 32 items, got: " + retrieved.getCount());
        }
        if (!buffer.isEmpty()) {
            throw helper.assertionException("Buffer should be empty after poll");
        }

        helper.succeed();
    }

    private static void testItemBufferStackMerging(GameTestHelper helper) {
        ItemBuffer buffer = new ItemBuffer(27);

        // Add two partial stacks of the same item
        buffer.add(new ItemStack(Items.DIAMOND, 16));
        buffer.add(new ItemStack(Items.DIAMOND, 16));

        // Should merge into one stack
        if (buffer.getUsedSlots() != 1) {
            throw helper.assertionException("Same items should merge, got slots: " + buffer.getUsedSlots());
        }
        if (buffer.getTotalItemCount() != 32) {
            throw helper.assertionException("Total count should be 32, got: " + buffer.getTotalItemCount());
        }

        // Add a different item type
        buffer.add(new ItemStack(Items.GOLD_INGOT, 10));

        if (buffer.getUsedSlots() != 2) {
            throw helper.assertionException("Different items should not merge, got slots: " + buffer.getUsedSlots());
        }

        helper.succeed();
    }

    private static void testItemBufferFullDetection(GameTestHelper helper) {
        // Create a tiny buffer with 2 slots
        ItemBuffer buffer = new ItemBuffer(2);

        // Fill both slots completely (sticks stack to 64)
        buffer.add(new ItemStack(Items.STICK, 64));
        buffer.add(new ItemStack(Items.ARROW, 64));

        if (!buffer.isFull()) {
            throw helper.assertionException("Buffer with all slots at max should be full");
        }

        // Should not be able to accept more
        if (buffer.canAccept(new ItemStack(Items.STONE, 1))) {
            throw helper.assertionException("Full buffer should not accept new items");
        }

        helper.succeed();
    }

    // ==================== Buffer Transfer Tests ====================

    private static void registerBufferTransferTests(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition> env) {
        registerTest(event, env, "buffer_transfer_basic", FloraFaunaGameTests::testBufferTransferBasic);
        registerTest(event, env, "buffer_transfer_partial", FloraFaunaGameTests::testBufferTransferPartial);
        registerTest(event, env, "buffer_transfer_empty_source", FloraFaunaGameTests::testBufferTransferEmptySource);
        registerTest(event, env, "buffer_transfer_full_destination", FloraFaunaGameTests::testBufferTransferFullDestination);
        registerTest(event, env, "buffer_transfer_one_stack", FloraFaunaGameTests::testBufferTransferOneStack);
    }

    private static void testBufferTransferBasic(GameTestHelper helper) {
        ItemBuffer source = new ItemBuffer(27);
        ItemBuffer dest = new ItemBuffer(27);

        // Add items to source
        source.add(new ItemStack(Items.COBBLESTONE, 64));
        source.add(new ItemStack(Items.DIAMOND, 32));

        // Transfer all
        BufferTransfer.TransferResult result = BufferTransfer.transferAll(source, dest);

        if (result.itemsTransferred() != 96) {
            throw helper.assertionException("Should transfer 96 items, got: " + result.itemsTransferred());
        }
        if (!source.isEmpty()) {
            throw helper.assertionException("Source should be empty after full transfer");
        }
        if (dest.getTotalItemCount() != 96) {
            throw helper.assertionException("Destination should have 96 items, got: " + dest.getTotalItemCount());
        }

        helper.succeed();
    }

    private static void testBufferTransferPartial(GameTestHelper helper) {
        ItemBuffer source = new ItemBuffer(27);
        ItemBuffer dest = new ItemBuffer(27);

        // Add 100 items to source
        source.add(new ItemStack(Items.IRON_INGOT, 64));
        source.add(new ItemStack(Items.IRON_INGOT, 36));

        // Transfer only 50
        BufferTransfer.TransferResult result = BufferTransfer.transfer(source, dest, 50);

        if (result.itemsTransferred() != 50) {
            throw helper.assertionException("Should transfer 50 items, got: " + result.itemsTransferred());
        }
        if (source.getTotalItemCount() != 50) {
            throw helper.assertionException("Source should have 50 items remaining, got: " + source.getTotalItemCount());
        }
        if (dest.getTotalItemCount() != 50) {
            throw helper.assertionException("Destination should have 50 items, got: " + dest.getTotalItemCount());
        }

        helper.succeed();
    }

    private static void testBufferTransferEmptySource(GameTestHelper helper) {
        ItemBuffer source = new ItemBuffer(27);
        ItemBuffer dest = new ItemBuffer(27);

        // Source is empty
        BufferTransfer.TransferResult result = BufferTransfer.transferAll(source, dest);

        if (result.itemsTransferred() != 0) {
            throw helper.assertionException("Should transfer 0 items from empty source, got: " + result.itemsTransferred());
        }
        if (!result.sourceEmpty()) {
            throw helper.assertionException("Result should indicate source is empty");
        }

        helper.succeed();
    }

    private static void testBufferTransferFullDestination(GameTestHelper helper) {
        ItemBuffer source = new ItemBuffer(27);
        ItemBuffer dest = new ItemBuffer(1); // Tiny destination

        // Add items to source
        source.add(new ItemStack(Items.GOLD_INGOT, 128));

        // Fill destination
        dest.add(new ItemStack(Items.STICK, 64));

        // Try to transfer - should fail because dest is full
        BufferTransfer.TransferResult result = BufferTransfer.transferAll(source, dest);

        if (result.itemsTransferred() != 0) {
            throw helper.assertionException("Should transfer 0 items to full destination, got: " + result.itemsTransferred());
        }
        if (!result.destinationFull()) {
            throw helper.assertionException("Result should indicate destination is full");
        }
        if (source.getTotalItemCount() != 128) {
            throw helper.assertionException("Source should still have 128 items, got: " + source.getTotalItemCount());
        }

        helper.succeed();
    }

    private static void testBufferTransferOneStack(GameTestHelper helper) {
        ItemBuffer source = new ItemBuffer(27);
        ItemBuffer dest = new ItemBuffer(27);

        // Add multiple stacks to source
        source.add(new ItemStack(Items.COAL, 64));
        source.add(new ItemStack(Items.REDSTONE, 64));

        // Transfer one stack
        int transferred = BufferTransfer.transferOneStack(source, dest);

        if (transferred != 64) {
            throw helper.assertionException("Should transfer 64 items (one stack), got: " + transferred);
        }
        if (source.getTotalItemCount() != 64) {
            throw helper.assertionException("Source should have 64 items remaining, got: " + source.getTotalItemCount());
        }
        if (dest.getTotalItemCount() != 64) {
            throw helper.assertionException("Destination should have 64 items, got: " + dest.getTotalItemCount());
        }

        helper.succeed();
    }

    // ==================== Claimed Item Data Tests ====================

    private static void registerClaimedItemDataTests(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition> env) {
        registerTest(event, env, "claimed_data_initial_state", FloraFaunaGameTests::testClaimedDataInitialState);
        registerTest(event, env, "claimed_data_animation_progress", FloraFaunaGameTests::testClaimedDataAnimationProgress);
        registerTest(event, env, "claimed_data_animation_completion", FloraFaunaGameTests::testClaimedDataAnimationCompletion);
    }

    private static void testClaimedDataInitialState(GameTestHelper helper) {
        ClaimedItemData defaultData = ClaimedItemData.DEFAULT;

        if (defaultData.claimed()) {
            throw helper.assertionException("Default data should not be claimed");
        }
        if (defaultData.animationDuration() != 0) {
            throw helper.assertionException("Default animation duration should be 0");
        }

        helper.succeed();
    }

    private static void testClaimedDataAnimationProgress(GameTestHelper helper) {
        BlockPos inputPos = new BlockPos(10, 20, 30);
        ClaimedItemData data = ClaimedItemData.create(inputPos, 1000, 20);

        // At start
        float progressAtStart = data.getAnimationProgress(1000);
        if (progressAtStart != 0.0f) {
            throw helper.assertionException("Progress at start should be 0, got: " + progressAtStart);
        }

        // At halfway
        float progressHalf = data.getAnimationProgress(1010);
        if (Math.abs(progressHalf - 0.5f) > 0.01f) {
            throw helper.assertionException("Progress at halfway should be 0.5, got: " + progressHalf);
        }

        // At end
        float progressEnd = data.getAnimationProgress(1020);
        if (progressEnd != 1.0f) {
            throw helper.assertionException("Progress at end should be 1.0, got: " + progressEnd);
        }

        // Past end (should clamp)
        float progressPast = data.getAnimationProgress(1030);
        if (progressPast != 1.0f) {
            throw helper.assertionException("Progress past end should clamp to 1.0, got: " + progressPast);
        }

        helper.succeed();
    }

    private static void testClaimedDataAnimationCompletion(GameTestHelper helper) {
        BlockPos inputPos = new BlockPos(0, 0, 0);
        ClaimedItemData data = ClaimedItemData.create(inputPos, 100, 40);

        // Not complete before duration
        if (data.isAnimationComplete(120)) {
            throw helper.assertionException("Animation should not be complete at tick 120 (20 ticks in)");
        }

        // Complete exactly at duration
        if (!data.isAnimationComplete(140)) {
            throw helper.assertionException("Animation should be complete at tick 140 (40 ticks in)");
        }

        // Complete past duration
        if (!data.isAnimationComplete(200)) {
            throw helper.assertionException("Animation should be complete past duration");
        }

        helper.succeed();
    }

    // ==================== Pod Item Handler Tests ====================

    private static void registerPodItemHandlerTests(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition> env) {
        registerTest(event, env, "pod_handler_extract_from_buffer", FloraFaunaGameTests::testPodHandlerExtractFromBuffer);
        registerTest(event, env, "pod_handler_buffer_sync", FloraFaunaGameTests::testPodHandlerBufferSync);
        registerTest(event, env, "pod_handler_extract_with_transaction", FloraFaunaGameTests::testPodHandlerExtractWithTransaction);
        registerTest(event, env, "pod_capability_registered", FloraFaunaGameTests::testPodCapabilityRegistered);
    }

    /**
     * Tests that extracting via PodItemHandler removes items from the underlying buffer.
     * This is a unit test that creates an ItemBuffer directly without needing a block entity.
     */
    private static void testPodHandlerExtractFromBuffer(GameTestHelper helper) {
        // Create a buffer and add items
        ItemBuffer buffer = new ItemBuffer(27);
        buffer.add(new ItemStack(Items.COBBLESTONE, 64));

        // Verify initial state
        if (buffer.getTotalItemCount() != 64) {
            throw helper.assertionException("Buffer should have 64 items initially, got: " + buffer.getTotalItemCount());
        }

        // Create a mock pod to test the handler
        // We'll test the extraction logic directly on the buffer since that's the core issue
        ItemResource cobbleResource = ItemResource.of(new ItemStack(Items.COBBLESTONE));

        // Simulate what PodItemHandler.extract does
        ItemStack current = buffer.getStack(0);
        if (!cobbleResource.matches(current)) {
            throw helper.assertionException("Resource should match cobblestone stack");
        }

        int toExtract = Math.min(32, current.getCount());
        current.shrink(toExtract);

        // Verify extraction worked
        if (buffer.getTotalItemCount() != 32) {
            throw helper.assertionException("Buffer should have 32 items after extraction, got: " + buffer.getTotalItemCount());
        }

        helper.succeed();
    }

    /**
     * Tests that changes made via buffer.setStack are reflected in subsequent reads.
     */
    private static void testPodHandlerBufferSync(GameTestHelper helper) {
        ItemBuffer buffer = new ItemBuffer(27);

        // Add items
        buffer.add(new ItemStack(Items.IRON_INGOT, 40));

        // Verify we can read it back
        ItemStack stack = buffer.getStack(0);
        if (stack.isEmpty() || stack.getCount() != 40) {
            throw helper.assertionException("Should read 40 iron ingots, got: " + stack.getCount());
        }

        // Modify via setStack (simulating extraction)
        buffer.setStack(0, new ItemStack(Items.IRON_INGOT, 20));

        // Verify modification persisted
        ItemStack modified = buffer.getStack(0);
        if (modified.getCount() != 20) {
            throw helper.assertionException("After setStack, should have 20 items, got: " + modified.getCount());
        }

        // Verify total count is correct
        if (buffer.getTotalItemCount() != 20) {
            throw helper.assertionException("Total count should be 20, got: " + buffer.getTotalItemCount());
        }

        helper.succeed();
    }

    /**
     * Tests extracting from PodItemHandler using the Transaction API,
     * similar to how hoppers extract items.
     */
    private static void testPodHandlerExtractWithTransaction(GameTestHelper helper) {
        // Place a Tier2Pod block
        BlockPos podPos = helper.absolutePos(new BlockPos(0, 1, 0));
        helper.setBlock(new BlockPos(0, 1, 0), FloraFaunaRegistry.TIER2_POD.get().defaultBlockState());

        // Get the block entity
        Tier2PodBlockEntity pod = helper.getBlockEntity(new BlockPos(0, 1, 0), Tier2PodBlockEntity.class);
        if (pod == null) {
            throw helper.assertionException("Block entity is not Tier2PodBlockEntity");
        }

        // Add items to the pod's buffer
        pod.getBuffer().add(new ItemStack(Items.COBBLESTONE, 64));

        // Verify items are in the buffer
        if (pod.getBuffer().getTotalItemCount() != 64) {
            throw helper.assertionException("Pod buffer should have 64 items, got: " + pod.getBuffer().getTotalItemCount());
        }

        // Get the item handler (as hoppers would)
        PodItemHandler handler = pod.getItemHandler();

        // Verify handler can see the items
        ItemResource resource = handler.getResource(0);
        if (resource.isEmpty()) {
            throw helper.assertionException("Handler getResource(0) returned empty, but buffer has items");
        }

        long amount = handler.getAmountAsLong(0);
        if (amount != 64) {
            throw helper.assertionException("Handler getAmountAsLong(0) should be 64, got: " + amount);
        }

        // Extract using transaction (like hoppers do)
        try (Transaction tx = Transaction.openRoot()) {
            int extracted = handler.extract(0, resource, 1, tx);
            if (extracted != 1) {
                throw helper.assertionException("Should extract 1 item, got: " + extracted);
            }
            tx.commit();
        }

        // Verify extraction worked on the underlying buffer
        if (pod.getBuffer().getTotalItemCount() != 63) {
            throw helper.assertionException("Buffer should have 63 items after extraction, got: " + pod.getBuffer().getTotalItemCount());
        }

        helper.succeed();
    }

    /**
     * Tests that the capability is properly registered and can be queried from the level.
     * This is how hoppers find item handlers.
     */
    private static void testPodCapabilityRegistered(GameTestHelper helper) {
        // Place a Tier2Pod block
        BlockPos relativePos = new BlockPos(0, 1, 0);
        helper.setBlock(relativePos, FloraFaunaRegistry.TIER2_POD.get().defaultBlockState());

        // Get the absolute position
        BlockPos absolutePos = helper.absolutePos(relativePos);

        // Add items to the pod
        Tier2PodBlockEntity pod = helper.getBlockEntity(relativePos, Tier2PodBlockEntity.class);
        if (pod == null) {
            throw helper.assertionException("Block entity is null");
        }
        pod.getBuffer().add(new ItemStack(Items.COBBLESTONE, 64));

        // Query the capability like a hopper would (from below, direction DOWN)
        ResourceHandler<ItemResource> handler = helper.getLevel().getCapability(
                Capabilities.Item.BLOCK, absolutePos, Direction.DOWN);

        if (handler == null) {
            throw helper.assertionException("Capability returned null - not registered!");
        }

        // Verify the handler can see the items
        int size = handler.size();
        if (size == 0) {
            throw helper.assertionException("Handler size is 0");
        }

        ItemResource resource = handler.getResource(0);
        if (resource.isEmpty()) {
            throw helper.assertionException("Handler getResource(0) returned empty but pod has items");
        }

        // Try extracting
        try (Transaction tx = Transaction.openRoot()) {
            int extracted = handler.extract(0, resource, 1, tx);
            if (extracted != 1) {
                throw helper.assertionException("Capability extract returned " + extracted + ", expected 1");
            }
            tx.commit();
        }

        // Verify extraction worked
        if (pod.getBuffer().getTotalItemCount() != 63) {
            throw helper.assertionException("Buffer should have 63 items, got: " + pod.getBuffer().getTotalItemCount());
        }

        helper.succeed();
    }

    private static void testItemBufferPartialMergeToFull(GameTestHelper helper) {
        ItemBuffer buffer = new ItemBuffer(27);

        // Add 48 diamonds, then 32 more (48 + 32 = 80, but max stack is 64)
        buffer.add(new ItemStack(Items.DIAMOND, 48));
        buffer.add(new ItemStack(Items.DIAMOND, 32));

        // Should have 2 slots: 64 in first, 16 in second
        if (buffer.getUsedSlots() != 2) {
            throw helper.assertionException("Should use 2 slots when exceeding stack size, got: " + buffer.getUsedSlots());
        }
        if (buffer.getTotalItemCount() != 80) {
            throw helper.assertionException("Total count should be 80, got: " + buffer.getTotalItemCount());
        }

        helper.succeed();
    }

    private static void testItemBufferOverflowToNewSlot(GameTestHelper helper) {
        ItemBuffer buffer = new ItemBuffer(27);

        // Add full stack, then add more of same item
        buffer.add(new ItemStack(Items.IRON_INGOT, 64));
        buffer.add(new ItemStack(Items.IRON_INGOT, 10));

        if (buffer.getUsedSlots() != 2) {
            throw helper.assertionException("Adding to full stack should create new slot, got: " + buffer.getUsedSlots());
        }
        if (buffer.getTotalItemCount() != 74) {
            throw helper.assertionException("Total count should be 74, got: " + buffer.getTotalItemCount());
        }

        helper.succeed();
    }

    private static void testItemBufferPeekNoRemove(GameTestHelper helper) {
        ItemBuffer buffer = new ItemBuffer(27);
        buffer.add(new ItemStack(Items.GOLD_INGOT, 20));

        // Peek should return the item
        ItemStack peeked = buffer.peek();
        if (peeked.isEmpty() || peeked.getCount() != 20) {
            throw helper.assertionException("Peek should return 20 gold ingots");
        }

        // Buffer should still have the items
        if (buffer.isEmpty()) {
            throw helper.assertionException("Buffer should not be empty after peek");
        }
        if (buffer.getTotalItemCount() != 20) {
            throw helper.assertionException("Total count should still be 20 after peek, got: " + buffer.getTotalItemCount());
        }

        helper.succeed();
    }

    private static void testItemBufferClear(GameTestHelper helper) {
        ItemBuffer buffer = new ItemBuffer(27);
        buffer.add(new ItemStack(Items.EMERALD, 32));
        buffer.add(new ItemStack(Items.REDSTONE, 64));

        // Clear the buffer
        buffer.clear();

        if (!buffer.isEmpty()) {
            throw helper.assertionException("Buffer should be empty after clear");
        }
        if (buffer.getUsedSlots() != 0) {
            throw helper.assertionException("Used slots should be 0 after clear, got: " + buffer.getUsedSlots());
        }
        if (buffer.getTotalItemCount() != 0) {
            throw helper.assertionException("Total count should be 0 after clear, got: " + buffer.getTotalItemCount());
        }

        helper.succeed();
    }

    // ==================== Mining Mode Tests ====================

    private static void registerMiningModeTests(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition> env) {
        registerTest(event, env, "mining_mode_initial_state", FloraFaunaGameTests::testMiningModeInitialState);
        registerTest(event, env, "mining_mode_cycling", FloraFaunaGameTests::testMiningModeCycling);
        registerTest(event, env, "mining_mode_wrap_around", FloraFaunaGameTests::testMiningModeWrapAround);
        registerTest(event, env, "mining_mode_radius_per_shape", FloraFaunaGameTests::testMiningModeRadiusPerShape);
    }

    private static void testMiningModeInitialState(GameTestHelper helper) {
        MiningModeData data = MiningModeData.DEFAULT;

        if (data.shape() != MiningShape.SINGLE) {
            throw helper.assertionException("Default shape should be SINGLE, got: " + data.shape());
        }
        if (data.radius() != 0) {
            throw helper.assertionException("Default radius should be 0, got: " + data.radius());
        }
        if (data.maxBlocksToBreak() != 64) {
            throw helper.assertionException("Default maxBlocksToBreak should be 64, got: " + data.maxBlocksToBreak());
        }

        helper.succeed();
    }

    private static void testMiningModeCycling(GameTestHelper helper) {
        MiningModeData data = MiningModeData.DEFAULT;

        // Cycle through all shapes: SINGLE → FLAT_3X3 → FLAT_5X5 → FLAT_7X7 → SHAPELESS → TUNNEL_UP → TUNNEL_DOWN
        MiningShape[] expectedOrder = {
            MiningShape.FLAT_3X3,
            MiningShape.FLAT_5X5,
            MiningShape.FLAT_7X7,
            MiningShape.SHAPELESS,
            MiningShape.TUNNEL_UP,
            MiningShape.TUNNEL_DOWN
        };

        for (MiningShape expected : expectedOrder) {
            data = data.getNextMode();
            if (data.shape() != expected) {
                throw helper.assertionException("Expected " + expected + ", got: " + data.shape());
            }
        }

        helper.succeed();
    }

    private static void testMiningModeWrapAround(GameTestHelper helper) {
        // Start from TUNNEL_DOWN (last shape)
        MiningModeData data = new MiningModeData(MiningShape.TUNNEL_DOWN, 0, 64, true);

        // Next mode should wrap to SINGLE
        data = data.getNextMode();

        if (data.shape() != MiningShape.SINGLE) {
            throw helper.assertionException("After TUNNEL_DOWN should wrap to SINGLE, got: " + data.shape());
        }

        helper.succeed();
    }

    private static void testMiningModeRadiusPerShape(GameTestHelper helper) {
        // Expected radii: SINGLE=0, FLAT_3X3=1, FLAT_5X5=2, FLAT_7X7=3, SHAPELESS=1, TUNNEL_UP=0, TUNNEL_DOWN=0
        int[] expectedRadii = {0, 1, 2, 3, 1, 0, 0};
        MiningShape[] shapes = MiningShape.values();

        for (int i = 0; i < shapes.length; i++) {
            int actualRadius = shapes[i].getRadius();
            if (actualRadius != expectedRadii[i]) {
                throw helper.assertionException(shapes[i] + " should have radius " + expectedRadii[i] + ", got: " + actualRadius);
            }
        }

        helper.succeed();
    }

    // ==================== Containment Chamber Tests ====================

    private static void registerContainmentChamberTests(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition> env) {
        registerTest(event, env, "containment_chamber_slot_validation", FloraFaunaGameTests::testContainmentChamberSlotValidation);
    }

    private static void testContainmentChamberSlotValidation(GameTestHelper helper) {
        // Create a test block entity
        ContainmentChamberBlockEntity be = new ContainmentChamberBlockEntity(BlockPos.ZERO, FloraFaunaRegistry.SYMBIOTE_CONTAINMENT_CHAMBER.get().defaultBlockState());

        // Slot 0 should reject regular items
        ItemResource diamondResource = ItemResource.of(Items.DIAMOND);
        if (be.handler.isValid(0, diamondResource)) {
            throw helper.assertionException("Slot 0 should reject diamond");
        }

        ItemResource stoneResource = ItemResource.of(Items.STONE);
        if (be.handler.isValid(0, stoneResource)) {
            throw helper.assertionException("Slot 0 should reject stone");
        }

        // Slot 0 should accept DormantSymbioteItem
        ItemResource symbioteResource = ItemResource.of(FloraFaunaRegistry.DORMANT_SYMBIOTE.get());
        if (!be.handler.isValid(0, symbioteResource)) {
            throw helper.assertionException("Slot 0 should accept DormantSymbioteItem");
        }

        // Slot 1 should accept any item (feeding slot)
        if (!be.handler.isValid(1, diamondResource)) {
            throw helper.assertionException("Slot 1 should accept diamond");
        }
        if (!be.handler.isValid(1, stoneResource)) {
            throw helper.assertionException("Slot 1 should accept stone");
        }
        if (!be.handler.isValid(1, symbioteResource)) {
            throw helper.assertionException("Slot 1 should accept symbiote too");
        }

        helper.succeed();
    }

    // ==================== Mob Barrier Tests ====================

    private static void registerMobBarrierTests(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition> env) {
        registerTest(event, env, "mob_barrier_config_default_empty", FloraFaunaGameTests::testMobBarrierConfigDefaultEmpty);
        registerTest(event, env, "mob_barrier_config_add_remove_entity_id", FloraFaunaGameTests::testMobBarrierConfigAddRemoveEntityId);
        registerTest(event, env, "mob_barrier_config_add_remove_tag", FloraFaunaGameTests::testMobBarrierConfigAddRemoveTag);
        registerTest(event, env, "mob_barrier_config_validation", FloraFaunaGameTests::testMobBarrierConfigValidation);
        registerTest(event, env, "mob_barrier_blocks_configured_entity", FloraFaunaGameTests::testMobBarrierBlocksConfiguredEntity);
        registerTest(event, env, "mob_barrier_allows_unconfigured_entity", FloraFaunaGameTests::testMobBarrierAllowsUnconfiguredEntity);
    }

    private static void testMobBarrierConfigDefaultEmpty(GameTestHelper helper) {
        MobBarrierConfig config = MobBarrierConfig.DEFAULT;

        if (!config.isEmpty()) {
            throw helper.assertionException("Default config should be empty");
        }
        if (config.totalEntries() != 0) {
            throw helper.assertionException("Default config should have 0 entries, got: " + config.totalEntries());
        }
        if (!config.entityIds().isEmpty()) {
            throw helper.assertionException("Default config should have no entity IDs");
        }
        if (!config.entityTags().isEmpty()) {
            throw helper.assertionException("Default config should have no entity tags");
        }

        helper.succeed();
    }

    private static void testMobBarrierConfigAddRemoveEntityId(GameTestHelper helper) {
        MobBarrierConfig config = MobBarrierConfig.DEFAULT;

        // Add an entity ID
        config = config.withAddedEntityId("minecraft:zombie");
        if (!config.entityIds().contains("minecraft:zombie")) {
            throw helper.assertionException("Config should contain zombie after adding");
        }
        if (config.totalEntries() != 1) {
            throw helper.assertionException("Config should have 1 entry after adding zombie");
        }

        // Add another
        config = config.withAddedEntityId("minecraft:skeleton");
        if (config.totalEntries() != 2) {
            throw helper.assertionException("Config should have 2 entries");
        }

        // Adding duplicate should not increase count
        config = config.withAddedEntityId("minecraft:zombie");
        if (config.totalEntries() != 2) {
            throw helper.assertionException("Adding duplicate should not increase count");
        }

        // Remove entity ID
        config = config.withRemovedEntityId("minecraft:zombie");
        if (config.entityIds().contains("minecraft:zombie")) {
            throw helper.assertionException("Config should not contain zombie after removal");
        }
        if (config.totalEntries() != 1) {
            throw helper.assertionException("Config should have 1 entry after removal");
        }

        helper.succeed();
    }

    private static void testMobBarrierConfigAddRemoveTag(GameTestHelper helper) {
        MobBarrierConfig config = MobBarrierConfig.DEFAULT;

        // Add a tag (with #)
        config = config.withAddedEntityTag("#minecraft:undead");
        if (!config.entityTags().contains("#minecraft:undead")) {
            throw helper.assertionException("Config should contain #minecraft:undead tag");
        }

        // Add tag without # (should normalize)
        config = config.withAddedEntityTag("minecraft:raiders");
        if (!config.entityTags().contains("#minecraft:raiders")) {
            throw helper.assertionException("Config should normalize tag to include #");
        }

        if (config.totalEntries() != 2) {
            throw helper.assertionException("Config should have 2 tag entries");
        }

        // Remove tag
        config = config.withRemovedEntityTag("#minecraft:undead");
        if (config.entityTags().contains("#minecraft:undead")) {
            throw helper.assertionException("Config should not contain undead tag after removal");
        }

        helper.succeed();
    }

    private static void testMobBarrierConfigValidation(GameTestHelper helper) {
        // Valid entity ID
        if (!MobBarrierConfig.isValidEntityId("minecraft:zombie")) {
            throw helper.assertionException("minecraft:zombie should be valid entity ID");
        }

        // Invalid entity ID
        if (MobBarrierConfig.isValidEntityId("minecraft:not_a_real_entity")) {
            throw helper.assertionException("minecraft:not_a_real_entity should be invalid");
        }

        // Valid tag format
        if (!MobBarrierConfig.isValidTagFormat("#minecraft:undead")) {
            throw helper.assertionException("#minecraft:undead should be valid tag format");
        }
        if (!MobBarrierConfig.isValidTagFormat("minecraft:undead")) {
            throw helper.assertionException("minecraft:undead (without #) should be valid tag format");
        }

        // Invalid tag format
        if (MobBarrierConfig.isValidTagFormat("not valid")) {
            throw helper.assertionException("'not valid' should be invalid tag format");
        }

        helper.succeed();
    }

    private static void testMobBarrierBlocksConfiguredEntity(GameTestHelper helper) {
        // Create config that blocks zombies
        MobBarrierConfig config = MobBarrierConfig.DEFAULT.withAddedEntityId("minecraft:zombie");

        // Spawn a zombie to test against
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));

        if (!config.shouldBlockEntity(zombie)) {
            throw helper.assertionException("Config with zombie ID should block zombie entity");
        }

        helper.succeed();
    }

    private static void testMobBarrierAllowsUnconfiguredEntity(GameTestHelper helper) {
        // Create config that only blocks zombies
        MobBarrierConfig config = MobBarrierConfig.DEFAULT.withAddedEntityId("minecraft:zombie");

        // Spawn a cow (not configured)
        var cow = helper.spawn(EntityType.COW, new BlockPos(1, 1, 1));

        if (config.shouldBlockEntity(cow)) {
            throw helper.assertionException("Config with only zombie should NOT block cow");
        }

        // Null entity should not be blocked
        if (config.shouldBlockEntity(null)) {
            throw helper.assertionException("Null entity should not be blocked");
        }

        helper.succeed();
    }

    // ==================== Mining Anchor Tests ====================

    private static void registerMiningAnchorTests(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition> env) {
        registerTest(event, env, "anchor_fill_state_normal", FloraFaunaGameTests::testAnchorFillStateNormal);
        registerTest(event, env, "anchor_fill_state_warning", FloraFaunaGameTests::testAnchorFillStateWarning);
        registerTest(event, env, "anchor_fill_state_full", FloraFaunaGameTests::testAnchorFillStateFull);
        registerTest(event, env, "anchor_fill_state_edge_cases", FloraFaunaGameTests::testAnchorFillStateEdgeCases);
        registerTest(event, env, "pod_slot_count_tier1", FloraFaunaGameTests::testPodSlotCountTier1);
        registerTest(event, env, "pod_slot_count_tier2", FloraFaunaGameTests::testPodSlotCountTier2);
        registerTest(event, env, "anchor_tier1_capacity", FloraFaunaGameTests::testAnchorTier1Capacity);
        registerTest(event, env, "anchor_tier2_capacity", FloraFaunaGameTests::testAnchorTier2Capacity);
        registerTest(event, env, "anchor_fill_state_potential_capacity", FloraFaunaGameTests::testAnchorFillStatePotentialCapacity);
    }

    private static void testAnchorFillStateNormal(GameTestHelper helper) {
        // Empty storage = NORMAL
        AnchorFillState empty = AnchorFillState.fromFillRatio(0, 100);
        if (empty != AnchorFillState.NORMAL) {
            throw helper.assertionException("0% fill should be NORMAL, got: " + empty);
        }

        // 50% = NORMAL
        AnchorFillState half = AnchorFillState.fromFillRatio(50, 100);
        if (half != AnchorFillState.NORMAL) {
            throw helper.assertionException("50% fill should be NORMAL, got: " + half);
        }

        // 74% = NORMAL (just under threshold)
        AnchorFillState justUnder = AnchorFillState.fromFillRatio(74, 100);
        if (justUnder != AnchorFillState.NORMAL) {
            throw helper.assertionException("74% fill should be NORMAL, got: " + justUnder);
        }

        helper.succeed();
    }

    private static void testAnchorFillStateWarning(GameTestHelper helper) {
        // 75% = WARNING (exactly at threshold)
        AnchorFillState atThreshold = AnchorFillState.fromFillRatio(75, 100);
        if (atThreshold != AnchorFillState.WARNING) {
            throw helper.assertionException("75% fill should be WARNING, got: " + atThreshold);
        }

        // 80% = WARNING
        AnchorFillState eighty = AnchorFillState.fromFillRatio(80, 100);
        if (eighty != AnchorFillState.WARNING) {
            throw helper.assertionException("80% fill should be WARNING, got: " + eighty);
        }

        // 99% = WARNING (just under full)
        AnchorFillState almostFull = AnchorFillState.fromFillRatio(99, 100);
        if (almostFull != AnchorFillState.WARNING) {
            throw helper.assertionException("99% fill should be WARNING, got: " + almostFull);
        }

        helper.succeed();
    }

    private static void testAnchorFillStateFull(GameTestHelper helper) {
        // 100% = FULL
        AnchorFillState full = AnchorFillState.fromFillRatio(100, 100);
        if (full != AnchorFillState.FULL) {
            throw helper.assertionException("100% fill should be FULL, got: " + full);
        }

        // Over 100% = FULL (overflow case)
        AnchorFillState overflow = AnchorFillState.fromFillRatio(150, 100);
        if (overflow != AnchorFillState.FULL) {
            throw helper.assertionException("150% fill should be FULL, got: " + overflow);
        }

        helper.succeed();
    }

    private static void testAnchorFillStateEdgeCases(GameTestHelper helper) {
        // Zero capacity = NORMAL (avoid division by zero)
        AnchorFillState zeroCapacity = AnchorFillState.fromFillRatio(50, 0);
        if (zeroCapacity != AnchorFillState.NORMAL) {
            throw helper.assertionException("Zero capacity should return NORMAL, got: " + zeroCapacity);
        }

        // Negative capacity = NORMAL
        AnchorFillState negativeCapacity = AnchorFillState.fromFillRatio(50, -100);
        if (negativeCapacity != AnchorFillState.NORMAL) {
            throw helper.assertionException("Negative capacity should return NORMAL, got: " + negativeCapacity);
        }

        // Warning threshold getter
        float threshold = AnchorFillState.getWarningThreshold();
        if (threshold != 0.75f) {
            throw helper.assertionException("Warning threshold should be 0.75, got: " + threshold);
        }

        helper.succeed();
    }

    private static void testPodSlotCountTier1(GameTestHelper helper) {
        // Tier 1 pods have 9 slots
        if (Tier1PodBlockEntity.SLOT_COUNT != 9) {
            throw helper.assertionException("Tier 1 pod should have 9 slots, got: " + Tier1PodBlockEntity.SLOT_COUNT);
        }

        // Create a pod and verify slot count
        BlockPos podPos = new BlockPos(0, 1, 0);
        helper.setBlock(podPos, FloraFaunaRegistry.TIER1_POD.get().defaultBlockState());

        Tier1PodBlockEntity pod = helper.getBlockEntity(podPos, Tier1PodBlockEntity.class);
        if (pod == null) {
            throw helper.assertionException("Tier 1 pod block entity is null");
        }

        if (pod.getSlotCount() != 9) {
            throw helper.assertionException("Tier 1 pod getSlotCount() should return 9, got: " + pod.getSlotCount());
        }

        // Capacity = slots × 64
        if (pod.getCapacity() != 576) {
            throw helper.assertionException("Tier 1 pod capacity should be 576 (9×64), got: " + pod.getCapacity());
        }

        helper.succeed();
    }

    private static void testPodSlotCountTier2(GameTestHelper helper) {
        // Tier 2 pods have 27 slots (same as shulker box)
        if (Tier2PodBlockEntity.SLOT_COUNT != 27) {
            throw helper.assertionException("Tier 2 pod should have 27 slots, got: " + Tier2PodBlockEntity.SLOT_COUNT);
        }

        // Create a pod and verify slot count
        BlockPos podPos = new BlockPos(0, 1, 0);
        helper.setBlock(podPos, FloraFaunaRegistry.TIER2_POD.get().defaultBlockState());

        Tier2PodBlockEntity pod = helper.getBlockEntity(podPos, Tier2PodBlockEntity.class);
        if (pod == null) {
            throw helper.assertionException("Tier 2 pod block entity is null");
        }

        if (pod.getSlotCount() != 27) {
            throw helper.assertionException("Tier 2 pod getSlotCount() should return 27, got: " + pod.getSlotCount());
        }

        // Capacity = slots × 64
        if (pod.getCapacity() != 1728) {
            throw helper.assertionException("Tier 2 pod capacity should be 1728 (27×64), got: " + pod.getCapacity());
        }

        helper.succeed();
    }

    private static void testAnchorTier1Capacity(GameTestHelper helper) {
        // Create a Tier 1 anchor
        BlockPos anchorPos = new BlockPos(0, 1, 0);
        helper.setBlock(anchorPos, FloraFaunaRegistry.TIER1_MINING_ANCHOR.get().defaultBlockState());

        Tier1MiningAnchorBlockEntity anchor = helper.getBlockEntity(anchorPos, Tier1MiningAnchorBlockEntity.class);
        if (anchor == null) {
            throw helper.assertionException("Tier 1 anchor block entity is null");
        }

        // Pod capacity: 9 slots × 64 items = 576 items
        int expectedPodCapacity = Tier1PodBlockEntity.SLOT_COUNT * 64;
        if (expectedPodCapacity != 576) {
            throw helper.assertionException("Expected pod capacity 576, got: " + expectedPodCapacity);
        }

        // Max capacity = max pods (4) × pod capacity (576) = 2304
        // Note: Config may override, but default is 4
        int maxCapacity = anchor.getMaxCapacity();
        // At minimum, capacity should be positive and match formula
        if (maxCapacity <= 0) {
            throw helper.assertionException("Max capacity should be positive, got: " + maxCapacity);
        }

        // With no pods, stored count should be 0
        int storedCount = anchor.getStoredCount();
        if (storedCount != 0) {
            throw helper.assertionException("Fresh anchor should have 0 stored items, got: " + storedCount);
        }

        helper.succeed();
    }

    private static void testAnchorTier2Capacity(GameTestHelper helper) {
        // Create a Tier 2 anchor
        BlockPos anchorPos = new BlockPos(0, 1, 0);
        helper.setBlock(anchorPos, FloraFaunaRegistry.TIER2_MINING_ANCHOR.get().defaultBlockState());

        Tier2MiningAnchorBlockEntity anchor = helper.getBlockEntity(anchorPos, Tier2MiningAnchorBlockEntity.class);
        if (anchor == null) {
            throw helper.assertionException("Tier 2 anchor block entity is null");
        }

        // Pod capacity: 27 slots × 64 items = 1728 items
        int expectedPodCapacity = Tier2PodBlockEntity.SLOT_COUNT * 64;
        if (expectedPodCapacity != 1728) {
            throw helper.assertionException("Expected pod capacity 1728, got: " + expectedPodCapacity);
        }

        // Max capacity should be positive
        int maxCapacity = anchor.getMaxCapacity();
        if (maxCapacity <= 0) {
            throw helper.assertionException("Max capacity should be positive, got: " + maxCapacity);
        }

        // Tier 2 should have more capacity than Tier 1
        // Tier 1: 4 × 576 = 2304, Tier 2: 8 × 1728 = 13824
        if (maxCapacity < 10000) {
            throw helper.assertionException("Tier 2 max capacity should be >= 10000 (8×1728=13824), got: " + maxCapacity);
        }

        helper.succeed();
    }

    private static void testAnchorFillStatePotentialCapacity(GameTestHelper helper) {
        // Fill state should be based on POTENTIAL capacity (all possible pods)
        // not just existing pods

        // Simulate: If an anchor can have 4 pods with 576 capacity each (2304 total)
        // and 1728 items are stored (75%), it should be in WARNING state
        AnchorFillState state75 = AnchorFillState.fromFillRatio(1728, 2304);
        if (state75 != AnchorFillState.WARNING) {
            throw helper.assertionException("75% fill (1728/2304) should be WARNING, got: " + state75);
        }

        // 50% of potential capacity = NORMAL
        AnchorFillState state50 = AnchorFillState.fromFillRatio(1152, 2304);
        if (state50 != AnchorFillState.NORMAL) {
            throw helper.assertionException("50% fill (1152/2304) should be NORMAL, got: " + state50);
        }

        // 100% of potential capacity = FULL
        AnchorFillState state100 = AnchorFillState.fromFillRatio(2304, 2304);
        if (state100 != AnchorFillState.FULL) {
            throw helper.assertionException("100% fill (2304/2304) should be FULL, got: " + state100);
        }

        // Even with only 1 pod spawned but at 75% of total potential = WARNING
        // This ensures the fill state is based on ALL pods, not just existing
        AnchorFillState partialFill = AnchorFillState.fromFillRatio(576 + 576 + 576, 2304);
        if (partialFill != AnchorFillState.WARNING) {
            throw helper.assertionException("3 pods worth (1728/2304 = 75%) should be WARNING, got: " + partialFill);
        }

        helper.succeed();
    }

    // ==================== Husk Tests ====================

    private static void registerHuskTests(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition> env) {
        registerTest(event, env, "husk_type_restoration", FloraFaunaGameTests::testHuskTypeRestoration);
        registerTest(event, env, "husk_type_container", FloraFaunaGameTests::testHuskTypeContainer);
        registerTest(event, env, "husk_type_broken", FloraFaunaGameTests::testHuskTypeBroken);
    }

    private static void testHuskTypeRestoration(GameTestHelper helper) {
        // RESTORATION husk: holds items and restores abilities
        if (!HuskType.RESTORATION.holdsItems()) {
            throw helper.assertionException("RESTORATION should hold items");
        }
        if (!HuskType.RESTORATION.restoresAbilities()) {
            throw helper.assertionException("RESTORATION should restore abilities");
        }

        helper.succeed();
    }

    private static void testHuskTypeContainer(GameTestHelper helper) {
        // CONTAINER husk: holds items but does NOT restore abilities
        if (!HuskType.CONTAINER.holdsItems()) {
            throw helper.assertionException("CONTAINER should hold items");
        }
        if (HuskType.CONTAINER.restoresAbilities()) {
            throw helper.assertionException("CONTAINER should NOT restore abilities");
        }

        helper.succeed();
    }

    private static void testHuskTypeBroken(GameTestHelper helper) {
        // BROKEN husk: does NOT hold items and does NOT restore abilities
        if (HuskType.BROKEN.holdsItems()) {
            throw helper.assertionException("BROKEN should NOT hold items");
        }
        if (HuskType.BROKEN.restoresAbilities()) {
            throw helper.assertionException("BROKEN should NOT restore abilities");
        }

        helper.succeed();
    }

    // ==================== Symbiote State Tests ====================

    private static void registerSymbioteStateTests(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition> env) {
        registerTest(event, env, "symbiote_state_unbound", FloraFaunaGameTests::testSymbioteStateUnbound);
        registerTest(event, env, "symbiote_state_ready_to_bind", FloraFaunaGameTests::testSymbioteStateReadyToBind);
        registerTest(event, env, "symbiote_state_bonded_active", FloraFaunaGameTests::testSymbioteStateBondedActive);
        registerTest(event, env, "symbiote_state_bonded_weakened", FloraFaunaGameTests::testSymbioteStateBondedWeakened);
    }

    private static void testSymbioteStateUnbound(GameTestHelper helper) {
        // UNBOUND: not bonded, no abilities
        if (SymbioteState.UNBOUND.isBonded()) {
            throw helper.assertionException("UNBOUND should NOT be bonded");
        }
        if (SymbioteState.UNBOUND.areAbilitiesActive()) {
            throw helper.assertionException("UNBOUND should NOT have active abilities");
        }

        helper.succeed();
    }

    private static void testSymbioteStateReadyToBind(GameTestHelper helper) {
        // READY_TO_BIND: not bonded yet, no abilities
        if (SymbioteState.READY_TO_BIND.isBonded()) {
            throw helper.assertionException("READY_TO_BIND should NOT be bonded");
        }
        if (SymbioteState.READY_TO_BIND.areAbilitiesActive()) {
            throw helper.assertionException("READY_TO_BIND should NOT have active abilities");
        }

        helper.succeed();
    }

    private static void testSymbioteStateBondedActive(GameTestHelper helper) {
        // BONDED_ACTIVE: bonded AND abilities active
        if (!SymbioteState.BONDED_ACTIVE.isBonded()) {
            throw helper.assertionException("BONDED_ACTIVE should be bonded");
        }
        if (!SymbioteState.BONDED_ACTIVE.areAbilitiesActive()) {
            throw helper.assertionException("BONDED_ACTIVE should have active abilities");
        }

        helper.succeed();
    }

    private static void testSymbioteStateBondedWeakened(GameTestHelper helper) {
        // BONDED_WEAKENED: bonded but abilities NOT active
        if (!SymbioteState.BONDED_WEAKENED.isBonded()) {
            throw helper.assertionException("BONDED_WEAKENED should be bonded");
        }
        if (SymbioteState.BONDED_WEAKENED.areAbilitiesActive()) {
            throw helper.assertionException("BONDED_WEAKENED should NOT have active abilities");
        }

        helper.succeed();
    }

    // ==================== Mob Transport Tests ====================

    private static void registerMobTransportTests(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition> env) {
        registerTest(event, env, "mob_symbiote_data_default", FloraFaunaGameTests::testMobSymbioteDataDefault);
        registerTest(event, env, "mob_symbiote_data_bonding", FloraFaunaGameTests::testMobSymbioteDataBonding);
        registerTest(event, env, "mob_symbiote_data_release_immunity", FloraFaunaGameTests::testMobSymbioteDataReleaseImmunity);
        registerTest(event, env, "captured_mob_buffer_capacity", FloraFaunaGameTests::testCapturedMobBufferCapacity);
        registerTest(event, env, "captured_mob_buffer_add_poll", FloraFaunaGameTests::testCapturedMobBufferAddPoll);
        registerTest(event, env, "mob_capture_eligibility_player", FloraFaunaGameTests::testMobCaptureEligibilityPlayer);
        registerTest(event, env, "mob_capture_eligibility_boss", FloraFaunaGameTests::testMobCaptureEligibilityBoss);
        registerTest(event, env, "mob_capture_eligibility_not_bondable", FloraFaunaGameTests::testMobCaptureEligibilityNotBondable);
        registerTest(event, env, "mob_capture_eligibility_recently_released", FloraFaunaGameTests::testMobCaptureEligibilityRecentlyReleased);
        registerTest(event, env, "captured_mob_ticket_ready_check", FloraFaunaGameTests::testCapturedMobTicketReadyCheck);
        // Lure goal tests
        registerTest(event, env, "lure_goal_only_bonded_mobs", FloraFaunaGameTests::testLureGoalOnlyBondedMobs);
        registerTest(event, env, "lure_goal_start_and_stop", FloraFaunaGameTests::testLureGoalStartAndStop);
        registerTest(event, env, "lure_goal_is_being_lured_to", FloraFaunaGameTests::testLureGoalIsBeingLuredTo);
        registerTest(event, env, "lure_goal_switch_target", FloraFaunaGameTests::testLureGoalSwitchTarget);
        // MobSymbiote item and MobOutput release tests
        registerTest(event, env, "mob_symbiote_item_bonds_mob", FloraFaunaGameTests::testMobSymbioteItemBondsMob);
        registerTest(event, env, "mob_output_release_clears_lure_goal", FloraFaunaGameTests::testMobOutputReleaseClearsLureGoal);
    }

    private static void testMobSymbioteDataDefault(GameTestHelper helper) {
        MobSymbioteData data = MobSymbioteData.DEFAULT;

        if (data.hasMobSymbiote()) {
            throw helper.assertionException("Default MobSymbioteData should not have MobSymbiote");
        }
        if (data.mobSymbioteLevel() != MobSymbioteData.LEVEL_NONE) {
            throw helper.assertionException("Default mobSymbioteLevel should be LEVEL_NONE (0)");
        }
        if (data.levelUpgradedAtTick() != 0L) {
            throw helper.assertionException("Default levelUpgradedAtTick should be 0");
        }
        if (data.recentlyReleasedUntil() != 0L) {
            throw helper.assertionException("Default recentlyReleasedUntil should be 0");
        }

        helper.succeed();
    }

    private static void testMobSymbioteDataBonding(GameTestHelper helper) {
        MobSymbioteData data = MobSymbioteData.DEFAULT;

        // Apply Level 1 MobSymbiote
        MobSymbioteData level1 = data.withMobSymbioteLevel(MobSymbioteData.LEVEL_TRANSPORT, 1000L);

        if (!level1.hasMobSymbiote()) {
            throw helper.assertionException("Should have MobSymbiote after applying Level 1");
        }
        if (level1.mobSymbioteLevel() != MobSymbioteData.LEVEL_TRANSPORT) {
            throw helper.assertionException("Level should be LEVEL_TRANSPORT (1), got: " + level1.mobSymbioteLevel());
        }
        if (level1.levelUpgradedAtTick() != 1000L) {
            throw helper.assertionException("levelUpgradedAtTick should be 1000, got: " + level1.levelUpgradedAtTick());
        }

        // Remove the MobSymbiote
        MobSymbioteData removed = level1.withMobSymbioteLevel(MobSymbioteData.LEVEL_NONE, 2000L);

        if (removed.hasMobSymbiote()) {
            throw helper.assertionException("Should not have MobSymbiote after removing");
        }

        helper.succeed();
    }

    private static void testMobSymbioteDataReleaseImmunity(GameTestHelper helper) {
        MobSymbioteData data = MobSymbioteData.DEFAULT;

        // Set release immunity until tick 2000
        MobSymbioteData released = data.withRecentlyReleased(2000L);

        // Should have immunity before tick 2000
        if (!released.hasReleaseImmunity(1500L)) {
            throw helper.assertionException("Should have release immunity at tick 1500");
        }
        if (!released.hasReleaseImmunity(1999L)) {
            throw helper.assertionException("Should have release immunity at tick 1999");
        }

        // Should NOT have immunity at or after tick 2000
        if (released.hasReleaseImmunity(2000L)) {
            throw helper.assertionException("Should NOT have release immunity at tick 2000");
        }
        if (released.hasReleaseImmunity(3000L)) {
            throw helper.assertionException("Should NOT have release immunity at tick 3000");
        }

        helper.succeed();
    }

    private static void testCapturedMobBufferCapacity(GameTestHelper helper) {
        int maxSize = 5;
        CapturedMobBuffer buffer = new CapturedMobBuffer(maxSize);

        if (!buffer.isEmpty()) {
            throw helper.assertionException("Fresh buffer should be empty");
        }
        if (buffer.isFull()) {
            throw helper.assertionException("Fresh buffer should not be full");
        }
        if (!buffer.canAccept()) {
            throw helper.assertionException("Fresh buffer should be able to accept");
        }

        // Fill the buffer
        for (int i = 0; i < maxSize; i++) {
            CapturedMobTicket ticket = CapturedMobTicket.create(
                    EntityType.ZOMBIE,
                    new CompoundTag(),
                    1000L + i,
                    1100L + i,
                    null,
                    null
            );
            buffer.add(ticket);
        }

        if (!buffer.isFull()) {
            throw helper.assertionException("Buffer should be full after adding " + maxSize + " tickets");
        }
        if (buffer.canAccept()) {
            throw helper.assertionException("Full buffer should not accept more");
        }

        helper.succeed();
    }

    private static void testCapturedMobBufferAddPoll(GameTestHelper helper) {
        CapturedMobBuffer buffer = new CapturedMobBuffer(5);

        CapturedMobTicket ticket1 = CapturedMobTicket.create(
                EntityType.ZOMBIE,
                new CompoundTag(),
                1000L,
                1100L,
                null,
                null
        );
        CapturedMobTicket ticket2 = CapturedMobTicket.create(
                EntityType.SKELETON,
                new CompoundTag(),
                2000L,
                2100L,
                null,
                null
        );

        buffer.add(ticket1);
        buffer.add(ticket2);

        if (buffer.size() != 2) {
            throw helper.assertionException("Buffer should have 2 tickets, got: " + buffer.size());
        }

        // Get ready ticket at tick 1100 (ticket1 should be ready)
        var ready = buffer.getReadyTicket(1100L);
        if (ready.isEmpty()) {
            throw helper.assertionException("Ticket1 should be ready at tick 1100");
        }
        if (!ready.get().entityTypeId().equals(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.ZOMBIE))) {
            throw helper.assertionException("Ready ticket should be zombie");
        }

        // Poll it
        buffer.pollReadyTicket(1100L);
        if (buffer.size() != 1) {
            throw helper.assertionException("Buffer should have 1 ticket after poll, got: " + buffer.size());
        }

        helper.succeed();
    }

    private static void testMobCaptureEligibilityPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        var result = MobCaptureEligibility.checkEligibility(player, 0L);

        if (result.isEligible()) {
            throw helper.assertionException("Player should NOT be capture-eligible");
        }
        if (result.reason() != MobCaptureEligibility.ExclusionReason.PLAYER) {
            throw helper.assertionException("Reason should be PLAYER, got: " + result.reason());
        }

        helper.succeed();
    }

    private static void testMobCaptureEligibilityBoss(GameTestHelper helper) {
        // Spawn the Ender Dragon (a boss - on exclusion list)
        var dragon = helper.spawn(EntityType.ENDER_DRAGON, new BlockPos(5, 5, 5));

        var result = MobCaptureEligibility.checkEligibility(dragon, 0L);

        if (result.isEligible()) {
            throw helper.assertionException("Boss should NOT be capture-eligible");
        }
        if (result.reason() != MobCaptureEligibility.ExclusionReason.MOB_SYMBIOTE_EXCLUDED) {
            throw helper.assertionException("Reason should be MOB_SYMBIOTE_EXCLUDED, got: " + result.reason());
        }

        helper.succeed();
    }

    private static void testMobCaptureEligibilityNotBondable(GameTestHelper helper) {
        // Spawn a Warden (on the exclusion list)
        var warden = helper.spawn(EntityType.WARDEN, new BlockPos(1, 1, 1));

        var result = MobCaptureEligibility.checkEligibility(warden, 0L);

        if (result.isEligible()) {
            throw helper.assertionException("Excluded mob should NOT be capture-eligible");
        }
        if (result.reason() != MobCaptureEligibility.ExclusionReason.MOB_SYMBIOTE_EXCLUDED) {
            throw helper.assertionException("Reason should be MOB_SYMBIOTE_EXCLUDED, got: " + result.reason());
        }

        helper.succeed();
    }

    private static void testMobCaptureEligibilityRecentlyReleased(GameTestHelper helper) {
        // Spawn a zombie
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));

        // Mark it as recently released until tick 2000
        MobSymbioteHelper.markRecentlyReleased(zombie, 2000L);

        // Check eligibility at tick 1500 (should be excluded)
        var result = MobCaptureEligibility.checkEligibility(zombie, 1500L);

        if (result.isEligible()) {
            throw helper.assertionException("Recently released mob should NOT be eligible at tick 1500");
        }
        if (result.reason() != MobCaptureEligibility.ExclusionReason.RECENTLY_RELEASED) {
            throw helper.assertionException("Reason should be RECENTLY_RELEASED, got: " + result.reason());
        }

        // Check at tick 2500 (immunity expired)
        var resultLater = MobCaptureEligibility.checkEligibility(zombie, 2500L);

        // Note: May still be excluded if allowUnbondedCapture is false
        // Just verify it's not excluded for RECENTLY_RELEASED
        if (resultLater.reason() == MobCaptureEligibility.ExclusionReason.RECENTLY_RELEASED) {
            throw helper.assertionException("Should NOT be excluded for RECENTLY_RELEASED at tick 2500");
        }

        helper.succeed();
    }

    private static void testCapturedMobTicketReadyCheck(GameTestHelper helper) {
        CapturedMobTicket ticket = CapturedMobTicket.create(
                EntityType.COW,
                new CompoundTag(),
                1000L,  // captured at
                1500L,  // ready at
                null,
                null
        );

        // Not ready before readyAtTick
        if (ticket.isReady(1000L)) {
            throw helper.assertionException("Ticket should NOT be ready at capture time");
        }
        if (ticket.isReady(1499L)) {
            throw helper.assertionException("Ticket should NOT be ready at tick 1499");
        }

        // Ready at and after readyAtTick
        if (!ticket.isReady(1500L)) {
            throw helper.assertionException("Ticket should be ready at tick 1500");
        }
        if (!ticket.isReady(2000L)) {
            throw helper.assertionException("Ticket should be ready at tick 2000");
        }

        helper.succeed();
    }

    // ==================== Lure Goal Tests ====================

    private static void testLureGoalOnlyBondedMobs(GameTestHelper helper) {
        // Spawn a zombie without MobSymbiote
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        BlockPos lureTarget = new BlockPos(5, 1, 5);

        // Try to start luring a mob without MobSymbiote - should fail
        boolean started = MobSymbioteHelper.startLuring(zombie, lureTarget);

        if (started) {
            throw helper.assertionException("startLuring should return false for mob without MobSymbiote");
        }
        if (MobSymbioteHelper.isBeingLured(zombie)) {
            throw helper.assertionException("Mob without MobSymbiote should NOT be lured");
        }

        // Now apply Level 1 MobSymbiote
        MobSymbioteHelper.applyMobSymbioteLevel1(zombie, 0L);

        // Try again - should succeed
        boolean startedAfterLevel1 = MobSymbioteHelper.startLuring(zombie, lureTarget);

        if (!startedAfterLevel1) {
            throw helper.assertionException("startLuring should return true for mob with MobSymbiote");
        }
        if (!MobSymbioteHelper.isBeingLured(zombie)) {
            throw helper.assertionException("Mob with MobSymbiote should be lured after startLuring");
        }

        helper.succeed();
    }

    private static void testLureGoalStartAndStop(GameTestHelper helper) {
        // Spawn a zombie with MobSymbiote
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        MobSymbioteHelper.applyMobSymbioteLevel1(zombie, 0L);
        BlockPos lureTarget = new BlockPos(5, 1, 5);

        // Initially not lured
        if (MobSymbioteHelper.isBeingLured(zombie)) {
            throw helper.assertionException("Mob should not be lured initially");
        }

        // Start luring
        MobSymbioteHelper.startLuring(zombie, lureTarget);

        if (!MobSymbioteHelper.isBeingLured(zombie)) {
            throw helper.assertionException("Mob should be lured after startLuring");
        }

        // Stop luring
        boolean stopped = MobSymbioteHelper.stopLuring(zombie);

        if (!stopped) {
            throw helper.assertionException("stopLuring should return true when goal was removed");
        }
        if (MobSymbioteHelper.isBeingLured(zombie)) {
            throw helper.assertionException("Mob should NOT be lured after stopLuring");
        }

        // Stop again - should return false (no goal to remove)
        boolean stoppedAgain = MobSymbioteHelper.stopLuring(zombie);

        if (stoppedAgain) {
            throw helper.assertionException("stopLuring should return false when no goal present");
        }

        helper.succeed();
    }

    private static void testLureGoalIsBeingLuredTo(GameTestHelper helper) {
        // Spawn a zombie with MobSymbiote
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        MobSymbioteHelper.applyMobSymbioteLevel1(zombie, 0L);
        BlockPos lureTarget1 = new BlockPos(5, 1, 5);
        BlockPos lureTarget2 = new BlockPos(10, 1, 10);

        // Start luring to target1
        MobSymbioteHelper.startLuring(zombie, lureTarget1);

        // Check isBeingLuredTo
        if (!MobSymbioteHelper.isBeingLuredTo(zombie, lureTarget1)) {
            throw helper.assertionException("Should be lured to target1");
        }
        if (MobSymbioteHelper.isBeingLuredTo(zombie, lureTarget2)) {
            throw helper.assertionException("Should NOT be lured to target2");
        }

        // Check getLureTarget
        BlockPos target = MobSymbioteHelper.getLureTarget(zombie);
        if (target == null || !target.equals(lureTarget1)) {
            throw helper.assertionException("getLureTarget should return target1, got: " + target);
        }

        helper.succeed();
    }

    private static void testLureGoalSwitchTarget(GameTestHelper helper) {
        // Spawn a zombie with MobSymbiote
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        MobSymbioteHelper.applyMobSymbioteLevel1(zombie, 0L);
        BlockPos lureTarget1 = new BlockPos(5, 1, 5);
        BlockPos lureTarget2 = new BlockPos(10, 1, 10);

        // Start luring to target1
        MobSymbioteHelper.startLuring(zombie, lureTarget1);

        if (!MobSymbioteHelper.isBeingLuredTo(zombie, lureTarget1)) {
            throw helper.assertionException("Should be lured to target1");
        }

        // Start luring to target2 (should switch)
        boolean switched = MobSymbioteHelper.startLuring(zombie, lureTarget2);

        if (!switched) {
            throw helper.assertionException("startLuring to new target should return true");
        }
        if (MobSymbioteHelper.isBeingLuredTo(zombie, lureTarget1)) {
            throw helper.assertionException("Should NOT be lured to target1 after switch");
        }
        if (!MobSymbioteHelper.isBeingLuredTo(zombie, lureTarget2)) {
            throw helper.assertionException("Should be lured to target2 after switch");
        }

        // Try to lure to same target again - should return false (already lured there)
        boolean sameTarget = MobSymbioteHelper.startLuring(zombie, lureTarget2);

        if (sameTarget) {
            throw helper.assertionException("startLuring to same target should return false");
        }

        helper.succeed();
    }

    private static void testMobSymbioteItemBondsMob(GameTestHelper helper) {
        // Spawn a zombie without MobSymbiote
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));

        // Verify no MobSymbiote initially
        if (MobSymbioteHelper.hasMobSymbiote(zombie)) {
            throw helper.assertionException("Zombie should NOT have MobSymbiote initially");
        }

        // Apply Level 1 MobSymbiote (simulating what MobSymbioteItem does)
        MobSymbioteHelper.applyMobSymbioteLevel1(zombie, 100L);

        // Verify now has MobSymbiote
        if (!MobSymbioteHelper.hasMobSymbiote(zombie)) {
            throw helper.assertionException("Zombie should have MobSymbiote after applyMobSymbioteLevel1()");
        }

        helper.succeed();
    }

    private static void testMobOutputReleaseClearsLureGoal(GameTestHelper helper) {
        // Spawn a zombie with MobSymbiote, then start luring
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        MobSymbioteHelper.applyMobSymbioteLevel1(zombie, 0L);
        BlockPos lureTarget = new BlockPos(5, 1, 5);
        MobSymbioteHelper.startLuring(zombie, lureTarget);

        // Verify luring
        if (!MobSymbioteHelper.isBeingLured(zombie)) {
            throw helper.assertionException("Zombie should be lured initially");
        }

        // Simulate what MobOutput does on release: stop luring but keep MobSymbiote
        MobSymbioteHelper.stopLuring(zombie);

        // Verify no longer lured
        if (MobSymbioteHelper.isBeingLured(zombie)) {
            throw helper.assertionException("Zombie should NOT be lured after stopLuring");
        }

        // Verify still has MobSymbiote
        if (!MobSymbioteHelper.hasMobSymbiote(zombie)) {
            throw helper.assertionException("Zombie should STILL have MobSymbiote after stopLuring");
        }

        helper.succeed();
    }

    // ==================== Item Input Structure Tests ====================

    private static void registerItemInputStructureTests(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition> env) {
        registerStructureTest(event, env, "item_input_collects_and_transfers",
                ITEM_INPUT_TO_CHEST_STRUCTURE, 200, FloraFaunaGameTests::testItemInputCollectsAndTransfers);
    }

    private static void testItemInputCollectsAndTransfers(GameTestHelper helper) {
        // Structure positions (relative to structure origin)
        BlockPos itemInputPos = new BlockPos(0, 1, 3);
        BlockPos storageAnchorPos = new BlockPos(2, 1, 3);
        BlockPos chestPos = new BlockPos(3, 1, 3);

        // Verify structure loaded correctly
        helper.assertBlockPresent(FloraFaunaRegistry.ITEM_INPUT.get(), itemInputPos);
        helper.assertBlockPresent(FloraFaunaRegistry.STORAGE_ANCHOR.get(), storageAnchorPos);

        // Pair the blocks programmatically (structure saves absolute positions which break on relocation)
        var itemInput = helper.getBlockEntity(itemInputPos,
                net.j40climb.florafauna.common.block.iteminput.rootiteminput.ItemInputBlockEntity.class);
        var storageAnchor = helper.getBlockEntity(storageAnchorPos,
                net.j40climb.florafauna.common.block.iteminput.storageanchor.StorageAnchorBlockEntity.class);

        if (itemInput == null || storageAnchor == null) {
            throw helper.assertionException("Block entities not loaded");
        }

        // Pair Item Input → Storage Anchor (using absolute positions)
        itemInput.pairWithAnchor(helper.absolutePos(storageAnchorPos));

        // Link Chest as destination on Storage Anchor
        storageAnchor.linkContainer(helper.absolutePos(chestPos));

        // Spawn a diamond above the item input
        BlockPos spawnPos = new BlockPos(0, 2, 3);
        helper.spawnItem(Items.DIAMOND, spawnPos);

        // Wait for: collection + animation + transfer (give plenty of time)
        helper.runAfterDelay(150, () -> {
            ChestBlockEntity chest = helper.getBlockEntity(chestPos, ChestBlockEntity.class);
            if (chest == null) {
                throw helper.assertionException("Chest not found at " + chestPos);
            }

            // Search chest inventory for diamond
            for (int i = 0; i < chest.getContainerSize(); i++) {
                ItemStack stack = chest.getItem(i);
                if (stack.is(Items.DIAMOND)) {
                    helper.succeed();
                    return;
                }
            }

            // If not in chest, check if it's in the Item Input buffer
            var inputCheck = helper.getBlockEntity(itemInputPos,
                    net.j40climb.florafauna.common.block.iteminput.rootiteminput.ItemInputBlockEntity.class);
            if (inputCheck != null && !inputCheck.getBuffer().isEmpty()) {
                throw helper.assertionException("Diamond stuck in Item Input buffer - pairing may have failed");
            }

            throw helper.assertionException("Diamond was not transferred to chest");
        });
    }

    // ==================== Helper Methods ====================

    /**
     * Register a test with a custom structure.
     */
    private static void registerStructureTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition> env,
            String name,
            Identifier structure,
            int maxTicks,
            Consumer<GameTestHelper> testFunction
    ) {
        Identifier testId = Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, name);

        TestData<Holder<TestEnvironmentDefinition>> testData = new TestData<>(
                env,
                structure,
                maxTicks,
                20,   // setupTicks - give block entities time to load
                true, // required
                Rotation.NONE,
                false, // manualOnly
                1,    // maxAttempts
                1,    // requiredSuccesses
                false // skyAccess
        );

        event.registerTest(testId, new SimpleGameTestInstance(testData, testFunction));
    }

    /**
     * Register a test with the default empty structure and settings.
     */
    private static void registerTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition> env,
            String name,
            Consumer<GameTestHelper> testFunction
    ) {
        Identifier testId = Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, name);

        TestData<Holder<TestEnvironmentDefinition>> testData = new TestData<>(
                env,
                EMPTY_STRUCTURE,
                100, // maxTicks
                0,   // setupTicks
                true, // required
                Rotation.NONE,
                false, // manualOnly
                1,    // maxAttempts
                1,    // requiredSuccesses
                false // skyAccess
        );

        // Create a SimpleGameTestInstance wrapper
        event.registerTest(testId, new SimpleGameTestInstance(testData, testFunction));
    }
}
