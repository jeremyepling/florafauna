package net.j40climb.florafauna.common.event;

import net.j40climb.florafauna.FloraFauna;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Registers capabilities for block entities to allow interaction with
 * hoppers, pipes, and other item transport systems.
 */
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public class ModCapabilities {

//    @SubscribeEvent
//    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
//        // Register item handler capability for Symbiote Containment Chamber
//        event.registerBlockEntity(
//                Capabilities.Item.BLOCK,
//                ModBlockEntities.SYMBIOTE_CONTAINMENT_CHAMBER,
//                (blockEntity, direction) -> blockEntity.getItemHandler(direction)
//        );
//    }
}