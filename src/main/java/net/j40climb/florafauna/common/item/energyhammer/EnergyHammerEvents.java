package net.j40climb.florafauna.common.item.energyhammer;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.client.BlockBreakUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public class EnergyHammerEvents {

    @SubscribeEvent
    public static void onBlockBreakEvent(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        Level level = player.level();
        GameType type = player.getAbilities().instabuild ? GameType.CREATIVE : GameType.SURVIVAL;
        ItemStack mainHandItem = player.getMainHandItem();
        BlockPos initialBlockPos = event.getPos();

        // It's on the server and the action isn't restricted, like Spectator mode
        if (player instanceof ServerPlayer serverPlayer &&
                !serverPlayer.blockActionRestricted(level, initialBlockPos, type)) {
            BlockBreakUtils.breakWithMiningMode(mainHandItem, initialBlockPos, serverPlayer, level);
        }
    }
}
