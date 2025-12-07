package net.j40climb.florafauna.common.block.entity;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, FloraFauna.MOD_ID);

    public static final Supplier<BlockEntityType<SymbioteContainmentChamberBlockEntity>> SYMBIOTE_CONTAINMENT_CHAMBER = BLOCK_ENTITIES.register(
            "symbiote_containment_chamber",
            () -> new BlockEntityType<>(
                            SymbioteContainmentChamberBlockEntity::new,
                            false,
                            ModBlocks.SYMBIOTE_CONTAINMENT_CHAMBER.get()
                    )
            );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}