package net.j40climb.florafauna.common;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.entity.frontpack.networking.PutDownFrenchiePayload;
import net.j40climb.florafauna.common.item.energyhammer.networking.SetMiningSpeedPayload;
import net.j40climb.florafauna.common.item.energyhammer.networking.SpawnLightningPayload;
import net.j40climb.florafauna.common.item.energyhammer.networking.TeleportToSurfacePayload;
import net.j40climb.florafauna.common.item.energyhammer.networking.ToggleFortureAndSilkTouchPayload;
import net.j40climb.florafauna.common.item.symbiote.networking.DashPayload;
import net.j40climb.florafauna.common.item.symbiote.networking.JumpStatePayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = FloraFauna.MOD_ID)
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
        registrar.playToServer(PutDownFrenchiePayload.TYPE, PutDownFrenchiePayload.STREAM_CODEC, PutDownFrenchiePayload::onServerReceived);
        registrar.playToServer(JumpStatePayload.TYPE, JumpStatePayload.STREAM_CODEC, JumpStatePayload::onServerReceived);
    }
}
