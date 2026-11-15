package net.j40climb.florafauna.dev;

import net.j40climb.florafauna.FloraFauna;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Permanent grid slots. Keep a plot's slot when adding or reordering entries. */
public enum DevPlot {
    ITEM_INPUT(0), RECOVERY(1), PODS(2), FRENCHIE(3), HAMMER(4), TRANSPORT(5), BARRIER(6),
    ANCHOR_BENCH(7), BARRIER_PENS(8), CREEPER_FEAR(9), BLAZE_FEAR(10), ENDERMAN_FEAR(11), LONG_ROUTE(12), TOOL_ACTIONS(13),
    IRON_GARDEN(14);

    private static final int COLUMNS = 4;
    private static final int SPACING = 32;
    private final int slot;
    private final String id;
    private final Identifier structure;

    DevPlot(int slot) {
        this.slot = slot;
        id = name().toLowerCase(Locale.ROOT);
        structure = Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "plots/" + id);
    }

    static {
        Set<Integer> slots = new HashSet<>();
        for (DevPlot plot : values()) {
            if (plot.slot < 0 || !slots.add(plot.slot)) {
                throw new IllegalStateException("Invalid or duplicate plot slot " + plot.slot);
            }
        }
    }

    public String id() { return id; }
    public Identifier structure() { return structure; }

    BlockPos gridPosition(int y) {
        return new BlockPos(96 + slot % COLUMNS * SPACING, y, -48 + slot / COLUMNS * SPACING);
    }

    StructureTemplate template(ServerLevel level) {
        //? if <26.3 {
        StructureTemplate template = level.getStructureManager().get(structure)
        //?} else {
        /*StructureTemplate template = level.getStructureTemplateManager().get(structure)
        *///?}
                .orElseThrow(() -> new IllegalStateException("Missing plot structure " + structure));
        Vec3i size = template.getSize();
        if (size.getX() < 3 || size.getY() < 3 || size.getZ() < 2 ||
                size.getX() > SPACING - 4 || size.getZ() > SPACING - 4 || size.getY() > 48) {
            throw new IllegalStateException("Plot " + structure + " has invalid dimensions " + size.toShortString());
        }
        return template;
    }

    public void place(ServerLevel level, BlockPos origin) {
        place(level, origin, template(level));
    }

    void place(ServerLevel level, BlockPos origin, StructureTemplate template) {
        var settings = new StructurePlaceSettings().setIgnoreEntities(true);
        settings.setBoundingBox(template.getBoundingBox(settings, origin));
        if (!template.placeInWorld(level, origin, origin, settings, level.getRandom(), Block.UPDATE_CLIENTS)) {
            throw new IllegalStateException("Could not place " + structure + " at " + origin.toShortString());
        }
    }
}
