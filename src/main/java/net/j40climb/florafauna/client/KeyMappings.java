package net.j40climb.florafauna.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.j40climb.florafauna.FloraFauna;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
public class KeyMappings {
    public static final String CATEGORY = "key.categories.florafauna";

    // Define key mappings
    public static final KeyMapping SUMMON_LIGHTNING_KEY = new KeyMapping(
            "key.florafauna.summon_lightning", // Translation key
            KeyConflictContext.IN_GAME, // Only active in-game
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R, // Default key is R
            CATEGORY // Category in options
    );

    public static final KeyMapping TELEPORT_SURFACE_KEY = new KeyMapping(
            "key.florafauna.teleport_surface",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            CATEGORY
    );

    public static final KeyMapping TOGGLE_FORTUNE_AND_SILK_TOUCH = new KeyMapping(
            "key.florafauna.toggle_fortune_and_silk_touch",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_MOUSE_BUTTON_4,
            CATEGORY
    );

    public static final KeyMapping DASH_KEY = new KeyMapping(
            "key.florafauna.dash",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_MOUSE_BUTTON_5,
            CATEGORY
    );

    // Register key mappings
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(SUMMON_LIGHTNING_KEY);
        event.register(TELEPORT_SURFACE_KEY);
        event.register(TOGGLE_FORTUNE_AND_SILK_TOUCH);
        event.register(DASH_KEY);
    }

}