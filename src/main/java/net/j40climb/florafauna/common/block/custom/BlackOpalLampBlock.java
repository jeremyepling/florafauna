package net.j40climb.florafauna.common.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class BlackOpalLampBlock extends Block {
    public static final BooleanProperty CLICKED = BooleanProperty.create("clicked");

    public BlackOpalLampBlock(Properties properties) {
        super(properties);
        // The block is off when it is placed
        this.registerDefaultState(this.defaultBlockState().setValue(CLICKED, false));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if(!level.isClientSide()) {
            boolean currentState = state.getValue(CLICKED);
            level.setBlock(pos, state.setValue(CLICKED, !currentState), 3);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CLICKED);
    }
}
