package net.j40climb.florafauna.setup;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.j40climb.florafauna.client.KeyInputEvents;
import net.j40climb.florafauna.common.block.cocoonchamber.CocoonChamberScreen;
import net.j40climb.florafauna.common.block.cocoonchamber.networking.OpenCocoonScreenPayload;
import net.j40climb.florafauna.common.block.containmentchamber.ContainmentChamberScreen;
import net.j40climb.florafauna.common.block.husk.HuskParticleHandler;
import net.j40climb.florafauna.common.block.iteminput.ItemInputAnimationTracker;
import net.j40climb.florafauna.common.block.iteminput.rootiteminput.networking.ItemInputAnimationPayload;
import net.j40climb.florafauna.common.block.mininganchor.networking.AnchorFillStatePayload;
import net.j40climb.florafauna.common.block.mobbarrier.FabricMobBarrierOutlines;
import net.j40climb.florafauna.common.block.slimegrate.SlimeSplitLayer;
import net.j40climb.florafauna.common.entity.gecko.*;
import net.j40climb.florafauna.common.entity.lizard.*;
import net.j40climb.florafauna.debug.DebugOverlay;
import net.j40climb.florafauna.noclip.NoClipClientState;
import net.j40climb.florafauna.platform.AttachmentAccess;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.function.Supplier;

/** Client-only Fabric registrations; shared input and screens use this bridge on both loaders. */
public final class ClientSetup implements ClientModInitializer {
    public static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("florafauna", "key-category"));
    public static final Supplier<KeyMapping> DASH_KEY = Suppliers.memoize(() -> new KeyMapping(
            "key.florafauna.dash", InputConstants.Type.MOUSE, InputConstants.MOUSE_BUTTON_4, KEY_CATEGORY));
    public static final Supplier<KeyMapping> MOB_BARRIER_CONFIG_KEY = Suppliers.memoize(() -> new KeyMapping(
            "key.florafauna.mob_barrier_config", InputConstants.Type.KEYSYM, InputConstants.KEY_B, KEY_CATEGORY));
    public static final Supplier<KeyMapping> NOCLIP_KEY = Suppliers.memoize(() -> new KeyMapping(
            "key.florafauna.noclip", InputConstants.Type.KEYSYM, InputConstants.KEY_N, KEY_CATEGORY));

    public static void sendToServer(CustomPacketPayload payload) { ClientPlayNetworking.send(payload); }

    @SuppressWarnings("unchecked") // Hammer exports this callback under its own namespaced protocol key.
    private static void connectHammerDiagnostics() {
        Object provider = FabricLoader.getInstance().getObjectShare().get("florafauna_hammer:tool_diagnostics");
        if (provider instanceof java.util.function.BiConsumer<?, ?>) {
            DebugOverlay.setHammerDiagnostics((java.util.function.BiConsumer<net.minecraft.world.item.ItemStack, java.util.List<String>>) provider);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerSlimeLayer(LivingEntityRenderLayerRegistrationCallback.RegistrationHelper layers,
                                          SlimeRenderer renderer, net.minecraft.client.model.geom.EntityModelSet models) {
        // Vanilla SlimeModel accepts EntityRenderState; this layer supplies its SlimeRenderState subtype.
        // Fabric's helper requires an exact model-state match, narrower than vanilla RenderLayer's bound.
        layers.register((net.minecraft.client.renderer.entity.layers.RenderLayer) new SlimeSplitLayer(renderer, models));
    }

    @Override
    public void onInitializeClient() {
        // All client initializers have run before CLIENT_STARTED, regardless of optional mod ordering.
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> connectHammerDiagnostics());
        KeyMappingHelper.registerKeyMapping(DASH_KEY.get());
        KeyMappingHelper.registerKeyMapping(MOB_BARRIER_CONFIG_KEY.get());
        KeyMappingHelper.registerKeyMapping(NOCLIP_KEY.get());
        ModelLayerRegistry.registerModelLayer(GeckoModel.GECKO, GeckoModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(LizardModel.LIZARD, LizardModel::createBodyLayer);
        EntityRendererRegistry.register(FloraFaunaRegistry.GECKO.get(), GeckoRenderer::new);
        EntityRendererRegistry.register(FloraFaunaRegistry.LIZARD.get(), LizardRenderer::new);
        MenuScreens.register(FloraFaunaRegistry.CONTAINMENT_CHAMBER_MENU.get(), ContainmentChamberScreen::new);
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, layers, context) -> {
            if (renderer instanceof SlimeRenderer slime) registerSlimeLayer(layers, slime, context.getModelSet());
        });
        ClientPlayNetworking.registerGlobalReceiver(OpenCocoonScreenPayload.TYPE, (payload, context) ->
                Minecraft.getInstance().setScreen(new CocoonChamberScreen(payload.chamberPos())));
        ClientPlayNetworking.registerGlobalReceiver(ItemInputAnimationPayload.TYPE, (payload, context) ->
                ItemInputAnimationTracker.startAnimation(payload.entityId(), payload.targetPos(), payload.animationDurationTicks()));
        ClientPlayNetworking.registerGlobalReceiver(AnchorFillStatePayload.TYPE, (payload, context) -> {
            var player = context.player();
            var data = AttachmentAccess.get(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
            if (data.hasActiveWaypointAnchor() && payload.anchorPos().equals(data.activeWaypointAnchorPos())) {
                AttachmentAccess.set(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA, data.withLastAnnouncedFillState(payload.fillState()));
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            KeyInputEvents.onClientTick();
            if (client.player != null) HuskParticleHandler.tick(client.player);
        });
        ClientTickEvents.END_LEVEL_TICK.register(level -> ItemInputAnimationTracker.tick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            NoClipClientState.setEnabled(false);
            ItemInputAnimationTracker.clearAll();
        });
        HudElementRegistry.addLast(DebugOverlay.LAYER_ID, new DebugOverlay()::render);
        FabricMobBarrierOutlines.register();
        net.j40climb.florafauna.client.FabricClientCommands.register();
    }
}
