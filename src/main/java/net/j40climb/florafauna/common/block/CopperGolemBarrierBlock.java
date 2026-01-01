package net.j40climb.florafauna.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * An invisible barrier block that blocks copper golems while allowing
 * all other entities (including players) to pass through freely.
 *
 * Features:
 * - Completely invisible when placed
 * - Solid collision ONLY for copper golems
 * - All other entities pass through freely
 * - Allows interaction through the block
 * - Blocks copper golem pathfinding specifically
 * - Unbreakable in survival mode
 */
public class CopperGolemBarrierBlock extends Block {

    public static final MapCodec<CopperGolemBarrierBlock> CODEC = simpleCodec(CopperGolemBarrierBlock::new);

    public CopperGolemBarrierBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Make block completely invisible
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Check if the entity colliding is a copper golem
        if (context instanceof EntityCollisionContext entityContext) {
            Entity entity = entityContext.getEntity();
            if (entity instanceof CopperGolem) {
                // Solid collision for copper golems - they can't pass through
                return Shapes.block();
            }
        }

        // No collision for all other entities - they can walk through
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // No visual outline
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        // Allow clicking through this block to interact with blocks behind it
        return Shapes.empty();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        // Don't consume interactions - let them pass through
        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public PathType getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob) {
        // Block pathfinding specifically for copper golems using instanceof
        // This is more reliable than string matching on entity type names
        if (mob instanceof CopperGolem) {
            return PathType.BLOCKED;
        }

        // Allow all other entities to pathfind through (treats as open/air)
        return null; // null = use default behavior
    }
}
