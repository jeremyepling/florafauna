package net.j40climb.florafauna.platform;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
public final class GameplayAccess {
 public static boolean canEntityGrief(ServerLevel level, Entity entity) {
  //? if neoforge {
  return net.neoforged.neoforge.event.EventHooks.canEntityGrief(level,entity);
  //?} else {
  /*return level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING);
  *///?}
 }
}
