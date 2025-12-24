package net.j40climb.florafauna.common;

import net.j40climb.florafauna.common.entity.frontpack.networking.PutDownFrenchiePayload;
import net.j40climb.florafauna.common.item.energyhammer.networking.SetMiningSpeedPayload;
import net.j40climb.florafauna.common.item.energyhammer.networking.SpawnLightningPayload;
import net.j40climb.florafauna.common.item.energyhammer.networking.TeleportToSurfacePayload;
import net.j40climb.florafauna.common.item.energyhammer.networking.UpdateEnergyHammerConfigPayload;
import net.j40climb.florafauna.common.item.symbiote.networking.DashPayload;
import net.j40climb.florafauna.common.item.symbiote.networking.JumpStatePayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class RegisterNetworking {
    public static void register(final RegisterPayloadHandlersEvent event) {
        // Sets the current network version
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SpawnLightningPayload.TYPE, SpawnLightningPayload.STREAM_CODEC, SpawnLightningPayload::onServerReceived);
        registrar.playToServer(TeleportToSurfacePayload.TYPE, TeleportToSurfacePayload.STREAM_CODEC, TeleportToSurfacePayload::onServerReceived);
        registrar.playToServer(SetMiningSpeedPayload.TYPE, SetMiningSpeedPayload.STREAM_CODEC, SetMiningSpeedPayload::onServerReceived);
        registrar.playToServer(UpdateEnergyHammerConfigPayload.TYPE, UpdateEnergyHammerConfigPayload.STREAM_CODEC, UpdateEnergyHammerConfigPayload::onServerReceived);
        registrar.playToServer(DashPayload.TYPE, DashPayload.STREAM_CODEC, DashPayload::onServerReceived);
        registrar.playToServer(PutDownFrenchiePayload.TYPE, PutDownFrenchiePayload.STREAM_CODEC, PutDownFrenchiePayload::onServerReceived);
        registrar.playToServer(JumpStatePayload.TYPE, JumpStatePayload.STREAM_CODEC, JumpStatePayload::onServerReceived);
    }
}
