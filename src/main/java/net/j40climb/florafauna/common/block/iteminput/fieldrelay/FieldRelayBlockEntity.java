package net.j40climb.florafauna.common.block.iteminput.fieldrelay;

import net.j40climb.florafauna.common.block.iteminput.shared.AbstractItemInputBlockEntity;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the Field Relay.
 * Uses the default behavior from AbstractItemInputBlockEntity.
 *
 * The Field Relay is designed for large-scale item collection from farms
 * and mob grinders. It has a larger collection radius than other item inputs.
 *
 * Future enhancements could include:
 * - Larger collection radius (configurable)
 * - Filter system for specific item types
 * - Priority system for multiple relays
 */
public class FieldRelayBlockEntity extends AbstractItemInputBlockEntity {

    public FieldRelayBlockEntity(BlockPos pos, BlockState blockState) {
        super(FloraFaunaRegistry.FIELD_RELAY_BE.get(), pos, blockState);
    }

    // The base class handles all functionality.
    // Subclasses can override methods to customize behavior:
    // - tickCollection() for different collection patterns
    // - tickAbsorption() for different absorption effects
    // - tickTransfer() for different transfer priorities
}
