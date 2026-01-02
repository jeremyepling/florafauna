package net.j40climb.florafauna.common.block.wood;

import net.j40climb.florafauna.FloraFauna;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles axe stripping behavior for mod wood blocks.
 * Converts logs to stripped logs, and wood to stripped wood when right-clicked with an axe.
 */
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public class WoodStripping {

    // Uses lazy initialization to avoid accessing blocks before they're registered
    private static Map<Block, Block> strippables = null;

    /**
     * Gets the strippables map, initializing it on first access.
     * Lazy initialization is necessary because static initializers run during
     * class loading, which happens before blocks are registered with the game.
     */
    private static Map<Block, Block> getStrippables() {
        if (strippables == null) {
            strippables = new HashMap<>();
            for (WoodType woodType : WoodType.values()) {
                WoodBlockSet wood = woodType.getBlockSet();
                strippables.put(wood.log().get(), wood.strippedLog().get());
                strippables.put(wood.wood().get(), wood.strippedWood().get());
            }
        }
        return strippables;
    }

    @SubscribeEvent
    public static void onBlockToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (event.getItemAbility() == ItemAbilities.AXE_STRIP) {
            Block block = event.getState().getBlock();
            Block stripped = getStrippables().get(block);
            if (stripped != null) {
                event.setFinalState(stripped.defaultBlockState());
            }
        }
    }
}
