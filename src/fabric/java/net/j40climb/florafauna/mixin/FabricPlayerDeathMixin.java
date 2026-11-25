package net.j40climb.florafauna.mixin;
import net.j40climb.florafauna.common.block.husk.HuskDeathHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerPlayer.class)
abstract class FabricPlayerDeathMixin {
 // This point is after Fabric's death veto and before vanilla consumes the inventory.
 @Inject(method="die",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V"))
 private void florafauna$storeInventory(DamageSource source,CallbackInfo ci){HuskDeathHandler.onPlayerDeath((ServerPlayer)(Object)this);}
}
