package net.j40climb.florafauna.dev;

import net.j40climb.florafauna.FloraFauna;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Development-only world setup that runs on player join.
 * Only active in dev environment (not in production builds).
 *
 * Sets up a comfortable dev environment:
 * - Unlocks all recipes
 * - Disables advancement announcements
 * - Sets time to day (1000)
 * - Stops daylight cycle
 * - Stops weather cycle
 * - Sets easy difficulty
 *
 * To reset: delete the world and recreate it.
 * World should be created as: Superflat, Creative, Tunneler's Dream preset
 */
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public class DevWorldSetup {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // Only run in dev environment (not production)
        if (!FMLEnvironment.isProduction() && event.getEntity() instanceof ServerPlayer player) {
            setupDevWorld(player);
        }
    }

    private static void setupDevWorld(ServerPlayer player) {
        ServerLevel serverLevel = player.level();
        MinecraftServer server = serverLevel.getServer();
        var rules = serverLevel.getGameRules();
        rules.set(GameRules.SHOW_ADVANCEMENT_MESSAGES, false, server);
        rules.set(GameRules.ADVANCE_TIME, false, server);
        rules.set(GameRules.ADVANCE_WEATHER, false, server);
        player.awardRecipes(server.getRecipeManager().getRecipes());
        serverLevel.dimensionType().defaultClock().ifPresent(clock ->
                server.clockManager().moveToTimeMarker(clock, ClockTimeMarkers.DAY));
        server.setWeatherParameters(ServerLevel.RAIN_DELAY.sample(serverLevel.getRandom()), 0, false, false);
        server.setDifficulty(Difficulty.EASY, true);
    }
}
