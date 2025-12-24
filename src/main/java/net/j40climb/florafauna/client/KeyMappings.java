package net.j40climb.florafauna.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.j40climb.florafauna.FloraFauna;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
public class KeyMappings {

    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(ResourceLocation.fromNamespaceAndPath("florafauna", "key-category"));

    // Define key mappings
    public static final Lazy<KeyMapping> SUMMON_LIGHTNING_KEY = Lazy.of(() -> new KeyMapping(
            "key.florafauna.summon_lightning", // Translation key
            KeyConflictContext.IN_GAME, // Only active in-game
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R, // Default key is R
            CATEGORY // Category in options
    ));

    public static final Lazy<KeyMapping> TELEPORT_SURFACE_KEY = Lazy.of(() -> new KeyMapping(
            "key.florafauna.teleport_surface",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            CATEGORY
    ));

    public static final Lazy<KeyMapping> DASH_KEY = Lazy.of(() -> new KeyMapping(
            "key.florafauna.dash",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_4,
            CATEGORY
    ));

    public static final Lazy<KeyMapping> ENERGY_HAMMER_CONFIG_KEY = Lazy.of(() -> new KeyMapping(
            "key.florafauna.energy_hammer_config",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            CATEGORY
    ));

    // Register key mappings
    @SubscribeEvent
    public static void registerBindings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);

        event.register(SUMMON_LIGHTNING_KEY.get());
        event.register(TELEPORT_SURFACE_KEY.get());
        event.register(DASH_KEY.get());
        event.register(ENERGY_HAMMER_CONFIG_KEY.get());
    }
}