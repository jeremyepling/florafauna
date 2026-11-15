package net.j40climb.florafauna.common.entity.frenchie.frontpack;

import com.google.common.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.entity.frenchie.frontpack.networking.PutDownFrenchiePayload;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Client input and rendering for the carried Frenchie.
 */
@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
public class FrontpackClientEvents {
    static final ContextKey<Float> ALERT_YAW = new ContextKey<>(Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "frontpack_alert_yaw"));
    private static final Identifier CREEPER_SPRITE = Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "frenchie/creeper");

    @SubscribeEvent
    public static void registerWarningHud(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "frontpack_warning"), (gui, delta) -> {
            var mc = Minecraft.getInstance();
            if (mc.player == null || mc.gui.hud.isHidden() || !mc.options.getCameraType().isFirstPerson()) return;
            var warning = mc.player.getExistingDataOrNull(FloraFaunaRegistry.FRENCHIE_WARNING);
            long now = mc.player.level().getGameTime();
            if (warning == null || !warning.visible(now)) return;
            var pack = mc.player.getExistingDataOrNull(FloraFaunaRegistry.FRENCH_FRONTPACK_DATA);
            if (pack == null || !pack.hasCarriedFrenchie()) return;
            gui.blitSprite(RenderPipelines.GUI_TEXTURED, CREEPER_SPRITE, gui.guiWidth() / 2 - 8, gui.guiHeight() - 51, 16, 16);
        });
    }

    /**
     * Registers the frontpack model layer definition.
     * This bakes the 3D geometry from FrenchFrontpackModel.createBodyLayer().
     */
    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(
                FrontpackModel.LAYER_LOCATION,
                FrontpackModel::createBodyLayer
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
                renderer.addLayer(new FrontpackLayer(renderer, modelSet));
            }
        }
    }

    @SubscribeEvent
    public static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new TypeToken<AvatarRenderer<?>>() {}, (avatar, state) -> {
            var data = avatar.getExistingDataOrNull(FloraFaunaRegistry.FRENCH_FRONTPACK_DATA);
            var warning = avatar.getExistingDataOrNull(FloraFaunaRegistry.FRENCHIE_WARNING);
            state.setRenderData(ALERT_YAW, warning != null && warning.visible(avatar.level().getGameTime())
                    ? Mth.clamp(Mth.wrapDegrees(warning.bearing() - state.bodyRot), -60F, 60F) : null);
            state.setRenderData(FrontpackLayer.CARRIED_COAT, data == null ? null : data.carriedCoat());
        });
    }

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        Player player = event.getEntity();
        var data = player.getExistingDataOrNull(FloraFaunaRegistry.FRENCH_FRONTPACK_DATA);
        if (player.isShiftKeyDown() && data != null && data.hasCarriedFrenchie()) {
            ClientPacketDistributor.sendToServer(PutDownFrenchiePayload.INSTANCE);
        }
    }
}
