package net.j40climb.florafauna.common.block.wood;

//? if <26.3 {
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
//?}

/** Registers vanilla axe stripping without changing the wood's axis. */
public final class WoodStripping {
    private WoodStripping() {}

    public static void init() {
        //? if <26.3 {
        for (WoodType type : WoodType.values()) {
            WoodBlockSet wood = type.getBlockSet();
            StrippableBlockRegistry.register(wood.log().get(), wood.strippedLog().get());
            StrippableBlockRegistry.register(wood.wood().get(), wood.strippedWood().get());
        }
        //?}
        // 26.3 uses the vanilla axe transformer; its mixin adds our stripping rules.
    }
}
