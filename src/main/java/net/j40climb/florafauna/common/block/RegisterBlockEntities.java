package net.j40climb.florafauna.common.block;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.cocoonchamber.CocoonChamberBlockEntity;
import net.j40climb.florafauna.common.block.containmentchamber.ContainmentChamberBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class RegisterBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, FloraFauna.MOD_ID);

    public static final Supplier<BlockEntityType<ContainmentChamberBlockEntity>> SYMBIOTE_CONTAINMENT_CHAMBER = BLOCK_ENTITIES.register(
            "containment_chamber",
            () -> new BlockEntityType<>(
                            ContainmentChamberBlockEntity::new,
                            false,
                            RegisterBlocks.SYMBIOTE_CONTAINMENT_CHAMBER.get()
                    )
            );

    public static final Supplier<BlockEntityType<CocoonChamberBlockEntity>> COCOON_CHAMBER = BLOCK_ENTITIES.register(
            "cocoon_chamber",
            () -> new BlockEntityType<>(
                            CocoonChamberBlockEntity::new,
                            false,
                            RegisterBlocks.COCOON_CHAMBER.get()
                    )
            );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}