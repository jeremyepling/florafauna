package net.j40climb.florafauna.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.j40climb.florafauna.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna.common.item.abilities.multiblock.MultiBlockBreaker;
import net.j40climb.florafauna.common.item.abilities.multiblock.MultiBlockPatterns;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerGameMode.class)
abstract class HammerMiningMixin {
    @Shadow @Final protected ServerPlayer player;
    @Unique private @Nullable BlockPos florafauna$target;
    @Unique private Direction florafauna$face = Direction.UP;

    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"))
    private void florafauna$rememberHit(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction face,
                                        int maxY, int sequence, CallbackInfo ci) {
        if (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
            florafauna$target = pos.immutable();
            florafauna$face = face;
        } else if (action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK) {
            florafauna$target = null;
        }
    }

    @WrapMethod(method = "destroyBlock")
    private boolean florafauna$minePattern(BlockPos pos, Operation<Boolean> original) {
        if (!pos.equals(florafauna$target)) return original.call(pos);
        // Consume this player's hit once. Recursive neighbor breaks have no pending hit.
        florafauna$target = null;
        var tool = player.getMainHandItem();
        var mode = tool.get(FloraFaunaRegistry.MULTI_BLOCK_MINING);
        if (mode == null || mode.shape() == MiningShape.SINGLE) return original.call(pos);
        var level = player.level();
        var before = level.getBlockState(pos);
        var plan = MultiBlockPatterns.plan(player, tool, pos, florafauna$face);
        boolean accepted = original.call(pos);
        // In 26.1 destroyBlock can return true even if onDestroyedByPlayer kept the block.
        if (accepted && !level.getBlockState(pos).is(before.getBlock()) && !plan.blocks().isEmpty()) {
            MultiBlockBreaker.finish(player, tool, mode, pos, plan);
        }
        return accepted;
    }
}