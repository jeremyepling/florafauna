package net.j40climb.florafauna.common.block.mobbarrier;

import net.j40climb.florafauna.common.block.mobbarrier.data.MobBarrierConfig;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
//? if >=26.3 {
/*import net.minecraft.world.item.context.BlockPlaceContext;*/
//?}
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Custom BlockItem for MobBarrierBlock that carries the barrier configuration
 * as a data component and copies it to the BlockEntity when placed.
 */
public class MobBarrierBlockItem extends BlockItem {

    public MobBarrierBlockItem(Block block, Properties properties) {
        super(block, properties
                .useBlockDescriptionPrefix()
                .component(FloraFaunaRegistry.MOB_BARRIER_CONFIG.get(), MobBarrierConfig.DEFAULT));
    }

    //? if <26.3 {
    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player, ItemStack stack, BlockState state) {
        boolean result = super.updateCustomBlockEntityTag(pos, level, player, stack, state);
        copyConfig(level, pos, stack);
        return result;
    }
    //?} else {
    /*@Override
    public void onBlockPlacedBy(BlockPlaceContext context, Level level, BlockPos pos, BlockState state, @Nullable Player placer, ItemStack stack) {
        copyConfig(level, pos, stack);
    }*/
    //?}

    private static void copyConfig(Level level, BlockPos pos, ItemStack stack) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MobBarrierBlockEntity barrier) {
            barrier.setConfig(stack.getOrDefault(FloraFaunaRegistry.MOB_BARRIER_CONFIG.get(), MobBarrierConfig.DEFAULT));
        }
    }
}
