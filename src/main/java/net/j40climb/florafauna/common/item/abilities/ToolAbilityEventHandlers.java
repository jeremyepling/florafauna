package net.j40climb.florafauna.common.item.abilities;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.item.abilities.data.MiningSpeed;
import net.j40climb.florafauna.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Event handlers for tool abilities.
 * These work with any item that has the appropriate DataComponents.
 */
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public class ToolAbilityEventHandlers {
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack itemStack = event.getEntity().getMainHandItem();

        // Component-based check - works with any item that has TOOL_CONFIG
        if (itemStack.has(FloraFaunaRegistry.TOOL_CONFIG)) {
            ToolConfig config = itemStack.get(FloraFaunaRegistry.TOOL_CONFIG);
            float newSpeed = switch (config.miningSpeed()) {
                case MiningSpeed.STANDARD -> event.getOriginalSpeed();
                case MiningSpeed.EFFICIENCY -> 35.0F;
                case MiningSpeed.INSTABREAK -> 100.0F;
            };
            event.setNewSpeed(newSpeed);
        }
    }
}
