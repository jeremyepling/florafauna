package net.j40climb.florafauna.mixin;
import net.j40climb.florafauna.common.block.slimegrate.SlimeGrateFarm;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(NoteBlock.class)
abstract class FabricNoteBlockMixin {
 @Inject(method="triggerEvent",at=@At("RETURN"))
 private void florafauna$hearNote(BlockState state,Level level,BlockPos pos,int type,int data,CallbackInfoReturnable<Boolean> cir){if(cir.getReturnValueZ())SlimeGrateFarm.onNote(level,pos);}
}
