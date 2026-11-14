package net.j40climb.florafauna.common.item.abilities;

import net.j40climb.florafauna.common.item.abilities.data.MultiToolAbilityData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;

final class ToolModification {
    static boolean apply(Player player, InteractionHand hand, BlockPos pos, BlockHitResult originalHit, MultiToolAbilityData multiTool) {
        Level level = player.level();
        BlockState state = level.getBlockState(pos);

        // Create synthetic hit result for this block position
        BlockHitResult syntheticHit = new BlockHitResult(
                Vec3.atCenterOf(pos), originalHit.getDirection(), pos, originalHit.isInside());
        UseOnContext context = new UseOnContext(player, hand, syntheticHit);

        // Try each enabled tool action in order of priority
        if (multiTool.strip()) {
            BlockState modified = state.getToolModifiedState(context, ItemAbilities.AXE_STRIP, false);
            if (modified != null && modified != state) {
                level.setBlock(pos, modified, 11);
                level.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0f, 1.0f);
                return true;
            }
        }

        if (multiTool.flatten()) {
            BlockState modified = state.getToolModifiedState(context, ItemAbilities.SHOVEL_FLATTEN, false);
            if (modified != null && modified != state) {
                level.setBlock(pos, modified, 11);
                level.playSound(null, pos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0f, 1.0f);
                return true;
            }
        }

        if (multiTool.till()) {
            BlockState modified = state.getToolModifiedState(context, ItemAbilities.HOE_TILL, false);
            if (modified != null && modified != state) {
                level.setBlock(pos, modified, 11);
                level.playSound(null, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0f, 1.0f);
                return true;
            }
        }

        return false;
    }
}
