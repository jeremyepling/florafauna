package net.j40climb.florafauna.common.block.iteminput.fieldrelay;

import com.mojang.serialization.MapCodec;
import net.j40climb.florafauna.common.block.vacuum.AbstractVacuumBlockEntity;
import net.j40climb.florafauna.common.block.vacuum.VacuumState;
import net.j40climb.florafauna.common.block.iteminput.storageanchor.StorageAnchorBlock;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The Field Relay collects dropped items from the world and transfers them
 * to paired storage via a Storage Anchor.
 *
 * This is a variant of the Item Input system designed for large-scale collection.
 * It has a larger collection radius and is intended for farms and mob grinders.
 *
 * Visual appearance changes based on state:
 * - NORMAL: Idle, waiting for items
 * - WORKING: Actively collecting/transferring items
 * - BLOCKED: Buffer full or no storage available
 */
public class FieldRelayBlock extends BaseEntityBlock {

    public static final MapCodec<FieldRelayBlock> CODEC = simpleCodec(FieldRelayBlock::new);

    public FieldRelayBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AbstractVacuumBlockEntity.STATE, VacuumState.NORMAL));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AbstractVacuumBlockEntity.STATE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FieldRelayBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    private static final int PAIRING_SEARCH_RADIUS = 16;

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof FieldRelayBlockEntity itemInput)) {
            return InteractionResult.PASS;
        }

        if (stack.isEmpty()) {
            // Sneak + right-click: unpair
            if (player.isShiftKeyDown() && itemInput.isPaired()) {
                itemInput.unpairAnchor();
                player.displayClientMessage(
                        Component.translatable("message.florafauna.field_relay.unpaired"),
                        true
                );
                return InteractionResult.SUCCESS;
            }

            // Not paired: try to find and pair with nearest Storage Anchor
            if (!itemInput.isPaired()) {
                BlockPos anchorPos = findNearestStorageAnchor(level, pos);
                if (anchorPos != null) {
                    itemInput.pairWithAnchor(anchorPos);
                    player.displayClientMessage(
                            Component.translatable("message.florafauna.field_relay.paired",
                                    anchorPos.getX(), anchorPos.getY(), anchorPos.getZ()),
                            true
                    );
                } else {
                    player.displayClientMessage(
                            Component.translatable("message.florafauna.field_relay.no_anchor"),
                            true
                    );
                }
                return InteractionResult.SUCCESS;
            }

            // Already paired: show status
            showStatus(player, itemInput);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    /**
     * Finds the nearest Storage Anchor within the search radius.
     */
    @Nullable
    private BlockPos findNearestStorageAnchor(Level level, BlockPos from) {
        BlockPos nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (int x = -PAIRING_SEARCH_RADIUS; x <= PAIRING_SEARCH_RADIUS; x++) {
            for (int y = -PAIRING_SEARCH_RADIUS; y <= PAIRING_SEARCH_RADIUS; y++) {
                for (int z = -PAIRING_SEARCH_RADIUS; z <= PAIRING_SEARCH_RADIUS; z++) {
                    BlockPos checkPos = from.offset(x, y, z);
                    if (level.getBlockState(checkPos).getBlock() instanceof StorageAnchorBlock) {
                        double distSq = from.distSqr(checkPos);
                        if (distSq < nearestDistSq) {
                            nearestDistSq = distSq;
                            nearest = checkPos;
                        }
                    }
                }
            }
        }

        return nearest;
    }

    /**
     * Shows the current status of the item input to the player.
     */
    private void showStatus(Player player, FieldRelayBlockEntity itemInput) {
        boolean paired = itemInput.isPaired();
        int buffered = itemInput.getBuffer().getUsedSlots();
        int maxSlots = itemInput.getBuffer().getMaxStacks();
        int totalItems = itemInput.getBuffer().getTotalItemCount();

        if (paired) {
            player.displayClientMessage(
                    Component.translatable("message.florafauna.field_relay.status_paired",
                            buffered, maxSlots, totalItems),
                    true
            );
        } else {
            player.displayClientMessage(
                    Component.translatable("message.florafauna.field_relay.status_unpaired"),
                    true
            );
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }

        return createTickerHelper(blockEntityType, FloraFaunaRegistry.FIELD_RELAY_BE.get(),
                (level1, blockPos, blockState, blockEntity) -> blockEntity.tick(level1, blockPos, blockState));
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FieldRelayBlockEntity itemInput) {
                itemInput.onRemoved();
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
