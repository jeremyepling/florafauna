package net.j40climb.florafauna.client.events;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.client.BlockBreakUtils;
import net.j40climb.florafauna.component.ModDataComponentTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/*
This code came from https://github.com/Direwolf20-MC/JustDireThings/blob/32225177c42a25f32e69d46e342ea81dfed91a7f/src/main/java/com/direwolf20/justdirethings/client/events/PlayerEvents.java
and has been modified for my needs.
 */

@EventBusSubscriber(modid = FloraFauna.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class PlayerEvents {

    @SubscribeEvent
    public static void LeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        ItemStack itemStack = event.getItemStack();

        Player player = event.getEntity();
        if (player.getMainHandItem().get(ModDataComponentTypes.MINING_MODE_DATA) != null) {
                BlockBreakUtils.doExtraCrumblings(event);
        }
    }
}
