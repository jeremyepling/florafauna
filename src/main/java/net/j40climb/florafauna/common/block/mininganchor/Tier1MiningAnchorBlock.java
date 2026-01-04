package net.j40climb.florafauna.common.block.mininganchor;

import com.mojang.serialization.MapCodec;
import net.j40climb.florafauna.common.block.vacuum.AbstractVacuumBlockEntity;
import net.j40climb.florafauna.common.block.vacuum.VacuumState;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
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
 * T0 (Feral) Mining Anchor block.
 * A mobile storage system for mining expeditions that vacuums block drops.
 *
 * Interactions:
 * - Right-click: Register as active waypoint
 * - Shift-right-click: Bind/unbind to player's symbiote
 *
 * Automatically spawns Feral Pods when storage fills up.
 */
public class Tier1MiningAnchorBlock extends BaseEntityBlock {

    public static final MapCodec<Tier1MiningAnchorBlock> CODEC = simpleCodec(Tier1MiningAnchorBlock::new);

    public Tier1MiningAnchorBlock(Properties properties) {
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
        return new Tier1MiningAnchorBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof Tier1MiningAnchorBlockEntity anchor)) {
            return InteractionResult.PASS;
        }

        if (stack.isEmpty() && player instanceof ServerPlayer serverPlayer) {
            if (player.isShiftKeyDown()) {
                // Shift-right-click: bind/unbind symbiote
                MiningAnchorInteractions.handleBindToggle(serverPlayer, pos, level);
                return InteractionResult.SUCCESS;
            }

            // Right-click: set as waypoint and show status
            MiningAnchorInteractions.handleSetWaypoint(serverPlayer, pos, level);
            showStatus(player, anchor);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    /**
     * Shows the current status of the mining anchor to the player.
     */
    private void showStatus(Player player, AbstractMiningAnchorBlockEntity anchor) {
        int stored = anchor.getStoredCount();
        int max = anchor.getMaxCapacity();
        int pods = anchor.getPodCount();
        AnchorFillState fillState = anchor.getFillState();

        player.displayClientMessage(
                Component.translatable("message.florafauna.mining_anchor.status",
                        stored, max, pods, fillState.getSerializedName()),
                true
        );
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }

        return createTickerHelper(blockEntityType, FloraFaunaRegistry.TIER1_MINING_ANCHOR_BE.get(),
                (level1, blockPos, blockState, blockEntity) -> blockEntity.tick(level1, blockPos, blockState));
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AbstractMiningAnchorBlockEntity anchor) {
                anchor.onRemoved();
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
