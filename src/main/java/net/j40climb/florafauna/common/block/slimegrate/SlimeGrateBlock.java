package net.j40climb.florafauna.common.block.slimegrate;

import com.mojang.serialization.MapCodec;
import net.j40climb.florafauna.common.mobsymbiote.MobSymbioteHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
//? if <26.3 {
import net.minecraft.world.entity.EntityType;
//?} else {
/*import net.minecraft.world.entity.EntityTypes;
*///?}
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SlimeGrateBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    //? if <26.3 {
    private static final MapCodec<SlimeGrateBlock> CODEC = simpleCodec(SlimeGrateBlock::new);
    //?}
    private static final VoxelShape X_PANEL = Block.box(7, 0, 0, 9, 16, 16);
    private static final VoxelShape Z_PANEL = Block.box(0, 0, 7, 16, 16, 9);

    public SlimeGrateBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Z));
    }

    //? if <26.3 {
    @Override
    protected MapCodec<SlimeGrateBlock> codec() {
        return CODEC;
    }
    //?}

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> state.cycle(AXIS);
            default -> state;
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? X_PANEL : Z_PANEL;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof Slime slime
                && slime.getType() == EntityType.SLIME && MobSymbioteHelper.hasMobSymbiote(slime)) {
            return Shapes.empty();
        }
        return getShape(state, level, pos, context);
    }
}
