package net.j40climb.florafauna.mixin;
import net.j40climb.florafauna.common.block.mobbarrier.MobBarrierBlock;
import net.minecraft.world.level.pathfinder.*;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(WalkNodeEvaluator.class)
abstract class FabricBarrierPathMixin extends NodeEvaluator {
 @Inject(method="getPathType(Lnet/minecraft/world/level/pathfinder/PathfindingContext;III)Lnet/minecraft/world/level/pathfinder/PathType;",at=@At("HEAD"),cancellable=true)
 private void florafauna$barrierPath(PathfindingContext context,int x,int y,int z,CallbackInfoReturnable<PathType> cir){
  BlockPos pos=new BlockPos(x,y,z);var state=context.getBlockState(pos);
  if(state.getBlock() instanceof MobBarrierBlock barrier){PathType type=barrier.getBlockPathType(state,context.level(),pos,mob);if(type!=null)cir.setReturnValue(type);}
 }
}
