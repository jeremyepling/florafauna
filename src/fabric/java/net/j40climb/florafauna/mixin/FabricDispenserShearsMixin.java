package net.j40climb.florafauna.mixin;

import net.j40climb.florafauna.common.mobsymbiote.silk.SilkLineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
//? if >=26.4 {
/*import net.minecraft.core.dispenser.DispenseBehavior.DispenseResult;
import net.minecraft.core.dispenser.ShearDispenseBehavior;
*///?} else {
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.core.dispenser.ShearsDispenseItemBehavior;
//?}
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if >=26.4 {
/*@Mixin(ShearDispenseBehavior.class)
*///?} else {
@Mixin(ShearsDispenseItemBehavior.class)
//?}
abstract class FabricDispenserShearsMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    //? if >=26.4 {
    /*private void florafauna$shearSilk(BlockSource source, ItemStack shears, CallbackInfoReturnable<DispenseResult> result) {
    *///?} else {
    private void florafauna$shearSilk(BlockSource source, ItemStack shears, CallbackInfoReturnable<ItemStack> result) {
    //?}
        var level = source.level();
        BlockPos pos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
        var state = level.getBlockState(pos);
        if (SilkLineBlock.canCut(level, pos, state)) {
            level.setBlockAndUpdate(pos, state.setValue(SilkLineBlock.CUT, true));
            level.scheduleTick(pos, state.getBlock(), 1);
            level.gameEvent(null, GameEvent.SHEAR, pos);
            shears.hurtAndBreak(1, level, null, item -> {});
            //? if >=26.4 {
            /*result.setReturnValue(DispenseResult.success(shears));
            *///?} else {
            ((OptionalDispenseItemBehavior) (Object) this).setSuccess(true);
            result.setReturnValue(shears);
            //?}
        }
    }
}
