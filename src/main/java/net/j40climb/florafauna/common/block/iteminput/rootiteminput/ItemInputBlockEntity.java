package net.j40climb.florafauna.common.block.iteminput.rootiteminput;

import net.j40climb.florafauna.common.block.iteminput.shared.AbstractItemInputBlockEntity;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the Item Input.
 * Uses the default behavior from AbstractItemInputBlockEntity.
 *
 * The Item Input collects items in a spherical radius,
 * absorbs them into its buffer, and transfers to paired storage.
 */
public class ItemInputBlockEntity extends AbstractItemInputBlockEntity {

    public ItemInputBlockEntity(BlockPos pos, BlockState blockState) {
        super(FloraFaunaRegistry.ITEM_INPUT_BE.get(), pos, blockState);
    }

    // The base class handles all functionality.
    // Subclasses can override methods to customize behavior:
    // - tickCollection() for different collection patterns
    // - tickAbsorption() for different absorption effects
    // - tickTransfer() for different transfer priorities
}
