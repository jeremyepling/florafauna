package net.j40climb.florafauna.common.mobsymbiote.silk;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.ItemAbilities;

public class SilkLineBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = EnumProperty.create("axis", Direction.Axis.class, Direction.Axis.X, Direction.Axis.Z);
    public static final BooleanProperty CUT = BooleanProperty.create("cut");
    public static final BooleanProperty FIRST = BooleanProperty.create("first");
    public static final BooleanProperty LAST = BooleanProperty.create("last");
    private static final VoxelShape X_SHAPE = box(0, 9, 5, 16, 15, 11);
    private static final VoxelShape Z_SHAPE = box(5, 9, 0, 11, 15, 16);
    //? if <26.3 {
    private static final MapCodec<SilkLineBlock> CODEC = simpleCodec(SilkLineBlock::new);
    //?}

    public SilkLineBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X).setValue(CUT, false)
                .setValue(FIRST, false).setValue(LAST, false));
    }
    //? if <26.3 {

    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    //?}

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, CUT, FIRST, LAST);
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? X_SHAPE : Z_SHAPE;
    }

    static boolean matches(LevelReader level, BlockPos pos, Direction along) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof SilkLineBlock && state.getValue(AXIS) == along.getAxis();
    }

    private static Direction along(BlockState state) {
        return state.getValue(AXIS) == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
    }

    static boolean canCut(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof SilkLineBlock)) return false;
        SilkSpan span = SilkSpan.existing(level, pos, along(state));
        if (span == null) return false;
        for (int i = 0; i < span.length(); i++) {
            if (level.getBlockState(span.piece(i)).getValue(CUT)) return false;
        }
        return true;
    }

    @Override protected InteractionResult useItemOn(ItemStack tool, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!tool.canPerformAction(ItemAbilities.SHEARS_HARVEST) || !player.mayBuild() || !canCut(level, pos, state)) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            server.setBlockAndUpdate(pos, state.setValue(CUT, true));
            server.scheduleTick(pos, this, 1);
            server.gameEvent(player, GameEvent.SHEAR, pos);
            tool.hurtAndBreak(1, player, hand);
        }
        return InteractionResult.SUCCESS;
    }

    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction dir, BlockPos neighbor, BlockState neighborState, RandomSource random) {
        if (dir.getAxis() == state.getValue(AXIS)) ticks.scheduleTick(pos, this, 1);
        return state;
    }

    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Direction direction = along(state);
        // Don't discard a strand whose other anchor is temporarily unloaded.
        if (!level.hasChunkAt(pos.relative(direction, SilkSpan.MAX_GAP)) ||
                !level.hasChunkAt(pos.relative(direction.getOpposite(), SilkSpan.MAX_GAP))) {
            level.scheduleTick(pos, this, 20);
            return;
        }
        SilkSpan span = SilkSpan.existing(level, pos, direction);
        if (span == null) {
            level.removeBlock(pos, false);
        } else if (state.getValue(CUT)) {
            for (int i = 0; i < span.length(); i++) level.removeBlock(span.piece(i), false);
            popResource(level, pos, new ItemStack(Items.STRING));
            level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.8F, 1.25F);
        }
    }
}
