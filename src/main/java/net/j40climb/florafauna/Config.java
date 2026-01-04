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

    // Capacity settings
    private static final ModConfigSpec.IntValue MINING_ANCHOR_BASE_CAPACITY = BUILDER
            .comment("Base capacity of the anchor buffer in items (before pods)")
            .defineInRange("baseCapacity", 256, 64, 1024);

    private static final ModConfigSpec.IntValue MINING_ANCHOR_POD_CAPACITY = BUILDER
            .comment("Capacity per storage pod in stacks (multiplied by 64 for item count)")
            .defineInRange("podCapacityStacks", 128, 16, 512);

    private static final ModConfigSpec.IntValue MINING_ANCHOR_MAX_PODS = BUILDER
            .comment("Maximum number of pods that can grow from an anchor")
            .defineInRange("maxPods", 4, 1, 8);

    // Growth settings
    private static final ModConfigSpec.DoubleValue MINING_ANCHOR_POD_GROWTH_THRESHOLD = BUILDER
            .comment("Fill percentage at which a new pod spawns (0.0 to 1.0)")
            .defineInRange("podGrowthThreshold", 0.8, 0.5, 1.0);

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
    public static int miningAnchorBaseCapacity;
    public static int miningAnchorPodCapacity;
    public static int miningAnchorMaxPods;
    public static double miningAnchorPodGrowthThreshold;
    public static int miningAnchorCollectRadius;
    public static int miningAnchorCollectInterval;
    public static boolean miningAnchorBlockDropsOnly;

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
        miningAnchorBaseCapacity = MINING_ANCHOR_BASE_CAPACITY.get();
        miningAnchorPodCapacity = MINING_ANCHOR_POD_CAPACITY.get();
        miningAnchorMaxPods = MINING_ANCHOR_MAX_PODS.get();
        miningAnchorPodGrowthThreshold = MINING_ANCHOR_POD_GROWTH_THRESHOLD.get();
        miningAnchorCollectRadius = MINING_ANCHOR_COLLECT_RADIUS.get();
        miningAnchorCollectInterval = MINING_ANCHOR_COLLECT_INTERVAL.get();
        miningAnchorBlockDropsOnly = MINING_ANCHOR_BLOCK_DROPS_ONLY.get();
    }
}
