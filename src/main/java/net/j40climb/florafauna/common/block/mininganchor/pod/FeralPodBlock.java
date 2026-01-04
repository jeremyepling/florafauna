package net.j40climb.florafauna.common.block.mininganchor.pod;

import com.mojang.serialization.MapCodec;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * T0 (Feral) Storage Pod block.
 * Spawned by Feral Mining Anchor when storage fills up.
 * Spills all items when broken - no item form.
 */
public class FeralPodBlock extends BaseEntityBlock {

    public static final MapCodec<FeralPodBlock> CODEC = simpleCodec(FeralPodBlock::new);

    public FeralPodBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FeralPodBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FeralPodBlockEntity pod) {
                // Spill items (unless creative mode)
                if (!player.isCreative()) {
                    pod.onBlockBroken(level, pos, player);
                }
                // Notify anchor
                pod.onRemoved();
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        // Pods don't tick - they're passive storage
        return null;
    }
}
