package net.j40climb.florafauna.client.events;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.client.CustomBlockOutlineRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
public class RenderEvents {

    @SubscribeEvent
    public static void extractBlockOutlineRenderStateEvent(ExtractBlockOutlineRenderStateEvent event) {
        event.addCustomRenderer(new CustomBlockOutlineRenderer());
    }
}
