package net.j40climb.florafauna;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.j40climb.florafauna.common.block.wood.WoodStripping;
import net.j40climb.florafauna.setup.FabricGameplayEvents;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.j40climb.florafauna.setup.FloraFaunaSetup;
import net.j40climb.florafauna.test.FloraFaunaGameTests;

/** Fabric entrypoint; gameplay and resources are shared with NeoForge. */
public final class FloraFauna implements ModInitializer {
    public static final String MOD_ID = "florafauna";

    @Override
    public void onInitialize() {
        Config.SPEC.watch(FabricLoader.getInstance().getConfigDir().resolve("florafauna-common.toml"), Config::loadConfig);
        Config.loadConfig();
        FloraFaunaRegistry.init();
        net.j40climb.florafauna.platform.SilkCooldown.init();
        WoodStripping.init();
        FloraFaunaSetup.init();
        FabricGameplayEvents.register();
        if (System.getProperty("fabric-api.gametest") != null) FloraFaunaGameTests.registerFabric();
    }
}
