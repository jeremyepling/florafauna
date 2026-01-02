package net.j40climb.florafauna.common.block;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.cocoonchamber.CocoonChamberBlock;
import net.j40climb.florafauna.common.block.containmentchamber.ContainmentChamberBlock;
import net.j40climb.florafauna.common.block.wood.ModWoodType;
import net.j40climb.florafauna.common.item.RegisterItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

/**
 * Central registry for all mod blocks. Blocks registered here automatically
 * get a corresponding BlockItem registered to ModItems.
 *
 * ACCESSING BLOCKS:
 *   Regular blocks:
 *     ModBlocks.TEAL_MOSS_BLOCK.get()    - Get the Block instance
 *     ModBlocks.TEAL_MOSS_BLOCK          - Get DeferredBlock (for creative tabs, recipes, etc.)
 *
 *   Wood blocks (via ModWoodType enum):
 *     ModWoodType.DRIFTWOOD.getBlockSet().log().get()   - Get the Block instance
 *     ModWoodType.DRIFTWOOD.getBlockSet().log()         - Get DeferredBlock
 *     ModWoodType.DRIFTWOOD.getBlockSet().planks()      - Access different block types
 *
 * Wood blocks live in ModWoodType enum instead of here because the enum
 * handles registration automatically - just add a new enum entry to create
 * a full set of wood blocks (log, stripped_log, wood, stripped_wood, planks).
 */
public class RegisterBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FloraFauna.MOD_ID);

    // ========== MISC BLOCKS ==========
    public static final DeferredBlock<Block> TEAL_MOSS_BLOCK = registerBlock("teal_moss_block",
            props -> new Block(props.strength(4f).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)));

    public static final DeferredBlock<ContainmentChamberBlock> SYMBIOTE_CONTAINMENT_CHAMBER =
            registerBlock("containment_chamber",
                    props -> new ContainmentChamberBlock(props
                            .strength(5f, 6f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .lightLevel(state -> 5)
                            .noOcclusion()
                    ));

    public static final DeferredBlock<CocoonChamberBlock> COCOON_CHAMBER =
            registerBlock("cocoon_chamber",
                    props -> new CocoonChamberBlock(props
                            .strength(5f, 6f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.SCULK)
                            .lightLevel(state -> 7)
                            .noOcclusion()
                    ));

    public static final DeferredBlock<Block> COPPER_GOLEM_BARRIER = registerBlock("copper_golem_barrier",
            props -> new CopperGolemBarrierBlock(props
                    .noOcclusion()
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.STONE)
            ));

    // ========== WOOD BLOCKS ==========
    // Wood blocks are registered via ModWoodType enum. We call this method to ensure
    // the enum class loads when ModBlocks loads. Loading the enum triggers its static
    // initialization, which runs the enum constructors and registers all wood blocks.
    static { registerWoodTypes(); }

    private static void registerWoodTypes() {
        ModWoodType.values();
    }

    // ========== BLOCK REGISTRATION HELPER ==========
    public static <T extends Block> DeferredBlock<T> registerBlock(String name, Function<BlockBehaviour.Properties, T> function) {
        DeferredBlock<T> blockToRegister = BLOCKS.registerBlock(name, function);
        RegisterItems.ITEMS.registerItem(name, (properties) -> new BlockItem(blockToRegister.get(), properties.useBlockDescriptionPrefix()));
        return blockToRegister;
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}


