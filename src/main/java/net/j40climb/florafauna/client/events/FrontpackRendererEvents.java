package net.j40climb.florafauna.client.events;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.client.model.FrenchFrontpackModel;
import net.j40climb.florafauna.client.renderer.layer.FrenchFrontpackLayer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Handles registration of the Frenchie frontpack model layer and adding it to player renderers.
 * Must be on the MOD event bus for registration events.
 */
@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
public class FrontpackRendererEvents {

    /**
     * Registers the frontpack model layer definition.
     * This bakes the 3D geometry from FrenchFrontpackModel.createBodyLayer().
     */
    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(
                FrenchFrontpackModel.LAYER_LOCATION,
                FrenchFrontpackModel::createBodyLayer
        );
    }

    /**
     * Adds the frontpack render layer to all player renderers.
     * This includes both "default" and "slim" player models.
     */
    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        EntityModelSet modelSet = event.getEntityModels();

        // Iterate through all player skin types (default, slim)
        for (PlayerModelType skinType : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(skinType);
            if (renderer != null) {
                // Add the Frenchie frontpack layer to this player renderer
                renderer.addLayer(new FrenchFrontpackLayer(renderer, modelSet));
            }
        }
    }
}
