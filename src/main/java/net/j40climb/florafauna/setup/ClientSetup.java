package net.j40climb.florafauna.setup;

import com.mojang.blaze3d.platform.InputConstants;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.client.SymbioteDebugOverlay;
import net.j40climb.florafauna.common.block.cocoonchamber.CocoonChamberScreen;
import net.j40climb.florafauna.common.block.containmentchamber.ContainmentChamberScreen;
import net.j40climb.florafauna.common.entity.frenchie.FrenchieRenderer;
import net.j40climb.florafauna.common.entity.gecko.GeckoRenderer;
import net.j40climb.florafauna.common.entity.lizard.LizardRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

/**
 * Handles all client-side initialization: renderers, screens, key bindings, GUI layers.
 * Call init() from main mod class to register listeners on the mod event bus.
 */
public class ClientSetup {

    // ==================== KEY MAPPINGS ====================

    public static final KeyMapping.Category KEY_CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath("florafauna", "key-category"));

    public static final Lazy<KeyMapping> SUMMON_LIGHTNING_KEY = Lazy.of(() -> new KeyMapping(
            "key.florafauna.summon_lightning",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KEY_CATEGORY
    ));

    public static final Lazy<KeyMapping> TELEPORT_SURFACE_KEY = Lazy.of(() -> new KeyMapping(
            "key.florafauna.teleport_surface",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            KEY_CATEGORY
    ));

    public static final Lazy<KeyMapping> DASH_KEY = Lazy.of(() -> new KeyMapping(
            "key.florafauna.dash",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_4,
            KEY_CATEGORY
    ));

    public static final Lazy<KeyMapping> HAMMER_CONFIG_KEY = Lazy.of(() -> new KeyMapping(
            "key.florafauna.hammer_config",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
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
        modEventBus.addListener(ClientSetup::onClientSetup);
        modEventBus.addListener(ClientSetup::registerScreens);
        modEventBus.addListener(ClientSetup::registerKeyBindings);
        modEventBus.addListener(ClientSetup::registerGuiLayers);
    }

    // ==================== EVENT HANDLERS ====================

    /**
     * Client setup: register entity renderers.
     */
    private static void onClientSetup(FMLClientSetupEvent event) {
        EntityRenderers.register(FloraFaunaRegistry.GECKO.get(), GeckoRenderer::new);
        EntityRenderers.register(FloraFaunaRegistry.LIZARD.get(), LizardRenderer::new);
        EntityRenderers.register(FloraFaunaRegistry.FRENCHIE.get(), FrenchieRenderer::new);
    }

    /**
     * Register menu screens.
     */
    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(FloraFaunaRegistry.CONTAINMENT_CHAMBER_MENU.get(), ContainmentChamberScreen::new);
        event.register(FloraFaunaRegistry.COCOON_CHAMBER_MENU.get(), CocoonChamberScreen::new);
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
    }

    /**
     * Register GUI layers (HUD overlays).
     */
    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(SYMBIOTE_DEBUG_LAYER_ID, (GuiLayer) new SymbioteDebugOverlay());
    }
}
