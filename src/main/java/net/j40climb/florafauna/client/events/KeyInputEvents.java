package net.j40climb.florafauna.client.events;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.client.ClientUtils;
import net.j40climb.florafauna.client.KeyMappings;
import net.j40climb.florafauna.common.item.custom.EnergyHammerItem;
import net.j40climb.florafauna.common.network.payloadandhandlers.DashPayload;
import net.j40climb.florafauna.common.network.payloadandhandlers.SpawnLightningPayload;
import net.j40climb.florafauna.common.network.payloadandhandlers.TeleportToSurfacePayload;
import net.j40climb.florafauna.common.network.payloadandhandlers.ToggleFortureAndSilkTouchPayload;
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
import net.neoforged.neoforge.network.PacketDistributor;


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
        while (KeyMappings.SUMMON_LIGHTNING_KEY.consumeClick()) {
            if (itemStack.getItem() instanceof EnergyHammerItem energyHammerItem) {
                Vec3 vec3 = ClientUtils.raycastFromPlayer(player, 10).getLocation();
                BlockPos targetPos = new BlockPos(new Vec3i((int) vec3.x, (int) vec3.y, (int) vec3.z));
                PacketDistributor.sendToServer(new SpawnLightningPayload(targetPos));
            }
        }
        while (KeyMappings.TOGGLE_FORTUNE_AND_SILK_TOUCH.consumeClick()) {
            if (itemStack.getItem() instanceof EnergyHammerItem energyHammerItem) {
                PacketDistributor.sendToServer(ToggleFortureAndSilkTouchPayload.INSTANCE);
            }
        }
        while (KeyMappings.TELEPORT_SURFACE_KEY.consumeClick()) {
            if (itemStack.getItem() instanceof EnergyHammerItem energyHammerItem) {
                PacketDistributor.sendToServer(TeleportToSurfacePayload.INSTANCE);
            }
        }
        while (KeyMappings.DASH_KEY.consumeClick()) {
            if (itemStack.getItem() instanceof EnergyHammerItem energyHammerItem) {
                PacketDistributor.sendToServer(DashPayload.INSTANCE);
            }
        }
    }
}