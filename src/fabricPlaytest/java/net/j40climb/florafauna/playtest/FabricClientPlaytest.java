package net.j40climb.florafauna.playtest;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

/** Hooks the same player-action driver into Fabric client ticks. */
public final class FabricClientPlaytest implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Playtest.tick();
            HuskMultiplayerClient.tick();
        });
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> HuskMultiplayerClient.onMessage(message));
    }
}
