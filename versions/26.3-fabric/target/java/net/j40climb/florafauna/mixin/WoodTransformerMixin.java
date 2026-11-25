package net.j40climb.florafauna.mixin;

import java.util.List;
import net.j40climb.florafauna.common.block.wood.WoodBlockSet;
import net.j40climb.florafauna.common.block.wood.WoodType;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.CopyPropertiesProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedStateProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Extends the vanilla axe transformer, including callers such as Hammer. */
@Mixin(BlockTransformer.class)
abstract class WoodTransformerMixin {
    @Inject(method = "transformBlock", at = @At("HEAD"), cancellable = true)
    private void florafauna$stripWood(UseOnContext context, CallbackInfoReturnable<InteractionResult> result) {
        if ((Object) this != context.getLevel().registryAccess().getOrThrow(BlockTransformers.AXE).value()) return;
        Block source = context.getLevel().getBlockState(context.getClickedPos()).getBlock();
        for (WoodType type : WoodType.values()) {
            WoodBlockSet wood = type.getBlockSet();
            Block target;
            if (source == wood.log().get()) target = wood.strippedLog().get();
            else if (source == wood.wood().get()) target = wood.strippedWood().get();
            else continue;
            var provider = RuleBasedStateProvider.ifTrueThenProvide(
                    BlockPredicate.matchesBlocks(source), new CopyPropertiesProvider(target));
            var transform = BlockTransformer.BlockTransformData.builder(provider)
                    .sound(SoundEvents.AXE_STRIP).itemDamagePerUse(1).build();
            // Delegate feedback, item damage, advancements, and secondary-use blocking to vanilla.
            result.setReturnValue(new BlockTransformer(List.of(transform)).transformBlock(context));
            return;
        }
    }
}
