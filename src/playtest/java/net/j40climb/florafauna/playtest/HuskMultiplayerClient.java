package net.j40climb.florafauna.playtest;

import net.j40climb.florafauna.FloraFauna;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if neoforge {
import net.neoforged.api.distmarker.Dist;
//?}
//? if neoforge {
import net.neoforged.bus.api.SubscribeEvent;
//?}
//? if neoforge {
import net.neoforged.fml.common.EventBusSubscriber;
//?}
//? if neoforge {
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
//?}
//? if neoforge {
import net.neoforged.neoforge.client.event.ClientTickEvent;
//?}

/** The two playtest clients use the same block interaction and respawn packets as regular players. */
//? if neoforge {
@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
//?}
public final class HuskMultiplayerClient {
    private static final boolean ENABLED = System.getProperty("florafauna.huskTest.client") != null;
    private static boolean click, stop;
    private static int readyTicks;

    //? if neoforge {
    @SubscribeEvent
    public static void onMessage(ClientChatReceivedEvent.System event) { onMessage(event.getMessage()); }
    //?}
    public static void onMessage(net.minecraft.network.chat.Component message) {
        if (!ENABLED) return;
        switch (message.getString()) {
            case HuskMultiplayerServer.CLICK -> {
                click = true;
                readyTicks = 0;
            }
            case HuskMultiplayerServer.DONE -> stop = true;
        }
    }

    //? if neoforge {
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) { tick(); }
    //?}
    public static void tick() {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        if (stop) {
            mc.stop();
            return;
        }
        if (mc.player == null || mc.gameMode == null || mc.level == null) return;
        if (mc.screen instanceof DeathScreen) {
            mc.player.respawn();
            mc.setScreen(null);
            return;
        }
        BlockPos pos = HuskMultiplayerServer.HUSK;
        if (click && mc.screen == null && mc.player.isWithinBlockInteractionRange(pos, 0)
                && mc.level.getBlockEntity(pos) != null && ++readyTicks >= 5) {
            click = false;
            var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0, -0.5), Direction.NORTH, pos, false);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        }
    }
}
