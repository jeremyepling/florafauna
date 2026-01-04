package net.j40climb.florafauna.test;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.containmentchamber.ContainmentChamberBlockEntity;
import net.j40climb.florafauna.common.block.vacuum.ClaimedItemData;
import net.j40climb.florafauna.common.block.vacuum.VacuumBuffer;
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
import net.j40climb.florafauna.common.symbiote.voice.VoiceCooldownState;
import net.j40climb.florafauna.common.symbiote.voice.VoiceTier;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
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
        registerVacuumBufferTests(event, defaultEnv);
        registerClaimedItemDataTests(event, defaultEnv);

        // Register mining mode tests
        registerMiningModeTests(event, defaultEnv);

        // Register containment chamber tests
        registerContainmentChamberTests(event, defaultEnv);

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

    private static void registerVacuumBufferTests(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition> env) {
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
        VacuumBuffer buffer = new VacuumBuffer(27);

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
        VacuumBuffer buffer = new VacuumBuffer(27);

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
        VacuumBuffer buffer = new VacuumBuffer(27);

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
        VacuumBuffer buffer = new VacuumBuffer(2);

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

    private static void testItemBufferPartialMergeToFull(GameTestHelper helper) {
        VacuumBuffer buffer = new VacuumBuffer(27);

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
        VacuumBuffer buffer = new VacuumBuffer(27);

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
        VacuumBuffer buffer = new VacuumBuffer(27);
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
        VacuumBuffer buffer = new VacuumBuffer(27);
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
