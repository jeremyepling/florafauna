package net.j40climb.florafauna.common.block.mininganchor.pod;

import com.mojang.serialization.MapCodec;
import net.j40climb.florafauna.common.block.mininganchor.AbstractMiningAnchorBlockEntity;
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
public class Tier2PodBlock extends BaseEntityBlock {

    public static final MapCodec<Tier2PodBlock> CODEC = simpleCodec(Tier2PodBlock::new);

    public Tier2PodBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tier2PodBlockEntity(pos, state);
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
            if (be instanceof Tier2PodBlockEntity pod) {
                pod.loadFromItem(stack);
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof Tier2PodBlockEntity pod) {
                // Check if anchor is tearing down - if so, anchor handles items
                boolean anchorHandling = isAnchorTearingDown(level, pod.getParentAnchorPos());

                // Drop as item ONLY if NOT being handled by anchor teardown
                if (!anchorHandling && !player.isCreative()) {
                    pod.onBlockBroken(level, pos, player);
                }
                // Notify anchor
                pod.onRemoved();
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * Checks if the parent anchor is currently tearing down.
     */
    private boolean isAnchorTearingDown(Level level, @Nullable BlockPos anchorPos) {
        if (anchorPos == null) return false;
        BlockEntity be = level.getBlockEntity(anchorPos);
        return be instanceof AbstractMiningAnchorBlockEntity anchor && anchor.isTearingDown();
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        // Pods don't tick - they're passive storage
        return null;
    }
}
