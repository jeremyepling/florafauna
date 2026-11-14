package net.j40climb.florafauna.playtest;

import net.j40climb.florafauna.common.block.mobtransport.MobInputBlockEntity;
import net.j40climb.florafauna.common.block.mobtransport.MobOutputBlockEntity;
import net.j40climb.florafauna.dev.DevPlot;
import net.minecraft.client.gui.screens.inventory.LecternScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
//? if <26.3 {
import net.minecraft.world.level.block.entity.BlockEntityType;
//?} else {
/*import net.minecraft.world.level.block.entity.BlockEntityTypes;*/
//?}
import net.minecraft.world.level.block.entity.SignBlockEntity;
//? if >=26.3 {
/*import net.minecraft.world.level.block.entity.SignTextSlot;*/
//?}
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.j40climb.florafauna.playtest.Playtest.require;

final class PlotsPlaytest {
    static void steps(Playtest t, int phase) {
        if (phase == 1) {
            t.server("Restore installed fixtures in the disposable copy", player -> {
                var storage = player.level().getServer().getCommandStorage();
                for (DevPlot plot : DevPlot.values()) {
                    if (storage.get(plot.structure()).contains("y")) command(player, "reset " + plot.id());
                }
            });
            t.server("Build a single plot and mark its entrance floor", player -> {
                command(player, "build hammer");
                command(player, "visit hammer");
                checkGuide(player, DevPlot.HAMMER);
                player.level().setBlockAndUpdate(floorMarker(player), Blocks.GOLD_BLOCK.defaultBlockState());
            });
            t.server("Build the combined layout twice and visit each distinct entrance", player -> {
                command(player, "build all");
                command(player, "build all");
                Set<BlockPos> entries = new HashSet<>();
                for (DevPlot plot : DevPlot.values()) {
                    command(player, "visit " + plot.id());
                    checkGuide(player, plot);
                    require(entries.add(player.blockPosition()), "Duplicate address for " + plot.id());
                }
                command(player, "visit hammer");
                require(player.level().getBlockState(floorMarker(player)).is(Blocks.GOLD_BLOCK), "Build replaced a manual edit");
            });
            t.server("Pair the relocated long route and reset it to the unpaired fixture", player -> {
                command(player, "visit long_route");
                var level = player.level();
                BlockPos origin = player.blockPosition().offset(-2, -1, 0);
                BlockPos inputPos = origin.offset(14, 1, 10);
                BlockPos outputPos = origin.offset(1, 1, 4);
                var input = (MobInputBlockEntity) level.getBlockEntity(inputPos);
                var output = (MobOutputBlockEntity) level.getBlockEntity(outputPos);
                require(!input.isPaired() && output.getInputCount() == 0, "Route imported an old world link");
                input.pairWithOutput(outputPos);
                require(output.getPairedInputs().contains(inputPos), "Relocated endpoints failed to pair");
                command(player, "reset long_route");
                require(!((MobInputBlockEntity) level.getBlockEntity(inputPos)).isPaired()
                        && ((MobOutputBlockEntity) level.getBlockEntity(outputPos)).getInputCount() == 0, "Reset retained links");
            });
            return;
        }
        t.server("Revisit saved addresses and reset only the chosen plot", player -> {
            command(player, "visit pods");
            BlockPos neighbor = floorMarker(player);
            player.level().setBlockAndUpdate(neighbor, Blocks.LAPIS_BLOCK.defaultBlockState());
            command(player, "visit hammer");
            checkGuide(player, DevPlot.HAMMER);
            require(player.level().getBlockState(floorMarker(player)).is(Blocks.GOLD_BLOCK), "Lost plot or edit on restart");
            command(player, "reset hammer");
            require(player.level().getBlockState(floorMarker(player)).is(Blocks.POLISHED_ANDESITE), "Reset missed the edited block");
            require(player.level().getBlockState(neighbor).is(Blocks.LAPIS_BLOCK), "Reset overwrote the neighboring plot");
            player.level().setBlockAndUpdate(neighbor, Blocks.POLISHED_ANDESITE.defaultBlockState());
        });
        t.awaitClient("Receive the hammer entrance and its book", mc ->
                mc.level.getBlockState(mc.player.blockPosition().south()).getOptionalValue(LecternBlock.HAS_BOOK).orElse(false));
        t.client("Read the hammer guide", mc -> {
            BlockPos book = mc.player.blockPosition().south();
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(book), Direction.NORTH, book, false));
        });
        t.awaitClient("Open the lectern screen", mc -> mc.screen instanceof LecternScreen);
        t.screenshot("plots-field-guide");
    }

    private static BlockPos floorMarker(ServerPlayer player) {
        return player.blockPosition().offset(1, -1, 0);
    }

    private static void checkGuide(ServerPlayer player, DevPlot plot) {
        var level = player.level();
        BlockPos entry = player.blockPosition();
        require(level.getBlockEntity(entry.offset(-2, 0, 1)) instanceof SignBlockEntity sign
        //? if <26.3 {
                && sign.getFrontText().getMessage(2, false).getString().equals(plot.id()), "Wrong entrance for " + plot.id());
        var lectern = level.getBlockEntity(entry.south(), BlockEntityType.LECTERN);
        //?} else {
        /*        && sign.getText(SignTextSlot.FRONT).getMessages(false).get(2).getString().equals(plot.id()), "Wrong entrance for " + plot.id());
        var lectern = level.getBlockEntity(entry.south(), BlockEntityTypes.LECTERN);*/
        //?}
        var book = lectern.map(block -> block.getBook().get(DataComponents.WRITTEN_BOOK_CONTENT))
                .orElseThrow(() -> new AssertionError("Missing field guide for " + plot.id()));
        List<String> pages = switch (plot) {
            case FRENCHIE -> List.of("setup", "steps", "expect", "carry", "release");
            case RECOVERY -> List.of("setup", "steps", "expect", "owner", "full", "partial", "rejoin");
            default -> List.of("setup", "steps", "expect");
        };
        require(book.pages().size() == pages.size(), "Wrong page count for " + plot.id());
        for (int i = 0; i < pages.size(); i++) {
            String key = "plot.florafauna." + plot.id() + "." + pages.get(i);
            var page = book.pages().get(i).raw();
            require(page.getContents() instanceof TranslatableContents contents
                    && contents.getKey().equals(key) && !page.getString().equals(key), "Missing field guide text for " + key);
        }
        if (plot != DevPlot.ITEM_INPUT) {
            require(level.getBlockEntity(entry.offset(2, 0, 1)) instanceof BarrelBlockEntity kit && !kit.isEmpty(),
                    "Missing supplies for " + plot.id());
        }
    }

    private static void command(ServerPlayer player, String args) {
        var server = player.level().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withEntity(player), "ffplot " + args);
    }
}
