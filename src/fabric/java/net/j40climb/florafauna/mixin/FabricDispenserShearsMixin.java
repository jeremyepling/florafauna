package net.j40climb.florafauna.mixin;
import net.j40climb.florafauna.common.mobsymbiote.silk.SilkLineBlock;
import net.minecraft.core.dispenser.ShearsDispenseItemBehavior;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ShearsDispenseItemBehavior.class)
abstract class FabricDispenserShearsMixin {
 @Inject(method="execute",at=@At("HEAD"),cancellable=true)
 private void florafauna$shearSilk(BlockSource source,ItemStack shears,CallbackInfoReturnable<ItemStack> cir){
  var level=source.level();BlockPos pos=source.pos().relative(source.state().getValue(DispenserBlock.FACING));var state=level.getBlockState(pos);
  if(SilkLineBlock.canCut(level,pos,state)){
   level.setBlockAndUpdate(pos,state.setValue(SilkLineBlock.CUT,true));level.scheduleTick(pos,state.getBlock(),1);
   level.gameEvent(null,GameEvent.SHEAR,pos);shears.hurtAndBreak(1,level,null,item->{});
   ((net.minecraft.core.dispenser.OptionalDispenseItemBehavior)(Object)this).setSuccess(true);cir.setReturnValue(shears);
  }
 }
}
