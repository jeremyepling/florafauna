package net.j40climb.florafauna.mixin;
import net.j40climb.florafauna.common.block.mobtransport.MobTransportRemoval;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerPlayerGameMode.class)
abstract class FabricTransportBreakMixin {
 @Shadow protected ServerPlayer player;
 @Inject(method="destroyBlock",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"),cancellable=true)
 private void florafauna$releaseBeforeRemoval(BlockPos pos,CallbackInfoReturnable<Boolean> cir){if(!MobTransportRemoval.beforePlayerBreak(player.level(),pos,player))cir.setReturnValue(false);}
}
