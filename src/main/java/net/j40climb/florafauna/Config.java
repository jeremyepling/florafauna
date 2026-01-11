package net.j40climb.florafauna;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Central configuration for the FloraFauna mod.
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ==================== ITEM INPUT SYSTEM ====================

    static {
        BUILDER.comment("Item Input System Configuration").push("iteminput");
    }

    // Collection settings
    private static final ModConfigSpec.IntValue COLLECT_RADIUS = BUILDER
            .comment("Radius in blocks to scan for dropped items")
            .defineInRange("collectRadius", 8, 1, 32);

    private static final ModConfigSpec.IntValue COLLECT_INTERVAL_TICKS = BUILDER
            .comment("Ticks between collection attempts")
            .defineInRange("collectIntervalTicks", 10, 1, 100);

    private static final ModConfigSpec.IntValue MAX_ITEM_ENTITIES_PER_COLLECT = BUILDER
            .comment("Maximum number of item entities to claim per collection cycle")
            .defineInRange("maxItemEntitiesPerCollect", 8, 1, 64);

    private static final ModConfigSpec.IntValue MAX_ITEMS_PER_COLLECT = BUILDER
            .comment("Maximum total item count to claim per collection cycle")
            .defineInRange("maxItemsPerCollect", 64, 1, 576);

    // Buffer settings
    private static final ModConfigSpec.IntValue MAX_BUFFERED_STACKS = BUILDER
            .comment("Maximum number of item stacks in the buffer (like a chest inventory)")
            .defineInRange("maxBufferedStacks", 27, 1, 54);

    // Transfer settings
    private static final ModConfigSpec.IntValue TRANSFER_INTERVAL_TICKS = BUILDER
            .comment("Ticks between transfer attempts to storage")
            .defineInRange("transferIntervalTicks", 20, 1, 200);

    private static final ModConfigSpec.IntValue MAX_STACKS_PER_TRANSFER_TICK = BUILDER
            .comment("Maximum number of unique stacks to transfer per tick")
            .defineInRange("maxStacksPerTransferTick", 4, 1, 27);

    private static final ModConfigSpec.IntValue MAX_ITEMS_PER_TRANSFER_TICK = BUILDER
            .comment("Maximum total item count to transfer per tick")
            .defineInRange("maxItemsPerTransferTick", 256, 1, 1728);

    // Overflow settings
    private static final ModConfigSpec.IntValue BLOCKED_RETRY_BASE_TICKS = BUILDER
            .comment("Base delay in ticks before retrying when storage is full")
            .defineInRange("blockedRetryBaseTicks", 40, 1, 200);

    private static final ModConfigSpec.IntValue BLOCKED_RETRY_MAX_TICKS = BUILDER
            .comment("Maximum delay in ticks for exponential backoff")
            .defineInRange("blockedRetryMaxTicks", 600, 100, 12000);

    // Animation settings
    private static final ModConfigSpec.IntValue ANIMATION_DURATION_TICKS = BUILDER
            .comment("Duration in ticks for the item absorption animation")
            .defineInRange("animationDurationTicks", 20, 5, 100);

    static {
        BUILDER.pop();
    }

    // ==================== MINING ANCHOR SYSTEM ====================

    static {
        BUILDER.comment("Mining Anchor System Configuration").push("mininganchor");
    }

    // Max pods per tier (pod capacity is defined by pod tier, not config)
    private static final ModConfigSpec.IntValue MINING_ANCHOR_TIER1_MAX_PODS = BUILDER
            .comment("Maximum number of pods for Tier 1 (Feral) anchors")
            .defineInRange("tier1MaxPods", 4, 1, 16);

    private static final ModConfigSpec.IntValue MINING_ANCHOR_TIER2_MAX_PODS = BUILDER
            .comment("Maximum number of pods for Tier 2 (Hardened) anchors")
            .defineInRange("tier2MaxPods", 8, 1, 16);

    // Spawn settings
    private static final ModConfigSpec.IntValue MINING_ANCHOR_POD_SPAWN_RADIUS = BUILDER
            .comment("Maximum distance from anchor where pods can spawn")
            .defineInRange("podSpawnRadius", 5, 1, 10);

    // Collection settings (overrides for block drops only mode)
    private static final ModConfigSpec.IntValue MINING_ANCHOR_COLLECT_RADIUS = BUILDER
            .comment("Radius in blocks to scan for block drops")
            .defineInRange("collectRadius", 8, 1, 32);

    private static final ModConfigSpec.IntValue MINING_ANCHOR_COLLECT_INTERVAL = BUILDER
            .comment("Ticks between collection attempts")
            .defineInRange("collectIntervalTicks", 10, 1, 100);

    private static final ModConfigSpec.BooleanValue MINING_ANCHOR_BLOCK_DROPS_ONLY = BUILDER
            .comment("If true, only collect items from block drops (not player drops or other sources)")
            .define("blockDropsOnly", true);

    static {
        BUILDER.pop();
    }

    // ==================== MOB TRANSPORT SYSTEM ====================

    static {
        BUILDER.comment("Mob Transport System Configuration (MobInput/MobOutput)").push("mobtransport");
    }

    // Lure settings
    private static final ModConfigSpec.IntValue MOB_LURE_RADIUS = BUILDER
            .comment("Radius in blocks for luring eligible mobs")
            .defineInRange("lureRadius", 12, 1, 32);

    private static final ModConfigSpec.IntValue MOB_LURE_INTERVAL_TICKS = BUILDER
            .comment("Ticks between lure attempts")
            .defineInRange("lureIntervalTicks", 20, 1, 100);

    // Capture settings
    private static final ModConfigSpec.DoubleValue MOB_CAPTURE_RADIUS = BUILDER
            .comment("Radius in blocks for capturing mobs (should be smaller than lure)")
            .defineInRange("captureRadius", 1.5, 0.5, 4.0);

    private static final ModConfigSpec.IntValue MOB_CAPTURE_ANIM_TICKS = BUILDER
            .comment("Duration of capture animation in ticks")
            .defineInRange("captureAnimTicks", 15, 5, 60);

    // Travel settings
    private static final ModConfigSpec.IntValue MOB_MIN_TRAVEL_DELAY_TICKS = BUILDER
            .comment("Minimum travel delay in ticks before mob is ready at output")
            .defineInRange("minTravelDelayTicks", 60, 20, 600);

    private static final ModConfigSpec.IntValue MOB_MAX_TRAVEL_DELAY_TICKS = BUILDER
            .comment("Maximum travel delay in ticks before mob is ready at output")
            .defineInRange("maxTravelDelayTicks", 200, 60, 1200);

    // Queue settings
    private static final ModConfigSpec.IntValue MOB_MAX_QUEUE_SIZE = BUILDER
            .comment("Maximum number of captured mobs queued per MobInput")
            .defineInRange("maxQueueSizePerInput", 5, 1, 20);

    private static final ModConfigSpec.IntValue MOB_RELEASE_CHECK_INTERVAL_TICKS = BUILDER
            .comment("Ticks between checking for mobs ready to release")
            .defineInRange("releaseCheckIntervalTicks", 20, 1, 100);

    // Eligibility settings
    private static final ModConfigSpec.BooleanValue MOB_ALLOW_UNBONDED_CAPTURE = BUILDER
            .comment("If true, unbonded mobs can be captured (bonded still prioritized)")
            .define("allowUnbondedCapture", false);

    private static final ModConfigSpec.IntValue MOB_RECENTLY_RELEASED_IMMUNITY_TICKS = BUILDER
            .comment("Ticks of capture immunity after a mob is released")
            .defineInRange("recentlyReleasedImmunityTicks", 100, 20, 600);

    static {
        BUILDER.pop();
    }

    // ==================== FEAR SYSTEM ====================

    static {
        BUILDER.comment("Fear System Configuration (Creeper Fear Ecosystem)").push("fear");
    }

    // Timing thresholds
    private static final ModConfigSpec.IntValue FEAR_CHECK_INTERVAL_TICKS = BUILDER
            .comment("Ticks between fear state updates")
            .defineInRange("checkIntervalTicks", 10, 1, 40);

    private static final ModConfigSpec.IntValue PANIC_DURATION_FOR_LEAK = BUILDER
            .comment("Ticks in PANICKED before triggering LEAK event")
            .defineInRange("panicDurationForLeak", 200, 40, 600);

    private static final ModConfigSpec.IntValue EXHAUSTED_COOLDOWN_TICKS = BUILDER
            .comment("Ticks in EXHAUSTED (cooldown) before returning to CALM")
            .defineInRange("exhaustedCooldownTicks", 4500, 200, 12000);

    private static final ModConfigSpec.IntValue MAX_LEAKS_BEFORE_OVERSTRESS = BUILDER
            .comment("Consecutive leaks without CALM reset before OVERSTRESS (explosion)")
            .defineInRange("maxLeaksBeforeOverstress", 3, 1, 10);

    // Detection range
    private static final ModConfigSpec.DoubleValue FEAR_SOURCE_DETECTION_RANGE = BUILDER
            .comment("Range in blocks to detect fear sources")
            .defineInRange("detectionRange", 16.0, 4.0, 32.0);

    // Creeper gunpowder drops
    private static final ModConfigSpec.IntValue GUNPOWDER_DROP_MIN = BUILDER
            .comment("Minimum gunpowder dropped per LEAK event")
            .defineInRange("gunpowderDropMin", 10, 1, 32);

    private static final ModConfigSpec.IntValue GUNPOWDER_DROP_MAX = BUILDER
            .comment("Maximum gunpowder dropped per LEAK event")
            .defineInRange("gunpowderDropMax", 14, 1, 64);

    // Enderman-specific settings
    private static final ModConfigSpec.DoubleValue ENDERMAN_STARE_DISTANCE = BUILDER
            .comment("Maximum distance in blocks for enderman to 'lock in' a stare at reflective blocks or armor stands")
            .defineInRange("endermanStareDistance", 4.0, 1.0, 16.0);

    private static final ModConfigSpec.IntValue ENDER_PEARL_DROP_MIN = BUILDER
            .comment("Minimum ender pearls dropped per LEAK event")
            .defineInRange("enderPearlDropMin", 2, 1, 16);

    private static final ModConfigSpec.IntValue ENDER_PEARL_DROP_MAX = BUILDER
            .comment("Maximum ender pearls dropped per LEAK event")
            .defineInRange("enderPearlDropMax", 4, 1, 32);

    // Blaze-specific settings
    private static final ModConfigSpec.IntValue BLAZE_MIN_SNOW_GOLEMS = BUILDER
            .comment("Minimum number of snow golems required to trigger blaze fear")
            .defineInRange("blazeMinSnowGolems", 1, 1, 10);

    private static final ModConfigSpec.IntValue BLAZE_COLD_SCAN_RADIUS = BUILDER
            .comment("Radius for scanning cold blocks (creates a cube of 2*radius+1)")
            .defineInRange("blazeColdScanRadius", 8, 4, 16);

    private static final ModConfigSpec.IntValue BLAZE_MIN_COLD_BLOCKS = BUILDER
            .comment("Minimum cold blocks (snow/ice) required in scan area to enable fear (0 = disabled)")
            .defineInRange("blazeMinColdBlocks", 2, 0, 500);

    private static final ModConfigSpec.BooleanValue BLAZE_REQUIRE_BOTH_CONDITIONS = BUILDER
            .comment("If true, blazes need BOTH snow golems AND cold blocks to leak. If false, either triggers fear.")
            .define("blazeRequireBothConditions", false);

    private static final ModConfigSpec.IntValue BLAZE_ROD_DROP_MIN = BUILDER
            .comment("Minimum blaze rods dropped per LEAK event")
            .defineInRange("blazeRodDropMin", 2, 1, 16);

    private static final ModConfigSpec.IntValue BLAZE_ROD_DROP_MAX = BUILDER
            .comment("Maximum blaze rods dropped per LEAK event")
            .defineInRange("blazeRodDropMax", 4, 1, 32);

    private static final ModConfigSpec.BooleanValue BLAZE_SUPPRESS_ATTACKS = BUILDER
            .comment("If true, blazes stop attacking while in PANICKED state")
            .define("blazeSuppressAttacks", true);

    static {
        BUILDER.pop();
    }

    // ==================== BUILD SPEC ====================

    public static final ModConfigSpec SPEC = BUILDER.build();

    // ==================== RUNTIME VALUES ====================

    // Item Input System
    public static int collectRadius;
    public static int collectIntervalTicks;
    public static int maxItemEntitiesPerCollect;
    public static int maxItemsPerCollect;
    public static int maxBufferedStacks;
    public static int transferIntervalTicks;
    public static int maxStacksPerTransferTick;
    public static int maxItemsPerTransferTick;
    public static int blockedRetryBaseTicks;
    public static int blockedRetryMaxTicks;
    public static int animationDurationTicks;

    // Mining Anchor System
    public static int miningAnchorTier1MaxPods;
    public static int miningAnchorTier2MaxPods;
    public static int miningAnchorPodSpawnRadius;
    public static int miningAnchorCollectRadius;
    public static int miningAnchorCollectInterval;
    public static boolean miningAnchorBlockDropsOnly;

    // Mob Transport System
    public static int lureRadius;
    public static int lureIntervalTicks;
    public static double captureRadius;
    public static int captureAnimTicks;
    public static int minTravelDelayTicks;
    public static int maxTravelDelayTicks;
    public static int maxQueueSizePerInput;
    public static int releaseCheckIntervalTicks;
    public static boolean allowUnbondedCapture;
    public static int recentlyReleasedImmunityTicks;

    // Fear System
    public static int fearCheckIntervalTicks;
    public static int panicDurationForLeak;
    public static int exhaustedCooldownTicks;
    public static int maxLeaksBeforeOverstress;
    public static double fearSourceDetectionRange;
    public static int gunpowderDropMin;
    public static int gunpowderDropMax;
    public static double endermanStareDistance;
    public static int enderPearlDropMin;
    public static int enderPearlDropMax;
    public static int blazeMinSnowGolems;
    public static int blazeColdScanRadius;
    public static int blazeMinColdBlocks;
    public static boolean blazeRequireBothConditions;
    public static int blazeRodDropMin;
    public static int blazeRodDropMax;
    public static boolean blazeSuppressAttacks;

    /**
     * Loads config values from the spec into static fields.
     * Called when config is loaded/reloaded.
     */
    public static void loadConfig() {
        // Item Input System
        collectRadius = COLLECT_RADIUS.get();
        collectIntervalTicks = COLLECT_INTERVAL_TICKS.get();
        maxItemEntitiesPerCollect = MAX_ITEM_ENTITIES_PER_COLLECT.get();
        maxItemsPerCollect = MAX_ITEMS_PER_COLLECT.get();
        maxBufferedStacks = MAX_BUFFERED_STACKS.get();
        transferIntervalTicks = TRANSFER_INTERVAL_TICKS.get();
        maxStacksPerTransferTick = MAX_STACKS_PER_TRANSFER_TICK.get();
        maxItemsPerTransferTick = MAX_ITEMS_PER_TRANSFER_TICK.get();
        blockedRetryBaseTicks = BLOCKED_RETRY_BASE_TICKS.get();
        blockedRetryMaxTicks = BLOCKED_RETRY_MAX_TICKS.get();
        animationDurationTicks = ANIMATION_DURATION_TICKS.get();

        // Mining Anchor System
        miningAnchorTier1MaxPods = MINING_ANCHOR_TIER1_MAX_PODS.get();
        miningAnchorTier2MaxPods = MINING_ANCHOR_TIER2_MAX_PODS.get();
        miningAnchorPodSpawnRadius = MINING_ANCHOR_POD_SPAWN_RADIUS.get();
        miningAnchorCollectRadius = MINING_ANCHOR_COLLECT_RADIUS.get();
        miningAnchorCollectInterval = MINING_ANCHOR_COLLECT_INTERVAL.get();
        miningAnchorBlockDropsOnly = MINING_ANCHOR_BLOCK_DROPS_ONLY.get();

        // Mob Transport System
        lureRadius = MOB_LURE_RADIUS.get();
        lureIntervalTicks = MOB_LURE_INTERVAL_TICKS.get();
        captureRadius = MOB_CAPTURE_RADIUS.get();
        captureAnimTicks = MOB_CAPTURE_ANIM_TICKS.get();
        minTravelDelayTicks = MOB_MIN_TRAVEL_DELAY_TICKS.get();
        maxTravelDelayTicks = MOB_MAX_TRAVEL_DELAY_TICKS.get();
        maxQueueSizePerInput = MOB_MAX_QUEUE_SIZE.get();
        releaseCheckIntervalTicks = MOB_RELEASE_CHECK_INTERVAL_TICKS.get();
        allowUnbondedCapture = MOB_ALLOW_UNBONDED_CAPTURE.get();
        recentlyReleasedImmunityTicks = MOB_RECENTLY_RELEASED_IMMUNITY_TICKS.get();

        // Fear System
        fearCheckIntervalTicks = FEAR_CHECK_INTERVAL_TICKS.get();
        panicDurationForLeak = PANIC_DURATION_FOR_LEAK.get();
        exhaustedCooldownTicks = EXHAUSTED_COOLDOWN_TICKS.get();
        maxLeaksBeforeOverstress = MAX_LEAKS_BEFORE_OVERSTRESS.get();
        fearSourceDetectionRange = FEAR_SOURCE_DETECTION_RANGE.get();
        gunpowderDropMin = GUNPOWDER_DROP_MIN.get();
        gunpowderDropMax = GUNPOWDER_DROP_MAX.get();
        endermanStareDistance = ENDERMAN_STARE_DISTANCE.get();
        enderPearlDropMin = ENDER_PEARL_DROP_MIN.get();
        enderPearlDropMax = ENDER_PEARL_DROP_MAX.get();
        blazeMinSnowGolems = BLAZE_MIN_SNOW_GOLEMS.get();
        blazeColdScanRadius = BLAZE_COLD_SCAN_RADIUS.get();
        blazeMinColdBlocks = BLAZE_MIN_COLD_BLOCKS.get();
        blazeRequireBothConditions = BLAZE_REQUIRE_BOTH_CONDITIONS.get();
        blazeRodDropMin = BLAZE_ROD_DROP_MIN.get();
        blazeRodDropMax = BLAZE_ROD_DROP_MAX.get();
        blazeSuppressAttacks = BLAZE_SUPPRESS_ATTACKS.get();
    }
}
