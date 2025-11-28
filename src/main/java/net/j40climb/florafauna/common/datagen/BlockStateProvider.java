package net.j40climb.florafauna.common.datagen;

import net.j40climb.florafauna.FloraFauna;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

public class BlockStateProvider extends net.neoforged.neoforge.client.model.generators.BlockStateProvider {
    public BlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, FloraFauna.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // No blocks with models currently
    }

    private void blockWithItem(DeferredBlock<Block> deferredBlock) {
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }
}
