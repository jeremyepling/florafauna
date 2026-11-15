package net.j40climb.florafauna.common.item.abilities;

import net.j40climb.florafauna.common.item.abilities.data.MultiToolAbilityData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class ToolModification {
    static boolean apply(Player player, InteractionHand hand, BlockPos pos, BlockHitResult originalHit, MultiToolAbilityData multiTool) {
        BlockState state = player.level().getBlockState(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), originalHit.getDirection(), pos, originalHit.isInside());
        UseOnContext context = new UseOnContext(player, hand, hit);

        // The vanilla axe transformer also scrapes copper. This tool's strip setting is for logs and bamboo.
        if (multiTool.strip() && (state.is(BlockTags.LOGS) || state.is(BlockTags.BAMBOO_BLOCKS))
                && transformBlock(context, BlockTransformers.AXE)) {
            return true;
        }
        if (multiTool.flatten() && transformBlock(context, BlockTransformers.SHOVEL)) {
            return true;
        }
        return multiTool.till() && transformBlock(context, BlockTransformers.HOE);
    }

    private static boolean transformBlock(UseOnContext context, ResourceKey<BlockTransformer> key) {
        Holder<BlockTransformer> transformer = context.getLevel().registryAccess().getOrThrow(key);
        return transformer.value().transformBlock(context, transformer).consumesAction();
    }
}
