package net.j40climb.florafauna.common.block.mobbarrier;

import com.mojang.serialization.MapCodec;
import net.j40climb.florafauna.common.block.mobbarrier.data.MobBarrierConfig;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * An invisible barrier block that blocks configurable mobs while allowing
 * all other entities (including players) to pass through freely.
 *
 * Features:
 * - Completely invisible when placed
 * - Solid collision ONLY for configured entities (by ID or tag)
 * - All other entities pass through freely
 * - Allows interaction through the block
 * - Blocks pathfinding for configured entities
 * - Unbreakable in survival mode
 * - Shift-right-click to copy config from placed block to held item
 */
public class MobBarrierBlock extends BaseEntityBlock {

    public static final MapCodec<MobBarrierBlock> CODEC = simpleCodec(MobBarrierBlock::new);

    // 24 pixels = 1.5 blocks tall, same as fences - prevents jumping over
    private static final VoxelShape BARRIER_SHAPE = Block.box(0, 0, 0, 16, 24, 16);

    public MobBarrierBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MobBarrierBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /**
     * Gets the MobBarrierConfig from the block entity at the given position.
     */
    private MobBarrierConfig getConfigAtPos(BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MobBarrierBlockEntity blockEntity) {
            return blockEntity.getConfig();
        }
        return MobBarrierConfig.DEFAULT;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext) {
            Entity entity = entityContext.getEntity();
            MobBarrierConfig config = getConfigAtPos(level, pos);

            if (config.shouldBlockEntity(entity)) {
                return BARRIER_SHAPE;
            }
        }
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return InteractionResult.PASS;
    }

    /**
     * Handle shift-right-click to copy config from placed block to held stack.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player.isShiftKeyDown() && stack.is(FloraFaunaRegistry.MOB_BARRIER.asItem())) {
            if (!level.isClientSide()) {
                MobBarrierConfig placedConfig = getConfigAtPos(level, pos);
                stack.set(FloraFaunaRegistry.MOB_BARRIER_CONFIG.get(), placedConfig);
                player.displayClientMessage(
                        Component.translatable("message.florafauna.mob_barrier.copied"),
                        true
                );
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public PathType getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob) {
        if (mob != null) {
            MobBarrierConfig config = getConfigAtPos(level, pos);
            if (config.shouldBlockEntity(mob)) {
                return PathType.BLOCKED;
            }
        }
        return null;
    }
}
