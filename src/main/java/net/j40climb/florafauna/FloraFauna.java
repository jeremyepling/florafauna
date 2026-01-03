package net.j40climb.florafauna;

import com.mojang.logging.LogUtils;
import net.j40climb.florafauna.common.datagen.FloraFaunaDataGenerators;
import net.j40climb.florafauna.setup.ClientSetup;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.j40climb.florafauna.setup.FloraFaunaSetup;
import net.j40climb.florafauna.test.FloraFaunaGameTests;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLLoader;
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
        // Register configs
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC, "florafauna-common.toml");
        modEventBus.addListener(this::onConfigLoad);

        // Initialize all registrations
        FloraFaunaRegistry.init(modEventBus);

        // Initialize mod setup (networking, creative tabs, events)
        FloraFaunaSetup.init(modEventBus);

        // Initialize client setup (renderers, screens, key bindings)
        if (FMLLoader.getCurrent().getDist().isClient()) {
            ClientSetup.init(modEventBus);
        }
        // Register datagen events on the mod bus
        modEventBus.addListener(FloraFaunaDataGenerators::gatherClientData);
        modEventBus.addListener(FloraFaunaDataGenerators::gatherServerData);

        // Register ourselves for server events
        NeoForge.EVENT_BUS.register(this);

        // Register game tests only when gametest namespace is enabled
        String enabledNamespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
        if (enabledNamespaces.contains(MOD_ID)) {
            FloraFaunaGameTests.register(modEventBus);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    private void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() == Config.SPEC) {
            Config.loadConfig();
        }
    }
}
