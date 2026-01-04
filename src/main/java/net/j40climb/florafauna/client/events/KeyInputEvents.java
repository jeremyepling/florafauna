package net.j40climb.florafauna.client.events;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.client.ClientUtils;
import net.j40climb.florafauna.common.block.mobbarrier.menu.MobBarrierConfigScreen;
import net.j40climb.florafauna.common.item.abilities.menu.ToolConfigScreen;
import net.j40climb.florafauna.common.item.abilities.networking.CycleMiningModePayload;
import net.j40climb.florafauna.common.item.abilities.networking.SpawnLightningPayload;
import net.j40climb.florafauna.common.item.abilities.networking.TeleportToSurfacePayload;
import net.j40climb.florafauna.common.item.abilities.networking.ThrowItemPayload;
import net.j40climb.florafauna.common.symbiote.abilities.DashPayload;
import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.setup.ClientSetup;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
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
        while (ClientSetup.SUMMON_LIGHTNING_KEY.get().consumeClick()) {
            // Component-based check - works with any item that has LIGHTNING_ABILITY
            if (itemStack.has(FloraFaunaRegistry.LIGHTNING_ABILITY)) {
                Vec3 vec3 = ClientUtils.raycastFromPlayer(player, 10).getLocation();
                BlockPos targetPos = new BlockPos(new Vec3i((int) vec3.x, (int) vec3.y, (int) vec3.z));
                ClientPacketDistributor.sendToServer(new SpawnLightningPayload(targetPos));
            }
        }
        while (ClientSetup.TELEPORT_SURFACE_KEY.get().consumeClick()) {
            // Component-based check - works with any item that has TELEPORT_SURFACE_ABILITY
            if (itemStack.has(FloraFaunaRegistry.TELEPORT_SURFACE_ABILITY)) {
                ClientPacketDistributor.sendToServer(TeleportToSurfacePayload.INSTANCE);
            }
        }
        while (ClientSetup.HAMMER_CONFIG_KEY.get().consumeClick()) {
            // Component-based check - works with any item that has TOOL_CONFIG
            if (itemStack.has(FloraFaunaRegistry.TOOL_CONFIG)) {
                mc.setScreen(new ToolConfigScreen());
            }
        }
        while (ClientSetup.DASH_KEY.get().consumeClick()) {
            // Check if player has a bonded symbiote
            PlayerSymbioteData symbioteData = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
            if (symbioteData.symbioteState().areAbilitiesActive() && symbioteData.dash()) {
                ClientPacketDistributor.sendToServer(DashPayload.INSTANCE);
            }
        }
        while (ClientSetup.THROW_ITEM_KEY.get().consumeClick()) {
            // Component-based check - works with any item that has THROWABLE_ABILITY
            if (itemStack.has(FloraFaunaRegistry.THROWABLE_ABILITY)) {
                // Play throw animation (arm swing)
                player.swing(InteractionHand.MAIN_HAND);
                ClientPacketDistributor.sendToServer(new ThrowItemPayload(true));
            }
        }
        while (ClientSetup.CYCLE_MINING_MODE_KEY.get().consumeClick()) {
            // Component-based check - works with any item that has MULTI_BLOCK_MINING
            if (itemStack.has(FloraFaunaRegistry.MULTI_BLOCK_MINING)) {
                ClientPacketDistributor.sendToServer(CycleMiningModePayload.INSTANCE);
            }
        }
        while (ClientSetup.MOB_BARRIER_CONFIG_KEY.get().consumeClick()) {
            // Check if player is holding a MobBarrier block item
            if (itemStack.is(FloraFaunaRegistry.MOB_BARRIER.asItem())) {
                mc.setScreen(new MobBarrierConfigScreen());
            }
        }
    }
}