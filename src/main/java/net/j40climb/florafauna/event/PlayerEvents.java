package net.j40climb.florafauna.event;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.items.interfaces.DiggerTool;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Set;

import static net.j40climb.florafauna.common.items.interfaces.DiggerTool.getBlocksToBeBroken;

/*
This code came from https://github.com/Direwolf20-MC/JustDireThings/blob/32225177c42a25f32e69d46e342ea81dfed91a7f/src/main/java/com/direwolf20/justdirethings/client/events/PlayerEvents.java
and has been modified for my needs.
 */

@EventBusSubscriber(modid = FloraFauna.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class PlayerEvents {
    private static BlockPos destroyPos = BlockPos.ZERO;
    private static int gameTicksMining = 0;

    @SubscribeEvent
    public static void LeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        ItemStack itemStack = event.getItemStack();

        if (itemStack.getItem() instanceof DiggerTool diggerTool && event.getFace() != null) {
            PlayerEvents.doExtraCrumblings(event, itemStack, diggerTool);
        }
    }

    static void doExtraCrumblings(PlayerInteractEvent.LeftClickBlock event, ItemStack itemStack, DiggerTool diggerTool) {

        Player player = event.getEntity();
        Level level = player.level();
        BlockPos blockPos = event.getPos();
        BlockState blockState = level.getBlockState(blockPos);
        if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START) { //Client and Server
            if (level.isClientSide) {
                PlayerEvents.gameTicksMining = 0;
                PlayerEvents.destroyPos = blockPos;
            }
        }
        if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.STOP || event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.ABORT) { //Server Only
            cancelBreaks(level, blockState, blockPos, player, diggerTool, itemStack);
        }

        if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.CLIENT_HOLD) { //Client Only
            if (blockPos.equals(PlayerEvents.destroyPos)) {
                PlayerEvents.gameTicksMining++;
            } else {
                PlayerEvents.gameTicksMining = 0;
                destroyPos = blockPos;
            }
            incrementDestroyProgress(level, blockState, blockPos, player, diggerTool, itemStack);
        }
    }

    static void incrementDestroyProgress(Level level, BlockState blockState, BlockPos pPos, Player player, DiggerTool diggerTool, ItemStack diggerItemStack) {
        Set<BlockPos> breakBlockPositions = getBlocksToBeBroken(3, pPos, player);
        int i = PlayerEvents.gameTicksMining;
        float f = blockState.getDestroyProgress(player, player.level(), pPos) * (float) (i + 1);
        int j = (int) (f * 10.0F);
        for (BlockPos blockPos : breakBlockPositions) {
            if (blockPos.equals(pPos)) continue; //Let the vanilla mechanics handle the block we're hitting
            if (level.isClientSide)
                level.destroyBlockProgress(player.getId() + generatePosHash(blockPos), blockPos, j);
            else
                sendDestroyBlockProgress(player.getId() + generatePosHash(blockPos), blockPos, -1, (ServerPlayer) player);
        }
    }

    static void cancelBreaks(Level level, BlockState pState, BlockPos pPos, Player player, DiggerTool diggerItem, ItemStack diggerItemStack) {
        Set<BlockPos> breakBlockPositions = getBlocksToBeBroken(3, pPos, player);
        for (BlockPos blockPos : breakBlockPositions) {
            if (blockPos.equals(pPos)) continue; //Let the vanilla mechanics handle the block we're hitting
            player.level().destroyBlockProgress(player.getId() + generatePosHash(blockPos), blockPos, -1);
        }
    }

    static int generatePosHash(BlockPos blockPos) {
        return (31 * 31 * blockPos.getX()) + (31 * blockPos.getY()) + blockPos.getZ(); //For now this is probably good enough, will add more randomness if needed
    }

    static void sendDestroyBlockProgress(int pBreakerId, BlockPos pPos, int pProgress, ServerPlayer serverPlayer) {
        serverPlayer.connection.send(new ClientboundBlockDestructionPacket(pBreakerId, pPos, pProgress));
    }

}
