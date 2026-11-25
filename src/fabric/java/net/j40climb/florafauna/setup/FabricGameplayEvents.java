package net.j40climb.florafauna.setup;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.j40climb.florafauna.common.block.cocoonchamber.CocoonRespawnHandler;
import net.j40climb.florafauna.common.block.husk.HuskBlockEntity;
import net.j40climb.florafauna.common.block.mobtransport.MobInputLinkingState;
import net.j40climb.florafauna.common.block.slimegrate.SlimeGrateFarm;
import net.j40climb.florafauna.common.mobsymbiote.MobSymbioteHelper;
import net.j40climb.florafauna.common.mobsymbiote.fear.FearStateEvents;
import net.j40climb.florafauna.common.mobsymbiote.irongarden.IronGardenStateEvents;
import net.j40climb.florafauna.common.mobsymbiote.silk.SpiderSilkEvents;
import net.j40climb.florafauna.common.symbiote.abilities.SymbioteAbilityEvents;
import net.j40climb.florafauna.common.symbiote.observation.ObservationEvents;
import net.j40climb.florafauna.common.symbiote.onboarding.SymbioteOnboarding;
import net.j40climb.florafauna.dev.DevWorldSetup;
import net.j40climb.florafauna.platform.StorageAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
public final class FabricGameplayEvents {
 public static void register() {
  ServerEntityEvents.ENTITY_LOAD.register(MobSymbioteHelper::onEntityJoin);
  ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->DevWorldSetup.onPlayerLoggedIn(handler.player));
  ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{
   CocoonRespawnHandler.onPlayerLogout(handler.player);MobInputLinkingState.onLogout(handler.player);
  });
  ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer,newPlayer,alive)->CocoonRespawnHandler.onPlayerRespawn(newPlayer));
  PlayerBlockBreakEvents.BEFORE.register((level,player,pos,state,be)->!(be instanceof HuskBlockEntity husk)||husk.isOwner(player));
  ServerLivingEntityEvents.AFTER_DAMAGE.register((entity,source,baseDamage,damageTaken,blocked)->{
   if(damageTaken>0)IronGardenStateEvents.onGolemDamaged(entity);
  });
 }
 public static void afterTick(Entity entity) {
  FearStateEvents.onEntityTick(entity);IronGardenStateEvents.onEntityTick(entity);
  SpiderSilkEvents.onSpiderTick(entity);SlimeGrateFarm.afterMove(entity);
  if(entity instanceof Player player){SymbioteAbilityEvents.onPlayerTick(player);ObservationEvents.onPlayerTick(player);SymbioteOnboarding.onPlayerTick(player);}
 }
}
