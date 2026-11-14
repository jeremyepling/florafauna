package net.j40climb.florafauna.setup;

import com.google.common.reflect.TypeToken;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.world.entity.monster.spider.Spider;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

import com.mojang.blaze3d.platform.InputConstants;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.cocoonchamber.CocoonChamberScreen;
import net.j40climb.florafauna.common.block.cocoonchamber.networking.OpenCocoonScreenPayload;
import net.j40climb.florafauna.debug.DebugOverlay;
import net.j40climb.florafauna.common.item.projectile.ThrownItemRenderer;
import net.j40climb.florafauna.common.block.containmentchamber.ContainmentChamberScreen;
import net.j40climb.florafauna.common.entity.frenchie.FrenchieModel;
import net.j40climb.florafauna.common.entity.gecko.GeckoModel;
import net.j40climb.florafauna.common.entity.lizard.LizardModel;
import net.j40climb.florafauna.common.entity.frenchie.FrenchieRenderer;
import net.j40climb.florafauna.common.entity.gecko.GeckoRenderer;
import net.j40climb.florafauna.common.entity.lizard.LizardRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.j40climb.florafauna.common.block.slimegrate.SlimeSplitLayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.util.Lazy;

/**
 * Handles all client-side initialization: renderers, screens, key bindings, GUI layers.
 * Call init() from main mod class to register listeners on the mod event bus.
 * Only runs on client. Never loaded on server.
 */
public class ClientSetup {

    // ==================== KEY MAPPINGS ====================

    public static final KeyMapping.Category KEY_CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath("florafauna", "key-category"));

    public static final Lazy<KeyMapping> SUMMON_LIGHTNING_KEY = Lazy.of(() -> new KeyMapping(
            "key.florafauna.summon_lightning",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_R,
            KEY_CATEGORY
    ));

    public static final Lazy<KeyMapping> TELEPORT_SURFACE_KEY = Lazy.of(() -> new KeyMapping(
            "key.florafauna.teleport_surface",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_M,
            KEY_CATEGORY
    ));

    public static final Lazy<KeyMapping> DASH_KEY = Lazy.of(() -> new KeyMapping(
            "key.florafauna.dash",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.MOUSE,
            InputConstants.MOUSE_BUTTON_4,
            KEY_CATEGORY
    ));

    public static final Lazy<KeyMapping> HAMMER_CONFIG_KEY = Lazy.of(() -> new KeyMapping(
            "key.florafauna.hammer_config",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_P,
            KEY_CATEGORY
    ));

    public static final Lazy<KeyMapping> THROW_ITEM_KEY = Lazy.of(() -> new KeyMapping(
            "key.florafauna.throw_item",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_T,
            KEY_CATEGORY
    ));

    public static final Lazy<KeyMapping> CYCLE_MINING_MODE_KEY = Lazy.of(() -> new KeyMapping(
            "key.florafauna.cycle_mining_mode",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_C,
            KEY_CATEGORY
    ));

    public static final Lazy<KeyMapping> PRECISION_MINING_KEY = Lazy.of(() -> new KeyMapping(
            "key.florafauna.precision_mining",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_J,
            KEY_CATEGORY
    ));

    public static final Lazy<KeyMapping> MOB_BARRIER_CONFIG_KEY = Lazy.of(() -> new KeyMapping(
            "key.florafauna.mob_barrier_config",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_B,
            KEY_CATEGORY
    ));

    public static final Lazy<KeyMapping> HAMMER_ACTION_WHEEL_KEY = Lazy.of(() -> new KeyMapping(
            "key.florafauna.hammer_action_wheel",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_K,
            KEY_CATEGORY
    ));

    public static final Lazy<KeyMapping> NOCLIP_KEY = Lazy.of(() -> new KeyMapping(
            "key.florafauna.noclip",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_N,
            KEY_CATEGORY
    ));

    // ==================== GUI LAYERS ====================

    public static final Identifier SYMBIOTE_DEBUG_LAYER_ID = Identifier.fromNamespaceAndPath(
            FloraFauna.MOD_ID, "symbiote_debug");

    // ==================== INITIALIZATION ====================

    /**
     * Registers all client-side event listeners on the mod event bus.
     * Call this from the main mod class.
     */
    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(ClientSetup::registerLayers);
        modEventBus.addListener(ClientSetup::registerRenderers);
        modEventBus.addListener(SlimeSplitLayer::addLayer);
        modEventBus.addListener(SlimeSplitLayer::registerState);
        modEventBus.addListener(ClientSetup::registerSilkPose);
        modEventBus.addListener(ClientSetup::registerScreens);
        modEventBus.addListener(ClientSetup::registerPayloads);
        modEventBus.addListener(ClientSetup::registerKeyBindings);
        modEventBus.addListener(ClientSetup::registerGuiLayers);
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GeckoModel.GECKO, GeckoModel::createBodyLayer);
        event.registerLayerDefinition(LizardModel.LIZARD, LizardModel::createBodyLayer);
        event.registerLayerDefinition(FrenchieModel.FRENCHIE, FrenchieModel::createBodyLayer);
    }

    // ==================== EVENT HANDLERS ====================

    private static void registerSilkPose(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new TypeToken<SpiderRenderer<Spider>>() {}, (spider, state) -> {
            if (spider.hasData(FloraFaunaRegistry.SILK_HANGING) && spider.getData(FloraFaunaRegistry.SILK_HANGING)) state.isUpsideDown = true;
        });
    }

    private static void registerPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(OpenCocoonScreenPayload.TYPE, (data, context) ->
                Minecraft.getInstance().gui.setScreen(new CocoonChamberScreen(data.chamberPos())));
    }

    /**
     * Client setup: register entity renderers.
     */
    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(FloraFaunaRegistry.GECKO.get(), GeckoRenderer::new);
        event.registerEntityRenderer(FloraFaunaRegistry.LIZARD.get(), LizardRenderer::new);
        event.registerEntityRenderer(FloraFaunaRegistry.FRENCHIE.get(), FrenchieRenderer::new);
        event.registerEntityRenderer(FloraFaunaRegistry.THROWN_ITEM.get(), ThrownItemRenderer::new);
    }

    /**
     * Register menu screens.
     */
    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(FloraFaunaRegistry.CONTAINMENT_CHAMBER_MENU.get(), ContainmentChamberScreen::new);
    }

    /**
     * Register key bindings.
     */
    private static void registerKeyBindings(RegisterKeyMappingsEvent event) {
        event.registerCategory(KEY_CATEGORY);

        event.register(SUMMON_LIGHTNING_KEY.get());
        event.register(TELEPORT_SURFACE_KEY.get());
        event.register(DASH_KEY.get());
        event.register(HAMMER_CONFIG_KEY.get());
        event.register(THROW_ITEM_KEY.get());
        event.register(CYCLE_MINING_MODE_KEY.get());
        event.register(PRECISION_MINING_KEY.get());
        event.register(MOB_BARRIER_CONFIG_KEY.get());
        event.register(HAMMER_ACTION_WHEEL_KEY.get());
        event.register(NOCLIP_KEY.get());
    }

    /**
     * Register GUI layers (HUD overlays).
     */
    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(SYMBIOTE_DEBUG_LAYER_ID, (GuiLayer) new DebugOverlay());
    }
}
