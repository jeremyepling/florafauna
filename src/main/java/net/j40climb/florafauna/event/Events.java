package net.j40climb.florafauna.event;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.item.custom.HammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(modid = FloraFauna.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class Events {
    private static final Set<BlockPos> HARVESTED_BLOCKS = new HashSet<>();

    @SubscribeEvent
    public static void onHammerUsage(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHandItem = player.getMainHandItem();

        if(mainHandItem.getItem() instanceof HammerItem hammer && player instanceof ServerPlayer serverPlayer) {
            BlockPos initialBlockPos = event.getPos();
            if(HARVESTED_BLOCKS.contains(initialBlockPos)) {
                return;
            }

            for(BlockPos pos : HammerItem.getBlocksToBeDestroyed( 1, initialBlockPos, serverPlayer)) {
                // Don't mine the position that was just mined, or a different type of block
                if(pos == initialBlockPos || !hammer.isCorrectToolForDrops(mainHandItem, event.getLevel().getBlockState(pos))) {
                    continue;
                }

                HARVESTED_BLOCKS.add(pos);
                // Call destroy which runs this event again so we need the fast return above in if(HARVESTED_BLOCKS.contains(initialBlockPos))
                // If not, it would keep deleting and not move through the set, creating an infinite loop
                // https://courses.kaupenjoe.net/courses/modding-by-kaupenjoe-neoforge-modding-for-minecraft-1-21-x/lectures/55341862 at 10min in
                serverPlayer.gameMode.destroyBlock(pos);
                HARVESTED_BLOCKS.remove(pos);
            }
        }
    }
}
