package net.j40climb.florafauna.platform;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Fabric's construction adapter for shared block-entity definitions. */
public final class PlatformRegistration {
    private PlatformRegistration() {}
    public static <T extends BlockEntity> BlockEntityType<T> blockEntity(BlockEntityFactory<T> factory, Block... blocks) {
        return FabricBlockEntityTypeBuilder.create(factory::create, blocks).build();
    }
}
