package net.j40climb.florafauna.network;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.network.payloadandhandlers.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = FloraFauna.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class RegisterNetworking {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        // Sets the current network version
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SpawnLightningPayload.TYPE, SpawnLightningPayload.STREAM_CODEC, SpawnLightningPayload::onServerReceived);
        registrar.playToServer(TeleportToSurfacePayload.TYPE, TeleportToSurfacePayload.STREAM_CODEC, TeleportToSurfacePayload::onServerReceived);
        registrar.playToServer(ToggleFortureAndSilkTouchPayload.TYPE, ToggleFortureAndSilkTouchPayload.STREAM_CODEC, ToggleFortureAndSilkTouchPayload::onServerReceived);
        registrar.playToServer(SetMiningSpeedPayload.TYPE, SetMiningSpeedPayload.STREAM_CODEC, SetMiningSpeedPayload::onServerReceived);
        registrar.playToServer(DashPayload.TYPE, DashPayload.STREAM_CODEC, DashPayload::onServerReceived);
    }
}
