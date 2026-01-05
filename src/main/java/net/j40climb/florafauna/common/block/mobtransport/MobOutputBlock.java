package net.j40climb.florafauna.common.block.mobtransport;

import com.mojang.serialization.MapCodec;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * MobOutput block where captured mobs emerge.
 * <p>
 * Implements BonemealableBlock to grow pre-paired MobInput blocks adjacent to it.
 * Similar to how MiningAnchor grows pods.
 * <p>
 * Interactions:
 * - Right-click (while linking): Complete pairing with MobInput
 * - Right-click: Show status
 */
public class MobOutputBlock extends BaseEntityBlock implements BonemealableBlock {

    public static final MapCodec<MobOutputBlock> CODEC = simpleCodec(MobOutputBlock::new);

    // Cardinal direction offsets for MobInput spawning (like MiningAnchor pods)
    private static final BlockPos[] INPUT_OFFSETS = {
            new BlockPos(1, 0, 0),   // East
            new BlockPos(-1, 0, 0),  // West
            new BlockPos(0, 0, 1),   // South
            new BlockPos(0, 0, -1)   // North
    };

    public MobOutputBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MobOutputBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // ==================== BONEMEAL GROWTH ====================

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        // Can grow if there's an empty adjacent space
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MobOutputBlockEntity output)) {
            return false;
        }

        // Check if we've reached max inputs
        if (output.getInputCount() >= INPUT_OFFSETS.length) {
            return false;
        }

        // Check if there's an available position
        for (BlockPos offset : INPUT_OFFSETS) {
            BlockPos checkPos = pos.offset(offset);
            if (level.getBlockState(checkPos).isAir()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true; // Always succeeds if valid
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MobOutputBlockEntity output)) {
            return;
        }

        // Find first available position
        for (BlockPos offset : INPUT_OFFSETS) {
            BlockPos inputPos = pos.offset(offset);
            if (level.getBlockState(inputPos).isAir()) {
                // Place MobInput
                level.setBlock(inputPos, FloraFaunaRegistry.MOB_INPUT.get().defaultBlockState(), Block.UPDATE_ALL);

                // Get the new MobInput and pair it
                BlockEntity inputBe = level.getBlockEntity(inputPos);
                if (inputBe instanceof MobInputBlockEntity mobInput) {
                    mobInput.pairWithOutput(pos);
                    output.pairInput(inputPos);
                }
                break;
            }
        }
    }

    // ==================== INTERACTION ====================

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // Check if player is in linking mode
        BlockPos linkingFrom = MobInputLinkingState.getLinkingFrom(player);
        if (linkingFrom != null) {
            // Complete the pairing
            BlockEntity inputBe = level.getBlockEntity(linkingFrom);
            BlockEntity outputBe = level.getBlockEntity(pos);

            if (inputBe instanceof MobInputBlockEntity mobInput && outputBe instanceof MobOutputBlockEntity mobOutput) {
                mobInput.pairWithOutput(pos);
                mobOutput.pairInput(linkingFrom);
                MobInputLinkingState.clearLinking(player);
                player.displayClientMessage(
                        Component.translatable("message.florafauna.mob_input.paired_to_output",
                                linkingFrom.getX(), linkingFrom.getY(), linkingFrom.getZ()),
                        true
                );
                return InteractionResult.SUCCESS;
            }
        }

        // Show status
        if (stack.isEmpty()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MobOutputBlockEntity output) {
                int inputs = output.getInputCount();
                int queued = output.getPendingReleaseCount();
                player.displayClientMessage(
                        Component.translatable("message.florafauna.mob_output.status", inputs, queued),
                        true
                );
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, FloraFaunaRegistry.MOB_OUTPUT_BE.get(),
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MobOutputBlockEntity output) {
                output.onRemoved();
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
