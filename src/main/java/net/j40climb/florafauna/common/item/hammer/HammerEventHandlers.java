package net.j40climb.florafauna.common.item.hammer;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.RegisterDataComponentTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public class HammerEventHandlers {
    @SubscribeEvent
    public static void extractBlockOutlineRenderStateEvent(ExtractBlockOutlineRenderStateEvent event) {
        event.addCustomRenderer(new MiningModeBlockOutlineRenderer());
    }

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
            boolean cancelEvent = MiningModeBlockInteractions.breakWithMiningMode(mainHandItem, initialBlockPos, serverPlayer, level);
            if (cancelEvent) {
                // This is needed to not delete the stairs that were placed
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void LeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        ItemStack itemStack = event.getItemStack();

        if (itemStack.get(RegisterDataComponentTypes.MINING_MODE_DATA) != null) {
            MiningModeBlockInteractions.doExtraCrumblings(event);
        }
    }
}
