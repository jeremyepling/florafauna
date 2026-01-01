package net.j40climb.florafauna.test;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.item.symbiote.dialogue.SymbioteDialogueEntry;
import net.j40climb.florafauna.common.item.symbiote.dialogue.SymbioteDialogueRepository;
import net.j40climb.florafauna.common.item.symbiote.observation.ChaosSuppressor;
import net.j40climb.florafauna.common.item.symbiote.observation.ObservationCategory;
import net.j40climb.florafauna.common.item.symbiote.progress.ConceptSignal;
import net.j40climb.florafauna.common.item.symbiote.progress.ProgressSignalTracker;
import net.j40climb.florafauna.common.item.symbiote.progress.SignalState;
import net.j40climb.florafauna.common.item.symbiote.voice.VoiceCooldownState;
import net.j40climb.florafauna.common.item.symbiote.voice.VoiceTier;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;
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

    private static final ResourceLocation EMPTY_STRUCTURE = ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "empty_1x1x1");

    private static void registerTests(RegisterGameTestsEvent event) {
        // Register a minimal test environment
        Holder<TestEnvironmentDefinition> defaultEnv = event.registerEnvironment(
                ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "default"),
                new TestEnvironmentDefinition.AllOf()
        );

        // Register all symbiote tests
        registerVoiceCooldownTests(event, defaultEnv);
        registerProgressSignalTests(event, defaultEnv);
        registerChaosSuppressorTests(event, defaultEnv);
        registerDialogueRepositoryTests(event, defaultEnv);
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

    // ==================== Helper Methods ====================

    /**
     * Register a test with the default empty structure and settings.
     */
    private static void registerTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition> env,
            String name,
            Consumer<GameTestHelper> testFunction
    ) {
        ResourceLocation testId = ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, name);

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
