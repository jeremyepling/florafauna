package net.j40climb.florafauna.common.block;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.custom.SymbioteContainmentChamberBlock;
import net.j40climb.florafauna.common.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FloraFauna.MOD_ID);

    // Add blockers here

    public static final DeferredBlock<Block> TEAL_MOSS_BLOCK = registerBlock("teal_moss_block",
            (blockBehavior$Properties) -> new Block(blockBehavior$Properties
                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)));

    public static final DeferredBlock<SymbioteContainmentChamberBlock> SYMBIOTE_CONTAINMENT_CHAMBER =
            registerBlock("containment_chamber",
                    props -> new SymbioteContainmentChamberBlock(props
                            .strength(5f, 6f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .lightLevel(state -> 5)
                            .noOcclusion()
                    ));

    ///  This a helper method that registers the block and the block item
    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Function<BlockBehaviour.Properties, T> function) {
        DeferredBlock<T> blockToRegister = BLOCKS.registerBlock(name, function);
        ModItems.ITEMS.registerItem(name, (properties) -> new BlockItem(blockToRegister.get(), properties.useBlockDescriptionPrefix()));
        return blockToRegister;
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}


