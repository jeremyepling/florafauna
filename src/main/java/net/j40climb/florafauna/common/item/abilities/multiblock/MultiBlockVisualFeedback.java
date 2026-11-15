package net.j40climb.florafauna.common.item.abilities.multiblock;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

/** The local player's additional block cracks, cleared using the positions actually drawn. */
@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
public final class MultiBlockVisualFeedback {
    private static BlockPos target = BlockPos.ZERO;
    private static Direction hitFace = Direction.UP;
    private static int ticksMining;
    private static List<BlockPos> cracks = List.of();
    private static boolean refreshCracks;

    private MultiBlockVisualFeedback() {}

    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()) return;
        switch (event.getAction()) {
            case STOP, ABORT -> clear(player);
            case START -> {
                clear(player);
                target = event.getPos().immutable();
                hitFace = event.getFace();
                ticksMining = 0;
            }
            case CLIENT_HOLD -> {
                if (!event.getPos().equals(target) || !event.getItemStack().has(FloraFaunaRegistry.MULTI_BLOCK_MINING)) {
                    clear(player);
                    return;
                }
                if (ticksMining == 0 || refreshCracks) {
                    refreshCracks = false;
                    cracks = MultiBlockPatterns.plan(player, event.getItemStack(), target, hitFace).blocks().stream()
                            .filter(pos -> !pos.equals(target)).toList();
                }
                ticksMining++;
                var state = player.level().getBlockState(target);
                float progress = state.getDestroyProgress(player, player.level(), target) * ticksMining;
                draw(player, Math.min(9, (int) (progress * 10)));
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (cracks.isEmpty()) return;
        var client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null) {
            cracks = List.of();
        } else if (!client.gameMode.isDestroying()) {
            clear(client.player);
        }
    }

    private static void clear(Player player) {
        draw(player, -1);
        cracks = List.of();
        ticksMining = 0;
    }

    public static void invalidatePattern(Player player) {
        draw(player, -1);
        cracks = List.of();
        refreshCracks = true;
    }

    private static void draw(Player player, int stage) {
        // Local overlays use negative IDs. Distinct vein positions can share BlockPos.hashCode().
        for (int i = 0; i < cracks.size(); i++) {
            player.level().destroyBlockProgress(-1 - i, cracks.get(i), stage);
        }
    }
}
