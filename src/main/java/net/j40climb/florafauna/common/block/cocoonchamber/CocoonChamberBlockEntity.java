package net.j40climb.florafauna.common.block.cocoonchamber;

import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the Cocoon Chamber.
 * This is a minimal block entity - no inventory, no ticking.
 * The chamber opens a button-only screen via network packet (not menu system).
 */
public class CocoonChamberBlockEntity extends BlockEntity {

    public CocoonChamberBlockEntity(BlockPos pos, BlockState blockState) {
        super(FloraFaunaRegistry.COCOON_CHAMBER_BE.get(), pos, blockState);
    }
}
