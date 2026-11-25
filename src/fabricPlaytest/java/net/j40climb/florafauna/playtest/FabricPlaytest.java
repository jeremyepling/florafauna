package net.j40climb.florafauna.playtest;

import java.io.IOException;
import java.io.UncheckedIOException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;

/** Test-only server callbacks for the existing world-copy and two-player fixtures. */
public final class FabricPlaytest implements ModInitializer {
    @Override public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            BloomDensityProbe.onServerStarted(server);
            try { HuskMultiplayerServer.start(server); }
            catch (IOException error) { throw new UncheckedIOException(error); }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            try { HuskMultiplayerServer.tick(server); }
            catch (IOException error) { throw new UncheckedIOException(error); }
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!level.isClientSide()) HuskMultiplayerServer.onClick(player, hit.getBlockPos());
            return InteractionResult.PASS;
        });
    }
}
