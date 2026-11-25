package net.j40climb.florafauna.common.block.mobtransport;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.network.chat.Component;
/** Runs only after a player break is accepted, before the block relinquishes queued mobs. */
public final class MobTransportRemoval {
 public static boolean beforePlayerBreak(Level level,BlockPos pos,Player player) {
  if(!(level instanceof ServerLevel server))return true;
  var entity=level.getBlockEntity(pos);
  boolean released=!(entity instanceof MobInputBlockEntity input)||input.releaseBeforeRemoval(server);
  if(entity instanceof MobOutputBlockEntity output)released=output.releaseBeforeRemoval(server);
  if(!released){var state=level.getBlockState(pos);level.sendBlockUpdated(pos,state,state,Block.UPDATE_CLIENTS);player.sendOverlayMessage(Component.translatable("message.florafauna.mob_transport.release_blocked"));}
  return released;
 }
}
