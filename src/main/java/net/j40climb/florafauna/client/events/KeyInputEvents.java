package net.j40climb.florafauna.client.events;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.client.ClientUtils;
import net.j40climb.florafauna.client.KeyMappings;
import net.j40climb.florafauna.common.RegisterAttachmentTypes;
import net.j40climb.florafauna.common.RegisterDataComponentTypes;
import net.j40climb.florafauna.common.item.abilities.menu.ToolConfigScreen;
import net.j40climb.florafauna.common.item.abilities.networking.SpawnLightningPayload;
import net.j40climb.florafauna.common.item.abilities.networking.TeleportToSurfacePayload;
import net.j40climb.florafauna.common.item.symbiote.SymbioteData;
import net.j40climb.florafauna.common.item.symbiote.abilities.DashPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
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
        // The KeyMappings has to be consumed during the event or it will replay the event on each tick. Don't check anything for this outside of player null
        while (KeyMappings.SUMMON_LIGHTNING_KEY.get().consumeClick()) {
            // Component-based check - works with any item that has LIGHTNING_ABILITY
            if (itemStack.has(RegisterDataComponentTypes.LIGHTNING_ABILITY)) {
                Vec3 vec3 = ClientUtils.raycastFromPlayer(player, 10).getLocation();
                BlockPos targetPos = new BlockPos(new Vec3i((int) vec3.x, (int) vec3.y, (int) vec3.z));
                ClientPacketDistributor.sendToServer(new SpawnLightningPayload(targetPos));
            }
        }
        while (KeyMappings.TELEPORT_SURFACE_KEY.get().consumeClick()) {
            // Component-based check - works with any item that has TELEPORT_SURFACE_ABILITY
            if (itemStack.has(RegisterDataComponentTypes.TELEPORT_SURFACE_ABILITY)) {
                ClientPacketDistributor.sendToServer(TeleportToSurfacePayload.INSTANCE);
            }
        }
        while (KeyMappings.ENERGY_HAMMER_CONFIG_KEY.get().consumeClick()) {
            // Component-based check - works with any item that has TOOL_CONFIG
            if (itemStack.has(RegisterDataComponentTypes.TOOL_CONFIG)) {
                mc.setScreen(new ToolConfigScreen());
            }
        }
        while (KeyMappings.DASH_KEY.get().consumeClick()) {
            // Check if player has a bonded symbiote
            SymbioteData symbioteData = player.getData(RegisterAttachmentTypes.SYMBIOTE_DATA);
            if (symbioteData.bonded() && symbioteData.dash()) {
                ClientPacketDistributor.sendToServer(DashPayload.INSTANCE);
            }
        }
    }
}