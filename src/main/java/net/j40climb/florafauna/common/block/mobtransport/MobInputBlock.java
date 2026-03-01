package net.j40climb.florafauna.common.block.mobtransport;

import com.mojang.serialization.MapCodec;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The MobInput "chompy plant" block that lures and captures eligible mobs.
 * <p>
 * Visual appearance changes based on state:
 * - IDLE: Default, waiting for mobs
 * - OPEN: Mouth open, actively luring mobs
 * - CHOMPING: Capture animation in progress
 * <p>
 * Interactions:
 * - Shift+right-click: Enter linking mode or unpair
 * - Right-click: Show status
 */
public class MobInputBlock extends BaseEntityBlock {

    public static final MapCodec<MobInputBlock> CODEC = simpleCodec(MobInputBlock::new);
    public static final EnumProperty<MobInputState> STATE = EnumProperty.create("state", MobInputState.class);

    public MobInputBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STATE, MobInputState.IDLE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STATE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MobInputBlockEntity(pos, state);
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
        if (!(be instanceof MobInputBlockEntity mobInput)) {
            return InteractionResult.PASS;
        }

        if (stack.isEmpty()) {
            // Shift+right-click: enter linking mode or unpair
            if (player.isShiftKeyDown()) {
                if (mobInput.isPaired()) {
                    mobInput.unpairOutput();
                    player.displayClientMessage(
                            Component.translatable("message.florafauna.mob_input.unpaired"),
                            true
                    );
                } else {
                    // Enter linking mode
                    MobInputLinkingState.setLinkingFrom(player, pos);
                    player.displayClientMessage(
                            Component.translatable("message.florafauna.mob_input.linking_mode"),
                            true
                    );
                }
                return InteractionResult.SUCCESS;
            }

            // Regular right-click: show status
            showStatus(player, mobInput);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private void showStatus(Player player, MobInputBlockEntity mobInput) {
        int queued = mobInput.getBuffer().size();
        int maxQueue = mobInput.getBuffer().getMaxSize();
        boolean paired = mobInput.isPaired();

        if (paired) {
            player.displayClientMessage(
                    Component.translatable("message.florafauna.mob_input.status_paired", queued, maxQueue),
                    true
            );
        } else {
            player.displayClientMessage(
                    Component.translatable("message.florafauna.mob_input.status_unpaired"),
                    true
            );
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, FloraFaunaRegistry.MOB_INPUT_BE.get(),
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MobInputBlockEntity mobInput) {
                mobInput.onRemoved();
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
