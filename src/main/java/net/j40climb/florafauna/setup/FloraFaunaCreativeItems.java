package net.j40climb.florafauna.setup;

import net.j40climb.florafauna.common.block.wood.WoodBlockSet;
import net.j40climb.florafauna.common.block.wood.WoodType;
import net.minecraft.world.item.CreativeModeTab;

/** The same discoverable content and order on every loader. */
public final class FloraFaunaCreativeItems {
    private FloraFaunaCreativeItems() {}
    public static void accept(CreativeModeTab.Output output) {

        // Items
        output.accept(FloraFaunaRegistry.DORMANT_SYMBIOTE.get());
        output.accept(FloraFaunaRegistry.SYMBIOTIC_POLLEN.get());

        // Spawn eggs
        output.accept(FloraFaunaRegistry.GECKO_SPAWN_EGG.get());
        output.accept(FloraFaunaRegistry.LIZARD_SPAWN_EGG.get());

        // Blocks
        output.accept(FloraFaunaRegistry.TEAL_MOSS_BLOCK.get());
        output.accept(FloraFaunaRegistry.SYMBIOTE_CONTAINMENT_CHAMBER.get());
        output.accept(FloraFaunaRegistry.COCOON_CHAMBER.get());
        output.accept(FloraFaunaRegistry.RESONANT_BLOOM.get());
        output.accept(FloraFaunaRegistry.MOB_BARRIER.get());
        output.accept(FloraFaunaRegistry.SLIME_GRATE.get());
        output.accept(FloraFaunaRegistry.HUSK.get());

        // Item Input System blocks
        output.accept(FloraFaunaRegistry.STORAGE_ANCHOR.get());
        output.accept(FloraFaunaRegistry.ITEM_INPUT.get());
        output.accept(FloraFaunaRegistry.FIELD_RELAY.get());

        // Mining Anchor System blocks
        output.accept(FloraFaunaRegistry.TIER1_MINING_ANCHOR.get());
        output.accept(FloraFaunaRegistry.TIER2_MINING_ANCHOR.get());
        output.accept(FloraFaunaRegistry.TIER2_POD.get());

        // Mob Transport System blocks
        output.accept(FloraFaunaRegistry.MOB_INPUT.get());
        output.accept(FloraFaunaRegistry.MOB_OUTPUT.get());
        output.accept(FloraFaunaRegistry.MOB_SYMBIOTE.get());

        // Iron Garden System blocks
        output.accept(FloraFaunaRegistry.FERRIC_POPPY.get());
        output.accept(FloraFaunaRegistry.FERRIC_POPPY_ITEM.get());

        // Wood blocks - iterates through all wood types
        for (WoodType woodType : WoodType.values()) {
            WoodBlockSet wood = woodType.getBlockSet();
            output.accept(wood.log().get());
            output.accept(wood.strippedLog().get());
            output.accept(wood.wood().get());
            output.accept(wood.strippedWood().get());
            output.accept(wood.planks().get());
            output.accept(wood.slab().get());
            output.accept(wood.fence().get());
            output.accept(wood.fenceGate().get());
        }

    }
}
