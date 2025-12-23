package net.j40climb.florafauna.client.events;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.client.model.FrenchieBackpackModel;
import net.j40climb.florafauna.client.renderer.layer.FrenchieBackpackLayer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Handles registration of the Frenchie backpack model layer and adding it to player renderers.
 * Must be on the MOD event bus for registration events.
 */
@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
public class BackpackRendererEvents {

    /**
     * Registers the backpack model layer definition.
     * This bakes the 3D geometry from FrenchieBackpackModel.createBodyLayer().
     */
    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(
                FrenchieBackpackModel.LAYER_LOCATION,
                FrenchieBackpackModel::createBodyLayer
        );
    }

    /**
     * Adds the backpack render layer to all player renderers.
     * This includes both "default" and "slim" player models.
     */
    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        EntityModelSet modelSet = event.getEntityModels();

        // Iterate through all player skin types (default, slim)
        for (PlayerModelType skinType : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(skinType);
            if (renderer != null) {
                // Add the Frenchie backpack layer to this player renderer
                renderer.addLayer(new FrenchieBackpackLayer(renderer, modelSet));
            }
        }
    }
}
