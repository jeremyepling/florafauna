package net.j40climb.florafauna.platform;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** NeoForge's construction adapter for shared block-entity definitions. */
public final class PlatformRegistration {
    private PlatformRegistration() {}
    public static <T extends BlockEntity> BlockEntityType<T> blockEntity(BlockEntityFactory<T> factory, Block... blocks) {
        return new BlockEntityType<>(factory::create, false, blocks);
    }
}
