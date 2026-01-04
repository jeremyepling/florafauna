package net.j40climb.florafauna.common.block.vacuum;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * Event handler that tags item drops from block breaking with BlockDropData.
 * This allows vacuum blocks (like Mining Anchor) to filter and only collect
 * items that originated from breaking blocks.
 */
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public class BlockDropEvents {

    /**
     * Tags all items dropped from block breaking with BlockDropData.
     * Called when a block is broken and drops items.
     */
    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        long currentTick = event.getLevel().getGameTime();
        BlockDropData blockDropData = BlockDropData.create(currentTick);

        // Tag all dropped item entities
        for (ItemEntity itemEntity : event.getDrops()) {
            itemEntity.setData(FloraFaunaRegistry.BLOCK_DROP_DATA, blockDropData);
        }
    }
}
