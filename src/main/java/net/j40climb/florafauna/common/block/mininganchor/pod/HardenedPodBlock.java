package net.j40climb.florafauna.common.block.mininganchor.pod;

import com.mojang.serialization.MapCodec;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * T2 (Hardened) Storage Pod block.
 * Player-placeable pod that preserves contents when broken (like shulker boxes).
 * Can be spawned by Hardened Mining Anchor or placed manually.
 */
public class HardenedPodBlock extends BaseEntityBlock {

    public static final MapCodec<HardenedPodBlock> CODEC = simpleCodec(HardenedPodBlock::new);

    public HardenedPodBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HardenedPodBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        // Load contents from item when placed
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HardenedPodBlockEntity pod) {
                pod.loadFromItem(stack);
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HardenedPodBlockEntity pod) {
                // Drop as item with contents (unless creative mode)
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
