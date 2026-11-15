package net.j40climb.florafauna.common.item.abilities.multiblock;

import net.j40climb.florafauna.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;

final class TunnelLighting {
    private static final int DARK_LIGHT_LEVEL = 7;
    private static final WeakHashMap<ServerLevel, Set<BlockPos>> pendingLights = new WeakHashMap<>();

    /** First walkable foot cell and the same axes used to excavate the tunnel. */
    record Passage(BlockPos first, Direction travel, int rise, int halfWidth, int depth) {
        BlockPos wallCell(int step) {
            return first.relative(travel, step).above(step * rise + 1).relative(travel.getClockWise(), halfWidth);
        }
    }

    static void afterMining(ServerPlayer player, ItemStack tool, Passage passage, Set<BlockPos> mined) {
        if (!enabled(player, tool) || findTorches(player).isEmpty()) return;
        List<BlockPos> cells = new ArrayList<>();
        for (int step = 0; step < passage.depth(); step++) {
            BlockPos pos = passage.wallCell(step);
            if (mined.contains(pos) || mined.contains(pos.below())) cells.add(pos);
        }
        if (cells.isEmpty()) return;
        ServerLevel level = player.level();
        var engine = level.getChunkSource().getLightEngine();
        // Newly excavated stone still has its old light value until propagation completes.
        var updates = cells.stream().map(ChunkPos::containing).distinct()
                .map(chunk -> engine.waitForPendingTasks(chunk.x(), chunk.z())).toArray(CompletableFuture<?>[]::new);
        CompletableFuture.allOf(updates).thenRunAsync(() -> {
            if (!player.isRemoved() && player.level() == level && enabled(player, tool)) {
                lightPassage(player, cells, passage.travel().getClockWise());
            }
        }, level.getServer());
    }

    private static boolean enabled(ServerPlayer player, ItemStack tool) {
        return player.getMainHandItem() == tool && !tool.isEmpty() && player.isAlive() && !player.isSpectator()
                && tool.getOrDefault(FloraFaunaRegistry.TOOL_CONFIG, ToolConfig.DEFAULT).tunnelLighting();
    }

    private static ItemStack findTorches(ServerPlayer player) {
        if (player.getOffhandItem().is(Items.TORCH)) return player.getOffhandItem();
        for (var stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(Items.TORCH)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static void lightPassage(ServerPlayer player, List<BlockPos> cells, Direction right) {
        ItemStack torches = findTorches(player);
        if (torches.isEmpty() || player.getCooldowns().isOnCooldown(torches)) return;
        ServerLevel level = player.level();
        Direction face = right.getOpposite();
        for (BlockPos pos : cells) {
            BlockPos support = pos.relative(right);
            if (!MultiBlockPatterns.canInteract(level, player, pos) || !MultiBlockPatterns.canInteract(level, player, support)
                    || !level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.below())
                    || !WallTorchBlock.canSurvive(level, pos, face)
                    || level.getMaxLocalRawBrightness(pos) > DARK_LIGHT_LEVEL || recentlyLit(level, pos)) continue;

            var hand = player.getOffhandItem() == torches ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            var hit = new BlockHitResult(Vec3.atCenterOf(support).relative(face, 0.5), face, support, false);
            var context = new UseOnContext(level, player, hand, torches, hit);
            // A replaceable wall would redirect this click into the wall itself.
            if (!new BlockPlaceContext(context).getClickedPos().equals(pos)) continue;
            if (torches.useOn(context).consumesAction() && level.getBlockState(pos).is(Blocks.WALL_TORCH)) {
                player.getInventory().setChanged();
                rememberUntilLit(level, pos);
            }
            return;
        }
    }

    private static void rememberUntilLit(ServerLevel level, BlockPos pos) {
        var pending = pendingLights.computeIfAbsent(level, ignored -> new HashSet<>());
        pending.add(pos);
        var chunk = ChunkPos.containing(pos);
        level.getChunkSource().getLightEngine().waitForPendingTasks(chunk.x(), chunk.z())
                .thenRunAsync(() -> pending.remove(pos), level.getServer());
    }

    /** Another swing can finish before a torch placed this tick reaches the light engine. */
    private static boolean recentlyLit(ServerLevel level, BlockPos pos) {
        var pending = pendingLights.get(level);
        if (pending == null || pending.stream().noneMatch(torch -> torch.distManhattan(pos) <= 6)) return false;
        // Only a nearby, still-propagating torch needs a short air-path check to rule out a separating wall.
        var queue = new ArrayDeque<BlockPos>();
        var visited = new HashSet<BlockPos>();
        queue.add(pos);
        visited.add(pos);
        for (int distance = 0; distance <= 6; distance++) {
            int count = queue.size();
            for (int i = 0; i < count; i++) {
                BlockPos next = queue.remove();
                var state = level.getBlockState(next);
                if (pending.contains(next) && state.is(Blocks.WALL_TORCH)) return true;
                if (!state.isAir() || distance == 6) continue;
                for (Direction direction : Direction.values()) {
                    BlockPos neighbor = next.relative(direction);
                    if (visited.add(neighbor) && level.hasChunkAt(neighbor)) queue.add(neighbor);
                }
            }
        }
        return false;
    }

    private TunnelLighting() {}
}
