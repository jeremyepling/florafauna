package net.j40climb.florafauna;

import com.mojang.logging.LogUtils;
import net.j40climb.florafauna.common.datagen.RegisterDataGenerators;
import net.j40climb.florafauna.setup.ClientSetup;
import net.j40climb.florafauna.setup.ModRegistry;
import net.j40climb.florafauna.setup.ModSetup;
import net.j40climb.florafauna.test.FloraFaunaGameTests;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

/**
 * Main mod class for FloraFauna.
 * Registration and setup are delegated to the setup package.
 */
@Mod(FloraFauna.MOD_ID)
public class FloraFauna {
    public static final String MOD_ID = "florafauna";
    private static final Logger LOGGER = LogUtils.getLogger();

    public FloraFauna(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Initialize all registrations
        ModRegistry.init(modEventBus);

        // Initialize mod setup (networking, creative tabs, events)
        ModSetup.init(modEventBus);

        // Initialize client setup (renderers, screens, key bindings)
        // Note: Client-only events only fire on client side, so no conditional needed
        ClientSetup.init(modEventBus);

        // Register datagen events on the mod bus
        modEventBus.addListener(RegisterDataGenerators::gatherClientData);
        modEventBus.addListener(RegisterDataGenerators::gatherServerData);

        // Register ourselves for server events
        NeoForge.EVENT_BUS.register(this);

        // Register game tests only when gametest namespace is enabled
        String enabledNamespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
        if (enabledNamespaces.contains(MOD_ID)) {
            FloraFaunaGameTests.register(modEventBus);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Common setup logic
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }
}
