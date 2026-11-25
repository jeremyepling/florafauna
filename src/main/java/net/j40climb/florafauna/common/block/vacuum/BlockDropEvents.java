package net.j40climb.florafauna.common.block.vacuum;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
//? if neoforge {
import net.neoforged.bus.api.SubscribeEvent;
//?}
//? if neoforge {
import net.neoforged.fml.common.EventBusSubscriber;
//?}
//? if neoforge {
import net.neoforged.neoforge.event.level.BlockDropsEvent;
//?}

/**
 * Event handler that tags item drops from block breaking with BlockDropData.
 * This allows vacuum blocks (like Mining Anchor) to filter and only collect
 * items that originated from breaking blocks.
 */
//? if neoforge {
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
//?}
public class BlockDropEvents {

    /**
     * Tags all items dropped from block breaking with BlockDropData.
     * Called when a block is broken and drops items.
     */
    //? if neoforge {
    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        onBlockDrops(event.getLevel(), event.getDrops());
    }
    //?}

    public static void onBlockDrops(net.minecraft.server.level.ServerLevel world, java.util.List<net.minecraft.world.entity.item.ItemEntity> drops) {
        long currentTick = world.getGameTime();
        BlockDropData blockDropData = BlockDropData.create(currentTick);

        // Tag all dropped item entities
        for (ItemEntity itemEntity : drops) {
            net.j40climb.florafauna.platform.AttachmentAccess.set(itemEntity, FloraFaunaRegistry.BLOCK_DROP_DATA, blockDropData);
        }
    }
}
