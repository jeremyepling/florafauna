package net.j40climb.florafauna.client;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.mobbarrier.menu.MobBarrierConfigScreen;
import net.j40climb.florafauna.noclip.ClippingEntity;
import net.j40climb.florafauna.noclip.NoClipClientState;
import net.j40climb.florafauna.noclip.NoClipPayload;
import net.j40climb.florafauna.common.symbiote.abilities.DashPayload;
import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.setup.ClientSetup;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
public class KeyInputEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null)
            return;

        ItemStack itemStack = player.getMainHandItem();
        // The key mappings have to be consumed during the event or it will replay the event on each tick. Don't check anything for this outside of player null
        while (ClientSetup.DASH_KEY.get().consumeClick()) {
            // Check if player has a bonded symbiote
            PlayerSymbioteData symbioteData = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
            if (symbioteData.symbioteState().areAbilitiesActive() && symbioteData.dash()) {
                ClientPacketDistributor.sendToServer(DashPayload.INSTANCE);
            }
        }
        while (ClientSetup.MOB_BARRIER_CONFIG_KEY.get().consumeClick()) {
            // Check if player is holding a MobBarrier block item
            if (itemStack.is(FloraFaunaRegistry.MOB_BARRIER.asItem())) {
                mc.setScreen(new MobBarrierConfigScreen());
            }
        }
        while (ClientSetup.NOCLIP_KEY.get().consumeClick()) {
            // Only allow noclip in creative mode
            if (player.isCreative()) {
                boolean newState = NoClipClientState.toggle();
                // Also set on local player immediately for responsive feel
                if (player instanceof ClippingEntity clippingPlayer) {
                    clippingPlayer.setClipping(newState);
                }
                // Send to server so it doesn't rubber-band us
                ClientPacketDistributor.sendToServer(new NoClipPayload(newState));
                player.sendOverlayMessage(
                        Component.translatable(newState ?
                                "command.florafauna.noclip.enabled" :
                                "command.florafauna.noclip.disabled")
                );
            } else {
                player.sendOverlayMessage(
                        Component.translatable("command.florafauna.noclip.creative_only")
                );
            }
        }
    }
}
