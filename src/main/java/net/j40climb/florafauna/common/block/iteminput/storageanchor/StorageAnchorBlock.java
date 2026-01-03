package net.j40climb.florafauna.common.block.iteminput.storageanchor;

import com.mojang.serialization.MapCodec;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Storage Anchor block acts as the central hub for the item input system.
 * It auto-detects nearby containers and provides them as destinations for item inputs.
 *
 * Features:
 * - Auto-detects vanilla containers (chests, barrels, hoppers) within radius
 * - Supports explicit linking to distant containers
 * - Pairs with multiple ItemInput blocks
 * - Periodically refreshes container list
 */
public class StorageAnchorBlock extends BaseEntityBlock {

    public static final MapCodec<StorageAnchorBlock> CODEC = simpleCodec(StorageAnchorBlock::new);

    public StorageAnchorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StorageAnchorBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }

        return createTickerHelper(blockEntityType, FloraFaunaRegistry.STORAGE_ANCHOR_BE.get(),
                (level1, blockPos, blockState, blockEntity) -> blockEntity.tick(level1, blockPos, blockState));
    }
}
