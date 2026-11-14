package net.j40climb.florafauna.dev;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.Set;

public final class DevPlotCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (FMLEnvironment.isProduction()) return;
        var root = Commands.literal("ffplot").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        var build = Commands.literal("build").then(Commands.literal("all").executes(context -> {
            int placed = 0;
            for (DevPlot plot : DevPlot.values()) placed += build(context.getSource(), plot, false);
            return placed;
        }));
        var visit = Commands.literal("visit");
        var reset = Commands.literal("reset");
        for (DevPlot plot : DevPlot.values()) {
            build.then(Commands.literal(plot.id()).executes(c -> build(c.getSource(), plot, false)));
            reset.then(Commands.literal(plot.id()).executes(c -> build(c.getSource(), plot, true)));
            visit.then(Commands.literal(plot.id()).executes(c -> visit(c.getSource(), plot)));
        }
        dispatcher.register(root.then(build).then(reset).then(visit)
                .then(Commands.literal("list").executes(c -> list(c.getSource()))));
    }

    private static int list(CommandSourceStack source) {
        for (DevPlot plot : DevPlot.values()) {
            var saved = record(source, plot);
            BlockPos pos = plot.gridPosition(saved.getIntOr("y", 0));
            String address = saved.contains("y") ? pos.toShortString()
                    : pos.getX() + ", _, " + pos.getZ();
            source.sendSuccess(() -> Component.translatable(saved.contains("y")
                    ? "command.florafauna.plot.address" : "command.florafauna.plot.available", plot.id(), address), false);
        }
        return DevPlot.values().length;
    }

    // The vanilla command storage persists with this save (including copies and restarts).
    private static CompoundTag record(CommandSourceStack source, DevPlot plot) {
        return source.getServer().getCommandStorage().get(plot.structure());
    }

    private static int build(CommandSourceStack source, DevPlot plot, boolean reset) throws CommandSyntaxException {
        ServerLevel level = source.getServer().overworld();
        CompoundTag saved = record(source, plot);
        boolean installed = saved.contains("y");
        if (installed && !reset) {
            source.sendSuccess(() -> Component.translatable("command.florafauna.plot.exists", plot.id()), false);
            return 0;
        }
        if (reset && !installed) throw missing(plot);

        var template = plot.template(level);
        Vec3i size = template.getSize();
        BlockPos origin = plot.gridPosition(installed ? saved.getIntOr("y", 0) : surface(level, plot, size));
        BlockPos end = origin.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1);
        if (!level.isInWorldBounds(origin) || !level.isInWorldBounds(end) ||
                !level.getWorldBorder().isWithinBounds(origin) || !level.getWorldBorder().isWithinBounds(end)) {
            throw new SimpleCommandExceptionType(Component.translatable("command.florafauna.plot.bounds", plot.id())).create();
        }
        // A replacement structure may have grown beyond the area owned by the previous version.
        for (BlockPos pos : BlockPos.betweenClosed(origin, end)) {
            if (installed && pos.getX() - origin.getX() < saved.getIntOr("width", 0)
                    && pos.getY() - origin.getY() < saved.getIntOr("height", 0)
                    && pos.getZ() - origin.getZ() < saved.getIntOr("depth", 0)) continue;
            if (!level.isEmptyBlock(pos)) {
                throw new SimpleCommandExceptionType(Component.translatable("command.florafauna.plot.occupied", pos.toShortString())).create();
            }
        }
        plot.place(level, origin, template);
        var updated = new CompoundTag();
        updated.putInt("y", origin.getY());
        updated.putInt("width", size.getX());
        updated.putInt("height", size.getY());
        updated.putInt("depth", size.getZ());
        source.getServer().getCommandStorage().set(plot.structure(), updated);
        source.sendSuccess(() -> Component.translatable("command.florafauna.plot.built", plot.id(), origin.toShortString()), false);
        return 1;
    }

    private static int surface(ServerLevel level, DevPlot plot, Vec3i size) {
        BlockPos base = plot.gridPosition(0);
        int top = level.getMinY();
        for (int x = base.getX(); x < base.getX() + size.getX(); x++) {
            for (int z = base.getZ(); z < base.getZ() + size.getZ(); z++) {
                int height = level.getChunk(x >> 4, z >> 4).getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
                top = Math.max(top, height + 1);
            }
        }
        return top;
    }

    private static int visit(CommandSourceStack source, DevPlot plot) throws CommandSyntaxException {
        var saved = record(source, plot);
        if (!saved.contains("y")) throw missing(plot);
        var player = source.getPlayerOrException();
        var level = source.getServer().overworld();
        BlockPos entry = plot.gridPosition(saved.getIntOr("y", 0)).offset(2, 1, 0);
        if (level.getBlockState(entry.below()).isAir() || !level.isEmptyBlock(entry) || !level.isEmptyBlock(entry.above())) {
            throw new SimpleCommandExceptionType(Component.translatable("command.florafauna.plot.entry", plot.id())).create();
        }
        return player.teleportTo(level, entry.getX() + 0.5, entry.getY(), entry.getZ() + 0.5,
                Set.of(), 0, 10, true) ? 1 : 0;
    }

    private static CommandSyntaxException missing(DevPlot plot) {
        return new SimpleCommandExceptionType(Component.translatable("command.florafauna.plot.missing", plot.id())).create();
    }

    private DevPlotCommands() {}
}
