package net.j40climb.florafauna.client.events;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.client.ClientUtils;
import net.j40climb.florafauna.client.KeyMappings;
import net.j40climb.florafauna.common.RegisterAttachmentTypes;
import net.j40climb.florafauna.common.item.RegisterItems;
import net.j40climb.florafauna.common.item.energyhammer.EnergyHammerConfigScreen;
import net.j40climb.florafauna.common.item.energyhammer.networking.SpawnLightningPayload;
import net.j40climb.florafauna.common.item.energyhammer.networking.TeleportToSurfacePayload;
import net.j40climb.florafauna.common.item.symbiote.SymbioteData;
import net.j40climb.florafauna.common.item.symbiote.networking.DashPayload;
import net.j40climb.florafauna.common.item.symbiote.networking.JumpStatePayload;
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

    // Track previous jump key state to detect changes
    private static boolean wasJumping = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null)
            return;

        ItemStack itemStack = player.getMainHandItem();
        // The KeyMappings has to be consumed during the event or it will replay the event on each tick. Don't check anything for this outside of player null
        while (KeyMappings.SUMMON_LIGHTNING_KEY.get().consumeClick()) {
            if (itemStack.is(RegisterItems.ENERGY_HAMMER.get())) {
                Vec3 vec3 = ClientUtils.raycastFromPlayer(player, 10).getLocation();
                BlockPos targetPos = new BlockPos(new Vec3i((int) vec3.x, (int) vec3.y, (int) vec3.z));
                ClientPacketDistributor.sendToServer(new SpawnLightningPayload(targetPos));
            }
        }
        while (KeyMappings.TELEPORT_SURFACE_KEY.get().consumeClick()) {
            if (itemStack.is(RegisterItems.ENERGY_HAMMER.get())) {
                ClientPacketDistributor.sendToServer(TeleportToSurfacePayload.INSTANCE);
            }
        }
        while (KeyMappings.ENERGY_HAMMER_CONFIG_KEY.get().consumeClick()) {
            if (itemStack.is(RegisterItems.ENERGY_HAMMER.get())) {
                mc.setScreen(new EnergyHammerConfigScreen());
            }
        }
        while (KeyMappings.DASH_KEY.get().consumeClick()) {
            // Check if player has a bonded symbiote
            SymbioteData symbioteData = player.getData(RegisterAttachmentTypes.SYMBIOTE_DATA);
            if (symbioteData.bonded() && symbioteData.dash()) {
                ClientPacketDistributor.sendToServer(DashPayload.INSTANCE);
            }
        }

        // Track jump key state for variable jump height
        SymbioteData symbioteData = player.getData(RegisterAttachmentTypes.SYMBIOTE_DATA);
        if (symbioteData.bonded() && symbioteData.jumpHeight() > 0) {
            // Check current jump key state
            boolean isJumping = mc.options.keyJump.isDown();

            // If state changed, send packet to server
            if (isJumping != wasJumping) {
                ClientPacketDistributor.sendToServer(new JumpStatePayload(isJumping));
                wasJumping = isJumping;
            }
        } else {
            // Reset state if symbiote is not bonded or jumpHeight is 0
            if (wasJumping) {
                wasJumping = false;
            }
        }
    }
}