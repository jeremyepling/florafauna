package net.j40climb.florafauna.client.events;

import com.mojang.blaze3d.platform.InputConstants;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.client.KeyMappings;
import net.j40climb.florafauna.item.custom.HammerItem;
import net.j40climb.florafauna.network.payloadandhandlers.SpawnLightningPayload;
import net.j40climb.florafauna.network.payloadandhandlers.TeleportToSurfacePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;


@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
public class KeyInputEvents {

    @SubscribeEvent
    public static void handleEventInput(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null)
            return;

        ItemStack hammerItemStack = player.getMainHandItem();

        // Check if the hammer is in hand and a keybind is pressed
        if (hammerItemStack.getItem() instanceof HammerItem hammerItem) {
            while (KeyMappings.THROW_HAMMER_KEY.consumeClick()) {
                BlockPos targetPos = player.blockPosition().offset((int) (player.getLookAngle().x * 5), (int)player.getLookAngle().y * 5, (int)player.getLookAngle().z * 5);

                PacketDistributor.sendToServer(new SpawnLightningPayload(targetPos));
            }
            while (KeyMappings.TELEPORT_SURFACE_KEY.consumeClick()) {
                PacketDistributor.sendToServer(TeleportToSurfacePayload.INSTANCE);
            }
        }
    }

    // Handling key presses
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null)
            return;
        Player player = mc.player;
        if (event.getAction() == InputConstants.PRESS) {
            for (int i = 0; i < mc.player.getInventory().items.size(); i++) {
                ItemStack itemStack = mc.player.getInventory().getItem(i);
                if (itemStack.getItem() instanceof HammerItem hammerItem ) {
//                    activateAbilities(itemStack, event.getKey(), toggleableTool, player, i, false);
                }
            }
        }
    }

    // Handling mouse clicks
    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null || event.getButton() == 0 || event.getButton() == 1 || event.getAction() != InputConstants.PRESS)
            return;
        Player player = mc.player;
        for (int i = 0; i < mc.player.getInventory().items.size(); i++) {
            ItemStack itemStack = mc.player.getInventory().getItem(i);
            if (itemStack.getItem() instanceof HammerItem hammerItem ) {
//                activateAbilities(itemStack, event.getButton(), toggleableTool, player, i, true);
            }
        }
    }

    private static void activateAbilities(ItemStack itemStack, int key, HammerItem hammerItem, Player player, int invSlot, boolean isMouse) {
//        List<Ability> abilities = LeftClickableTool.getCustomBindingListFor(itemStack, key, isMouse, player);
//        if (!abilities.isEmpty()) {
//            //Do them client side and Server side, since some abilities (like ore scanner) are client side activated.
//            toggleableTool.useAbility(player.level(), player, itemStack, key, isMouse);
//            BlockHitResult blockHitResult = getHitResult(player);
//            if (blockHitResult.getType() == HitResult.Type.BLOCK) {
//                UseOnContext useoncontext = new UseOnContext(player.level(), player, InteractionHand.MAIN_HAND, itemStack, blockHitResult);
//                toggleableTool.useOnAbility(useoncontext, itemStack, key, isMouse);
//            }
//            PacketDistributor.sendToServer(new LeftClickPayload(0, false, BlockPos.ZERO, -1, invSlot, key, isMouse)); //Type 0 == air
    }
}