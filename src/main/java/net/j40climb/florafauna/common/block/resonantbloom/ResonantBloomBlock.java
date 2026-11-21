package net.j40climb.florafauna.common.block.resonantbloom;

import net.j40climb.florafauna.common.symbiote.onboarding.SymbioteOnboarding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** A wild flower whose released spores introduce a player's symbiote. */
public class ResonantBloomBlock extends BushBlock {
    public ResonantBloomBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    //? if <26.3 {
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            releaseSpores(serverLevel, serverPlayer, pos);
        }
    }
    //?} else {
    /*@Override
    public void playerDestroy(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state,
                              BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        releaseSpores(level, player, pos);
    }
    *///?}

    private static void releaseSpores(ServerLevel level, ServerPlayer player, BlockPos pos) {
        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                pos.getX() + 0.5, pos.getY() + 0.55, pos.getZ() + 0.5,
                24, 0.35, 0.3, 0.35, 0.01);
        SymbioteOnboarding.onBloomBroken(player);
    }
}
