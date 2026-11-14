package net.j40climb.florafauna.test;

import com.mojang.authlib.GameProfile;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna.common.item.abilities.data.MiningSpeed;
import net.j40climb.florafauna.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna.common.item.abilities.networking.UpdateToolConfigPayload;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

final class HammerMiningGameTests {
    private static final Identifier MINE = Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "test_hammer_mine");

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env) {
        for (MiningShape mode : MiningShape.values()) {
            String name = "hammer_" + mode.getSerializedName() + "_all_faces";
            FloraFaunaGameTests.registerStructureTest(event, env, name, MINE, 100, h -> allFaces(h, mode));
        }
        FloraFaunaGameTests.registerStructureTest(event, env, "hammer_ore_drops", MINE, 100, HammerMiningGameTests::oreDrops);
        FloraFaunaGameTests.registerStructureTest(event, env, "hammer_repeated_excavation", MINE, 100, HammerMiningGameTests::repeat);
        FloraFaunaGameTests.registerStructureTest(event, env, "hammer_vein_limit_and_connectivity", MINE, 100, HammerMiningGameTests::vein);
        FloraFaunaGameTests.registerStructureTest(event, env, "hammer_tunnel_gaps", MINE, 100, HammerMiningGameTests::gaps);
        FloraFaunaGameTests.registerStructureTest(event, env, "hammer_dimension_floor", MINE, 100, HammerMiningGameTests::buildFloor);
        FloraFaunaGameTests.registerStructureTest(event, env, "hammer_permission_and_tool_rules", MINE, 100, HammerMiningGameTests::eligibility);
    }

    private static void allFaces(GameTestHelper h, MiningShape mode) {
        BlockPos center = h.absolutePos(new BlockPos(11, 14, 11));
        ServerPlayer player = player(h, mode);
        player.setGameMode(GameType.SURVIVAL);
        player.setOnGround(true);
        for (Direction face : Direction.values()) {
            for (Direction heading : Direction.Plane.HORIZONTAL) {
                // Check both player axes on vertical hits. Side hits depend only on the face.
                if (face.getAxis().isHorizontal() && heading != Direction.NORTH) continue;
                fill(player, center.offset(-7, -8, -7), center.offset(7, 9, 7), Blocks.STONE);
                Map<BlockPos, Block> expected = new HashMap<>();
                Direction stairsFace = Direction.NORTH;
                switch (mode) {
                    case SINGLE -> expected.put(center, Blocks.AIR);
                    case FLAT_3X3, FLAT_5X5, FLAT_7X7 -> {
                        int radius = switch (mode) {
                            case FLAT_3X3 -> 1;
                            case FLAT_5X5 -> 2;
                            default -> 3;
                        };
                        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius,-radius,-radius), center.offset(radius,radius,radius))) {
                            if (face.getAxis().choose(pos.getX()-center.getX(), pos.getY()-center.getY(), pos.getZ()-center.getZ()) == 0) {
                                expected.put(pos.immutable(), Blocks.AIR);
                            }
                        }
                    }
                    case SHAPELESS -> {
                        for (int i = 0; i < 4; i++) {
                            var ore = center.relative(face.getOpposite(), i);
                            set(player, ore, Blocks.IRON_ORE);
                            expected.put(ore, Blocks.AIR);
                        }
                    }
                    case WALKABLE_TUNNEL, LARGE_TUNNEL, BORE -> {
                        Direction endDirection = face.getOpposite();
                        int length = mode == MiningShape.BORE ? 7 : 5;
                        BlockPos end = center.relative(endDirection, length - 1);
                        BlockPos first = center;
                        BlockPos last = end;
                        if (mode == MiningShape.LARGE_TUNNEL) {
                            // One block of padding along each axis perpendicular to the depth.
                            int x = face.getAxis() == Direction.Axis.X ? 0 : 1;
                            int y = face.getAxis() == Direction.Axis.Y ? 0 : 1;
                            int z = face.getAxis() == Direction.Axis.Z ? 0 : 1;
                            first = center.offset(-x, -y, -z);
                            last = end.offset(x, y, z);
                        } else if (mode == MiningShape.WALKABLE_TUNNEL) {
                            if (face.getAxis().isHorizontal()) {
                                last = end.above(2);
                            } else {
                                first = center.relative(heading.getOpposite());
                                last = end.relative(heading);
                            }
                        }
                        for (BlockPos pos : BlockPos.betweenClosed(first, last)) expected.put(pos.immutable(), Blocks.AIR);
                        h.assertTrue(expected.size() == switch (mode) {
                            case WALKABLE_TUNNEL -> 15;
                            case LARGE_TUNNEL -> 45;
                            default -> 7;
                        }, "Incorrect test footprint for " + mode);
                        // Air and denied cells consume depth. Blocks beyond the gaps still break.
                        set(player, center.relative(endDirection), Blocks.AIR);
                        BlockPos protectedBlock = center.relative(endDirection, 2);
                        set(player, protectedBlock, Blocks.BEDROCK);
                        expected.put(protectedBlock, Blocks.BEDROCK);
                    }
                    case TUNNEL_UP, TUNNEL_DOWN -> {
                        boolean up = mode == MiningShape.TUNNEL_UP;
                        if (face == Direction.DOWN || (up && face == Direction.UP)) {
                            expected.put(center, Blocks.AIR);
                        } else {
                            Direction travel = face == Direction.UP ? heading : face.getOpposite();
                            stairsFace = up ? travel : travel.getOpposite();
                            BlockPos tread = face == Direction.UP ? center : center.below(up ? 1 : 2);
                            for (int step = 0; step < 5; step++) {
                                expected.put(tread, Blocks.COBBLESTONE_STAIRS);
                                for (int y = 1; y <= 4; y++) expected.put(tread.above(y), Blocks.AIR);
                                tread = tread.relative(travel).above(up ? 1 : -1);
                            }
                        }
                    }
                }
                swing(player, center, face, heading);
                for (BlockPos pos : BlockPos.betweenClosed(center.offset(-7, -8, -7), center.offset(7, 9, 7))) {
                    Block block = expected.getOrDefault(pos, Blocks.STONE);
                    check(h, pos, block, mode + " " + face + " " + heading);
                    if (block == Blocks.COBBLESTONE_STAIRS) stair(h, pos, stairsFace);
                }
            }
        }
        h.succeed();
    }

    private static void oreDrops(GameTestHelper h) {
        ServerPlayer player = player(h, MiningShape.FLAT_3X3);
        player.setGameMode(GameType.SURVIVAL);
        player.setOnGround(true);
        for (boolean silk : new boolean[]{false, true}) {
            var center = h.absolutePos(new BlockPos(silk ? 14 : 5, 10, 5));
            UpdateToolConfigPayload.applyToHeldItem(player, new ToolConfig(!silk, silk, MiningSpeed.INSTABREAK));
            set(player, center, Blocks.DIAMOND_ORE);
            set(player, center.east(), Blocks.DIAMOND_ORE);
            swing(player, center, Direction.UP, Direction.SOUTH);
            check(h, center, Blocks.AIR, "ore center");
            check(h, center.east(), Blocks.AIR, "ore neighbor");
            var bounds = new AABB(Vec3.atCenterOf(center), Vec3.atCenterOf(center.east())).inflate(2);
            var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, bounds);
            int count = 0;
            for (var drop : drops) {
                var stack = drop.getItem();
                h.assertTrue(stack.is(silk ? Items.DIAMOND_ORE : Items.DIAMOND), "Unexpected ore drop: " + stack);
                count += stack.getCount();
            }
            h.assertTrue(silk ? count == 2 : count >= 2 && count <= 8, "Wrong total ore loot: " + count);
            boolean hasExperience = !h.getLevel().getEntitiesOfClass(ExperienceOrb.class, bounds).isEmpty();
            h.assertTrue(hasExperience == !silk, "Wrong experience outcome for " + (silk ? "Silk Touch" : "Fortune"));
        }
        h.succeed();
    }

    private static void repeat(GameTestHelper h) {
        var player = player(h, MiningShape.FLAT_3X3);
        var start = h.absolutePos(new BlockPos(6, 22, 5));
        for (Direction travel : new Direction[]{Direction.SOUTH, Direction.DOWN}) {
            var end = start.relative(travel, 4);
            if (travel == Direction.SOUTH) fill(player, start.offset(-2, -2, -2), end.offset(2, 2, 2), Blocks.STONE);
            // Explicit box for the descending lane (the end is below the start).
            if (travel == Direction.DOWN) fill(player, end.offset(-2, -1, -2), start.offset(2, 1, 2), Blocks.STONE);
            for (int i = 0; i < 4; i++) {
                BlockPos center = start.relative(travel, i);
                swing(player, center, travel.getOpposite(), Direction.SOUTH);
                for (int x = -2; x <= 2; x++) {
                    for (int b = -2; b <= 2; b++) {
                        var pos = travel == Direction.DOWN ? center.offset(x, 0,b) : center.offset(x,b, 0);
                        check(h, pos, Math.abs(x) <= 1 && Math.abs(b) <= 1 ? Blocks.AIR : Blocks.STONE, "pass " + i);
                    }
                }
                check(h, center.relative(travel), Blocks.STONE, "backing");
            }
        }
        configure(player, MiningShape.TUNNEL_DOWN);
        start = h.absolutePos(new BlockPos(4, 24, 15));
        fill(player, start.offset(-1, -16, -1), start.offset(16, 5, 1), Blocks.STONE);
        for (int flight = 0; flight < 3; flight++) {
            var landing = start.offset(flight * 5, -flight * 5, 0);
            swing(player, landing, Direction.UP, Direction.EAST);
        }
        for (int step = 0; step < 15; step++) {
            var tread = start.offset(step, -step, 0);
            stair(h, tread, Direction.WEST);
            for (int y = 1; y <= 4; y++) check(h, tread.above(y), Blocks.AIR, "headroom");
            check(h, tread.above(5), Blocks.STONE, "roof");
            check(h, tread.below(), Blocks.STONE, "floor");
            check(h, tread.north(), Blocks.STONE, "side wall");
        }
        h.succeed();
    }

    private static void vein(GameTestHelper h) {
        var player = player(h, MiningShape.SHAPELESS);
        var center = h.absolutePos(new BlockPos(5, 12, 5));
        var last = center.offset(6, 0, 0);
        for (BlockPos pos : new BlockPos[]{center, center.east(), center.offset(2, 1, 0), center.offset(3, 2, 1), last}) {
            set(player, pos, Blocks.IRON_ORE);
        }
        set(player, center.north(), Blocks.COPPER_ORE);
        swing(player, center, Direction.NORTH, Direction.EAST);
        for (BlockPos pos : new BlockPos[]{center, center.east(), center.offset(2, 1, 0), center.offset(3, 2, 1)}) {
            check(h, pos, Blocks.AIR, "face, edge and corner neighbors");
        }
        check(h, last, Blocks.IRON_ORE, "disconnected island");
        check(h, center.north(), Blocks.COPPER_ORE, "different ore");
        // 4x4x4 plus one ore reachable only through the last column. Exactly 64 may break.
        var cap = center.above(5);
        fill(player, cap, cap.offset(3, 3, 3), Blocks.IRON_ORE);
        set(player, cap.offset(4, 3, 3), Blocks.IRON_ORE);
        swing(player, cap, Direction.UP, Direction.EAST);
        for (BlockPos pos : BlockPos.betweenClosed(cap, cap.offset(3, 3, 3))) check(h, pos, Blocks.AIR, "64-block cap");
        check(h, cap.offset(4, 3, 3), Blocks.IRON_ORE, "65th block");

        // Custom components can request a smaller cap. Malformed legacy values still have bounded behavior.
        var bounded = new MiningModeData(MiningShape.SHAPELESS, Integer.MAX_VALUE, Integer.MAX_VALUE, true);
        if (bounded.radius() != 3 || bounded.maxBlocksToBreak() != 64) throw h.assertionException("Unbounded component");
        player.getMainHandItem().set(FloraFaunaRegistry.MULTI_BLOCK_MINING, new MiningModeData(MiningShape.SHAPELESS, 1, 2, true));
        fill(player, cap, cap.east(2), Blocks.IRON_ORE);
        swing(player, cap, Direction.UP, Direction.NORTH);
        check(h, cap.east(), Blocks.AIR, "custom second block");
        check(h, cap.east(2), Blocks.IRON_ORE, "custom cap");
        h.succeed();
    }

    private static void buildFloor(GameTestHelper h) {
        var player = player(h, MiningShape.TUNNEL_DOWN);
        var area = h.absolutePos(new BlockPos(5, 0, 5));
        var level = h.getLevel();
        var center = new BlockPos(area.getX(), level.getMinY() + 2, area.getZ());
        // Stock GameTest structures are placed above the floor. Restore this small outlying fixture.
        Map<BlockPos, BlockState> original = new HashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.below(2), center.offset(4, 4, 0))) {
            original.put(pos.immutable(), level.getBlockState(pos));
            set(player, pos, Blocks.STONE);
        }
        try {
            swing(player, center, Direction.UP, Direction.EAST);
            for (int step = 0; step <= 2; step++) stair(h, center.east(step).below(step), Direction.WEST);
            check(h, center.east(3).below(2), Blocks.AIR, "headroom at minimum Y");
            check(h, center.east(3).below(3), Blocks.VOID_AIR, "below build height");
        } finally {
            original.forEach((pos, state) -> level.setBlock(pos, state, Block.UPDATE_CLIENTS));
        }
        h.succeed();
    }

    private static void gaps(GameTestHelper h) {
        var player = player(h, MiningShape.TUNNEL_UP);
        player.setGameMode(GameType.SURVIVAL);
        player.setOnGround(true);
        for (var mode : new MiningShape[]{MiningShape.TUNNEL_UP, MiningShape.TUNNEL_DOWN}) {
            configure(player, mode);
            boolean up = mode == MiningShape.TUNNEL_UP;
            var center = h.absolutePos(new BlockPos(5, 17, up ? 5 : 12));
            fill(player, center.offset(-1, -7, -1), center.offset(5, 8, 1), Blocks.STONE);
            // A two-column cave with air beneath the future stairs and solid rock on the far side.
            fill(player, center.offset(1, -7, -1), center.offset(2, 8, 1), Blocks.AIR);
            var firstTread = center.below(up ? 1 : 2);
            var bedrock = firstTread.east(3).above(up ? 3 : -3);
            set(player, bedrock, Blocks.BEDROCK);
            swing(player, center, Direction.WEST, Direction.EAST);
            for (int step = 0; step < 5; step++) {
                var tread = firstTread.east(step).above(up ? step : -step);
                if (step == 3) check(h, tread, Blocks.BEDROCK, "unbreakable tread");
                else stair(h, tread, up ? Direction.EAST : Direction.WEST);
                for (int y = 1; y <= 4; y++) check(h, tread.above(y), Blocks.AIR, "headroom");
                if (step == 1 || step == 2) {
                    check(h, tread.below(), Blocks.AIR, "empty space under the bridge");
                    check(h, tread.north(), Blocks.AIR, "empty space beside the bridge");
                }
            }
            check(h, center.east(5), Blocks.STONE, "swing ends at its normal reach");
        }
        h.succeed();
    }

    private static void eligibility(GameTestHelper h) {
        var player = player(h, MiningShape.FLAT_3X3);
        var center = h.absolutePos(new BlockPos(8, 12, 8));
        fill(player, center.offset(-1, -1, -1), center.offset(1, 1, 1), Blocks.STONE);
        Consumer<BreakBlockEvent> cancel = event -> {
            if (event.getPlayer() == player && event.getPos().equals(center)) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, BreakBlockEvent.class, cancel);
        try {
            swing(player, center, Direction.UP, Direction.NORTH);
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
        }
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
            check(h, pos, Blocks.STONE, "canceled center");
        }

        var protectedPos = center.west();
        Consumer<BreakBlockEvent> protect = event -> {
            if (event.getPlayer() == player && event.getPos().equals(protectedPos)) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, BreakBlockEvent.class, protect);
        var border = player.level().getWorldBorder();
        double x = border.getCenterX(), z = border.getCenterZ(), size = border.getSize();
        try {
            border.setCenter(center.getX() - 4.0, center.getZ() + 0.5);
            border.setSize(10.0); // east edge at center.x + 1
            swing(player, center, Direction.UP, Direction.NORTH);
        } finally {
            border.setCenter(x, z);
            border.setSize(size);
            NeoForge.EVENT_BUS.unregister(protect);
        }
        check(h, protectedPos, Blocks.STONE, "canceled extra");
        check(h, center.east(), Blocks.STONE, "outside world border");
        check(h, center, Blocks.AIR, "allowed center");
        check(h, center.north(), Blocks.AIR, "allowed neighbor");

        player.setGameMode(GameType.SURVIVAL);
        var pick = new ItemStack(Items.DIAMOND_PICKAXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, pick);
        for (boolean ignore : new boolean[]{false, true}) {
            pick.set(FloraFaunaRegistry.MULTI_BLOCK_MINING, new MiningModeData(MiningShape.FLAT_3X3, 1, 64, ignore));
            fill(player, center.offset(-1, 0, -1), center.offset(1, 0, 1), Blocks.STONE);
            set(player, center.west(), Blocks.OAK_LOG);
            set(player, center.south(), Blocks.BEDROCK);
            swing(player, center, Direction.UP, Direction.SOUTH);
            check(h, center, Blocks.AIR, "survival center");
            check(h, center.east(), Blocks.AIR, "pickaxe ore eligibility");
            check(h, center.west(), ignore ? Blocks.AIR : Blocks.OAK_LOG, "respect tool restrictions = " + !ignore);
            check(h, center.south(), Blocks.BEDROCK, "unbreakable block");
        }
        h.succeed();
    }

    private static ServerPlayer player(GameTestHelper h, MiningShape mode) {
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "mining-test"));
        player.setGameMode(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FloraFaunaRegistry.HAMMER.get()));
        configure(player, mode);
        return player;
    }

    private static void configure(ServerPlayer player, MiningShape mode) {
        player.getMainHandItem().set(FloraFaunaRegistry.MULTI_BLOCK_MINING, new MiningModeData(mode, mode.getRadius(), 64, true));
    }

    private static void swing(ServerPlayer player, BlockPos target, Direction face, Direction heading) {
        Vec3 stand = Vec3.atCenterOf(target.relative(face, 2));
        player.setPos(stand.x, stand.y - player.getEyeHeight(), stand.z);
        player.setYRot(heading.toYRot());
        player.gameMode.handleBlockBreakAction(target, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                face, player.level().getMaxY(), 0);
        if (!player.getAbilities().instabuild) {
            for (int tick = 0; tick < 20; tick++) player.gameMode.tick();
            player.gameMode.handleBlockBreakAction(target, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                    face, player.level().getMaxY(), 1);
        }
    }

    private static void set(ServerPlayer player, BlockPos pos, Block block) {
        player.level().setBlock(pos, block.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    private static void fill(ServerPlayer player, BlockPos min, BlockPos max, Block block) {
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) set(player, pos, block);
    }

    private static void check(GameTestHelper h, BlockPos pos, Block expected, String detail) {
        var state = h.getLevel().getBlockState(pos);
        if (!state.is(expected)) throw h.assertionException(detail + " at " + pos + ": expected " + expected + ", got " + state);
    }

    private static void stair(GameTestHelper h, BlockPos pos, Direction expected) {
        check(h, pos, Blocks.COBBLESTONE_STAIRS, "stair");
        var state = h.getLevel().getBlockState(pos);
        if (state.getValue(StairBlock.FACING) != expected || state.getValue(StairBlock.HALF) != Half.BOTTOM) {
            throw h.assertionException("Wrong stair at " + pos + ": expected bottom facing " + expected + ", got " + state);
        }
    }
}
