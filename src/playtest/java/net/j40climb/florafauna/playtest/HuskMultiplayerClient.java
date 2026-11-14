package net.j40climb.florafauna.playtest;

import net.j40climb.florafauna.FloraFauna;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** The two playtest clients use the same block interaction and respawn packets as regular players. */
@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
public final class HuskMultiplayerClient {
    private static final boolean ENABLED = System.getProperty("florafauna.huskTest.client") != null;
    private static boolean click, stop;
    private static int readyTicks;

    @SubscribeEvent
    public static void onMessage(ClientChatReceivedEvent.System event) {
        if (!ENABLED) return;
        switch (event.getMessage().getString()) {
            case HuskMultiplayerServer.CLICK -> {
                click = true;
                readyTicks = 0;
            }
            case HuskMultiplayerServer.DONE -> stop = true;
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        if (stop) {
            mc.stop();
            return;
        }
        if (mc.player == null || mc.gameMode == null || mc.level == null) return;
        if (mc.gui.screen() instanceof DeathScreen) {
            mc.player.respawn();
            mc.gui.setScreen(null);
            return;
        }
        BlockPos pos = HuskMultiplayerServer.HUSK;
        if (click && mc.gui.screen() == null && mc.player.isWithinBlockInteractionRange(pos, 0)
                && mc.level.getBlockEntity(pos) != null && ++readyTicks >= 5) {
            click = false;
            var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0, -0.5), Direction.NORTH, pos, false);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        }
    }
}
