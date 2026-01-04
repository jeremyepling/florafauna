package net.j40climb.florafauna.common;

import net.j40climb.florafauna.FloraFauna;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * - Sets peaceful difficulty
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
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        MinecraftServer server = serverLevel.getServer();
        var source = player.createCommandSourceStack().withSuppressedOutput();
        var commands = server.getCommands();

        // Unlock all recipes
        commands.performPrefixedCommand(source, "recipe give @s *");

        // Set gamerules
        commands.performPrefixedCommand(source, "gamerule announceAdvancements false");
        commands.performPrefixedCommand(source, "gamerule doDaylightCycle false");
        commands.performPrefixedCommand(source, "gamerule doWeatherCycle false");

        // Set time to day and clear weather
        commands.performPrefixedCommand(source, "time set day");
        commands.performPrefixedCommand(source, "weather clear");

        // Set peaceful
        commands.performPrefixedCommand(source, "difficulty peaceful");
    }
}
