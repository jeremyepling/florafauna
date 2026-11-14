package net.j40climb.florafauna.common.block.wood;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import net.neoforged.neoforge.registries.datamaps.builtin.Transformable;

import java.util.concurrent.CompletableFuture;

public class WoodTransformsProvider extends DataMapProvider {
    public WoodTransformsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void gather(HolderLookup.Provider registries) {
        var transforms = builder(NeoForgeDataMaps.TRANSFORMABLES);
        for (WoodType type : WoodType.values()) {
            WoodBlockSet wood = type.getBlockSet();
            transforms.add(wood.log(), Transformable.stripping(wood.log().get(), wood.strippedLog().get()), false);
            transforms.add(wood.wood(), Transformable.stripping(wood.wood().get(), wood.strippedWood().get()), false);
        }
    }
}
