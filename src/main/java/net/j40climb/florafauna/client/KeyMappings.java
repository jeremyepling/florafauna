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

@EventBusSubscriber(modid = FloraFauna.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KeyMappings {
    public static final String CATEGORY = "key.categories.florafauna";

    // Define key mappings
    public static final KeyMapping THROW_HAMMER_KEY = new KeyMapping(
            "key.florafauna.throw_hammer", // Translation key
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

    // Register key mappings
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(THROW_HAMMER_KEY);
        event.register(TELEPORT_SURFACE_KEY);
    }

}