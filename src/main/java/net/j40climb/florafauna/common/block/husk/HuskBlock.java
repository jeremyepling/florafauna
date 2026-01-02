package net.j40climb.florafauna.common.block.husk;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The Symbiotic Husk block - stores player inventory on death.
 * Created when a bonded player dies to preserve items and symbiote state.
 *
 * Key behaviors:
 * - Only the owner can interact with and break the block
 * - Mobs cannot damage or interact with it
 * - Never used as a spawn point
 * - Light level 7 for RESTORATION type (beacon effect)
 */
public class HuskBlock extends BaseEntityBlock {

    public static final EnumProperty<HuskType> HUSK_TYPE = EnumProperty.create("husk_type", HuskType.class);
    public static final MapCodec<HuskBlock> CODEC = simpleCodec(HuskBlock::new);

    public HuskBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HUSK_TYPE, HuskType.RESTORATION));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HUSK_TYPE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HuskBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Explicitly prevent respawning at this block.
     * Per spec: Husks are never used as spawn points.
     */
    @Override
    public boolean isPossibleToRespawnInThis(BlockState state) {
        return false;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof HuskBlockEntity huskEntity)) {
            return InteractionResult.PASS;
        }

        // Delegate to the interaction handler
        return HuskInteractionHandler.handleInteraction(serverPlayer, huskEntity, state, level, pos);
    }

    /**
     * Prevent non-owners from breaking the block.
     * Returns 0 (unbreakable) for non-owners.
     */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof HuskBlockEntity huskEntity) {
            if (!huskEntity.isOwner(player)) {
                return 0.0f; // Unbreakable for non-owners
            }
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    /**
     * Called when the block is removed (broken).
     * Drops items for the owner, prevents drops for non-owners.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof HuskBlockEntity huskEntity) {
                // Only owner can break and get drops
                if (huskEntity.isOwner(player)) {
                    huskEntity.drops();
                }
                // Non-owners can't break due to getDestroyProgress returning 0
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
