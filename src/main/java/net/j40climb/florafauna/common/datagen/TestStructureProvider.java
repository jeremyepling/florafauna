package net.j40climb.florafauna.common.datagen;

import net.j40climb.florafauna.FloraFauna;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Generates empty structure templates for GameTest.
 */
public class TestStructureProvider implements DataProvider {

    private final PackOutput output;
    private final CompletableFuture<HolderLookup.Provider> lookupProvider;

    public TestStructureProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        this.output = output;
        this.lookupProvider = lookupProvider;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return lookupProvider.thenAccept(lookup -> {
            Path outputPath = output.getOutputFolder()
                    .resolve("data")
                    .resolve(FloraFauna.MOD_ID)
                    .resolve("structure")
                    .resolve("empty_1x1x1.nbt");

            // Create minimal empty structure NBT
            CompoundTag structureTag = createEmptyStructure(1, 1, 1);

            try {
                outputPath.getParent().toFile().mkdirs();
                NbtIo.writeCompressed(structureTag, outputPath);
            } catch (Exception e) {
                throw new RuntimeException("Failed to write structure NBT: " + outputPath, e);
            }
        });
    }

    /**
     * Create a minimal empty structure NBT tag.
     * Based on Minecraft's structure template format.
     */
    private CompoundTag createEmptyStructure(int sizeX, int sizeY, int sizeZ) {
        CompoundTag tag = new CompoundTag();

        // Size
        ListTag sizeList = new ListTag();
        sizeList.add(net.minecraft.nbt.IntTag.valueOf(sizeX));
        sizeList.add(net.minecraft.nbt.IntTag.valueOf(sizeY));
        sizeList.add(net.minecraft.nbt.IntTag.valueOf(sizeZ));
        tag.put("size", sizeList);

        // Empty blocks list (air fills the space)
        tag.put("blocks", new ListTag());

        // Empty entities list
        tag.put("entities", new ListTag());

        // Palette with just air
        ListTag palette = new ListTag();
        CompoundTag airBlock = new CompoundTag();
        airBlock.putString("Name", "minecraft:air");
        palette.add(airBlock);
        tag.put("palette", palette);

        // Data version (1.21.x)
        tag.putInt("DataVersion", 3953);

        return tag;
    }

    @Override
    public String getName() {
        return "Test Structures: " + FloraFauna.MOD_ID;
    }
}
