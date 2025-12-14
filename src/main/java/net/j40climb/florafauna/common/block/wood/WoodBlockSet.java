package net.j40climb.florafauna.common.block.wood;

import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Holds all the block references for a single wood type.
 * Access blocks via: ModBlocks.DRIFTWOOD.log(), ModBlocks.DRIFTWOOD.planks(), etc.
 */
public record WoodBlockSet(
        DeferredBlock<RotatedPillarBlock> log,
        DeferredBlock<RotatedPillarBlock> strippedLog,
        DeferredBlock<RotatedPillarBlock> wood,
        DeferredBlock<RotatedPillarBlock> strippedWood,
        DeferredBlock<Block> planks,
        DeferredBlock<SlabBlock> slab,
        DeferredBlock<FenceBlock> fence,
        DeferredBlock<FenceGateBlock> fenceGate
        // TODO: Future phases - Add these fields when ready
        // DeferredBlock<StairBlock> stairs,
        // DeferredBlock<DoorBlock> door,
        // DeferredBlock<TrapDoorBlock> trapdoor,
        // DeferredBlock<PressurePlateBlock> pressurePlate,
        // DeferredBlock<ButtonBlock> button
) {}
