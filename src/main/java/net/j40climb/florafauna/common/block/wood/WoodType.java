package net.j40climb.florafauna.common.block.wood;

/**
 * Enum of all wood types in the mod. Adding a new entry here automatically registers
 * all wood blocks (log, stripped_log, wood, stripped_wood, planks) for that wood type.
 *
 * To add a new wood type, just add an enum entry:
 *   MAPLE("maple"),
 *   WILLOW("willow")
 *
 * Access blocks via: ModWoodType.DRIFTWOOD.getBlockSet().log()
 * Iterate via: ModWoodType.values()
 */
public enum WoodType {
    DRIFTWOOD("driftwood");
    // Add more wood types here - that's all you need to do!
    // MAPLE("maple"),
    // WILLOW("willow")

    private final String name;
    private final WoodBlockSet blockSet;

    WoodType(String name) {
        this.name = name;
        this.blockSet = WoodBlockRegistration.register(name);
    }

    public String getName() {
        return name;
    }

    public WoodBlockSet getBlockSet() {
        return blockSet;
    }
}
