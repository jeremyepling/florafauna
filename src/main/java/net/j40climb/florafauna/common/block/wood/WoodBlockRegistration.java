package net.j40climb.florafauna.common.block.wood;

import net.j40climb.florafauna.common.block.ModBlocks;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Registers all blocks for a wood type (log, stripped_log, wood, stripped_wood, planks).
 * Uses ModBlocks.registerBlock() to ensure blocks and block items are registered together.
 *
 * Usage in ModBlocks.java:
 *   public static final WoodBlockSet DRIFTWOOD = WoodBlockRegistration.register("driftwood");
 *
 * To add a new wood type, just add another line with a different name.
 */
public class WoodBlockRegistration {

    public static WoodBlockSet register(String name) {
        DeferredBlock<RotatedPillarBlock> log = ModBlocks.registerBlock(name + "_log",
                props -> new RotatedPillarBlock(props.strength(2.0f).sound(SoundType.WOOD).ignitedByLava()));

        DeferredBlock<RotatedPillarBlock> strippedLog = ModBlocks.registerBlock("stripped_" + name + "_log",
                props -> new RotatedPillarBlock(props.strength(2.0f).sound(SoundType.WOOD).ignitedByLava()));

        DeferredBlock<RotatedPillarBlock> wood = ModBlocks.registerBlock(name + "_wood",
                props -> new RotatedPillarBlock(props.strength(2.0f).sound(SoundType.WOOD).ignitedByLava()));

        DeferredBlock<RotatedPillarBlock> strippedWood = ModBlocks.registerBlock("stripped_" + name + "_wood",
                props -> new RotatedPillarBlock(props.strength(2.0f).sound(SoundType.WOOD).ignitedByLava()));

        DeferredBlock<Block> planks = ModBlocks.registerBlock(name + "_planks",
                props -> new Block(props.strength(2.0f, 3.0f).sound(SoundType.WOOD).ignitedByLava()));

        DeferredBlock<SlabBlock> slab = ModBlocks.registerBlock(name + "_slab",
                props -> new SlabBlock(props.strength(2.0f, 3.0f).sound(SoundType.WOOD).ignitedByLava()));

        DeferredBlock<FenceBlock> fence = ModBlocks.registerBlock(name + "_fence",
                props -> new FenceBlock(props.strength(2.0f, 3.0f).sound(SoundType.WOOD).ignitedByLava()));

        DeferredBlock<FenceGateBlock> fenceGate = ModBlocks.registerBlock(name + "_fence_gate",
                props -> new FenceGateBlock(WoodType.OAK, props.strength(2.0f, 3.0f).sound(SoundType.WOOD).ignitedByLava()));

        // TODO: Future phases - Add these when ready
        // DeferredBlock<StairBlock> stairs = ModBlocks.registerBlock(name + "_stairs",
        //         props -> new StairBlock(planks.get().defaultBlockState(), props.strength(2.0f, 3.0f).sound(SoundType.WOOD).ignitedByLava()));
        // DeferredBlock<DoorBlock> door = ModBlocks.registerBlock(name + "_door",
        //         props -> new DoorBlock(BlockSetType.OAK, props.strength(3.0f).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));
        // DeferredBlock<TrapDoorBlock> trapdoor = ModBlocks.registerBlock(name + "_trapdoor",
        //         props -> new TrapDoorBlock(BlockSetType.OAK, props.strength(3.0f).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));
        // DeferredBlock<PressurePlateBlock> pressurePlate = ModBlocks.registerBlock(name + "_pressure_plate",
        //         props -> new PressurePlateBlock(BlockSetType.OAK, props.strength(0.5f).sound(SoundType.WOOD).noCollission().ignitedByLava()));
        // DeferredBlock<ButtonBlock> button = ModBlocks.registerBlock(name + "_button",
        //         props -> new ButtonBlock(BlockSetType.OAK, 30, props.strength(0.5f).sound(SoundType.WOOD).noCollission().ignitedByLava()));

        return new WoodBlockSet(log, strippedLog, wood, strippedWood, planks, slab, fence, fenceGate);
    }
}
