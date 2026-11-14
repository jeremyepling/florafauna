package net.j40climb.florafauna.test;

import com.mojang.authlib.GameProfile;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna.common.item.abilities.data.MiningSpeed;
import net.j40climb.florafauna.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna.common.item.abilities.multiblock.RemoteOreStrike;
import net.j40climb.florafauna.common.item.abilities.networking.ChargedThrowPayload;
import net.j40climb.florafauna.common.item.abilities.networking.UpdateToolConfigPayload;
import net.j40climb.florafauna.common.item.hammer.HammerItem;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.j40climb.florafauna.common.item.projectile.ThrownItemEntity;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.UUID;
import java.util.function.Consumer;

final class ChargedHammerGameTests {
    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env) {
        var mine = Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "test_hammer_mine");
        FloraFaunaGameTests.registerStructureTest(event, env, "hammer_charged_ore_rules", mine, 100, ChargedHammerGameTests::oreRules);
        FloraFaunaGameTests.registerStructureTest(event, env, "hammer_charged_first_surface", mine, 100, ChargedHammerGameTests::firstSurface);
        FloraFaunaGameTests.registerStructureTest(event, env, "hammer_charged_permissions", mine, 100, ChargedHammerGameTests::permissions);
        FloraFaunaGameTests.registerStructureTest(event, env, "hammer_charged_survival_reload", mine, 100, h -> flight(h, false));
        FloraFaunaGameTests.registerStructureTest(event, env, "hammer_charged_creative_reload", mine, 100, h -> flight(h, true));
        FloraFaunaGameTests.registerStructureTest(event, env, "hammer_charged_tap_and_recharge", mine, 100, ChargedHammerGameTests::tap);
    }

    private static ServerPlayer player(GameTestHelper h, boolean creative) {
        var p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "charge-test"));
        p.setGameMode(creative ? GameType.CREATIVE : GameType.SURVIVAL);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FloraFaunaRegistry.HAMMER.get()));
        return p;
    }

    private static void rock(ServerPlayer p, BlockPos pos, Block block) {
        p.level().setBlock(pos, block.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    private static void oreRules(GameTestHelper h) {
        for (boolean exact : new boolean[]{false, true}) {
            var p = player(h, false);
            var hammer = p.getMainHandItem();
            UpdateToolConfigPayload.applyToHeldItem(p, new ToolConfig(false, true, MiningSpeed.STANDARD, exact));
            hammer.set(FloraFaunaRegistry.MULTI_BLOCK_MINING, new MiningModeData(MiningShape.SINGLE, 0, 2, true));
            var selected = new ItemStack(Items.STICK, 5);
            p.setItemInHand(InteractionHand.MAIN_HAND, selected);
            var center = h.absolutePos(new BlockPos(4, 6, exact ? 14 : 4));
            rock(p, center, Blocks.DIAMOND_ORE);
            rock(p, center.east(), Blocks.DEEPSLATE_DIAMOND_ORE);
            rock(p, center.east(2), Blocks.DIAMOND_ORE);
            rock(p, center.north(), Blocks.IRON_ORE);
            RemoteOreStrike.hit(p, hammer, center);
            int count = exact ? 1 : 2;
            h.assertTrue(HammerItem.charge(hammer) == HammerItem.MAX_CHARGE - count, "Only successful blocks should cost charge");
            h.assertTrue(p.getMainHandItem() == selected, "Hotbar stack was replaced");
            h.assertTrue(p.level().isEmptyBlock(center) && p.level().isEmptyBlock(center.east()) != exact
                    && p.level().getBlockState(center.east(2)).is(Blocks.DIAMOND_ORE)
                    && p.level().getBlockState(center.north()).is(Blocks.IRON_ORE), "Wrong remote vein or cap");
            var drops = p.level().getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(3));
            int silk = drops.stream().filter(e -> e.getItem().is(Items.DIAMOND_ORE) || e.getItem().is(Items.DEEPSLATE_DIAMOND_ORE))
                    .mapToInt(e -> e.getItem().getCount()).sum();
            h.assertTrue(silk == count, "Expected " + count + " silk ore blocks, got " + drops.stream().map(ItemEntity::getItem).toList());
        }
        h.succeed();
    }

    private static void permissions(GameTestHelper h) {
        var p = player(h, false);
        var hammer = p.getMainHandItem();
        hammer.set(FloraFaunaRegistry.HAMMER_CHARGE, 1);
        var center = h.absolutePos(new BlockPos(6, 8, 8));
        rock(p, center, Blocks.DIAMOND_ORE);
        rock(p, center.east(), Blocks.DIAMOND_ORE);
        rock(p, center.west(), Blocks.DIAMOND_ORE);
        var held = new ItemStack(Items.STONE);
        p.setItemInHand(InteractionHand.MAIN_HAND, held);
        Consumer<BreakBlockEvent> reject = event -> {
            if (event.getPlayer() == p) {
                h.assertTrue(event.getPlayer().getMainHandItem() == hammer, "Break hook saw the wrong tool");
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, BreakBlockEvent.class, reject);
        try {
            RemoteOreStrike.hit(p, hammer, center);
        } finally {
            NeoForge.EVENT_BUS.unregister(reject);
        }
        h.assertTrue(p.getMainHandItem() == held && HammerItem.charge(hammer) == 1 && !p.level().isEmptyBlock(center)
                && !p.level().isEmptyBlock(center.east()) && !p.level().isEmptyBlock(center.west()), "Denied center should do nothing");
        // A denied neighbor must leave charge available for another target.
        hammer.set(FloraFaunaRegistry.HAMMER_CHARGE, 2);
        rock(p, center.east(2), Blocks.DIAMOND_ORE);
        Consumer<BreakBlockEvent> rejectWest = event -> {
            if (event.getPlayer() == p && event.getPos().equals(center.west())) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, BreakBlockEvent.class, rejectWest);
        try {
            RemoteOreStrike.hit(p, hammer, center);
        } finally {
            NeoForge.EVENT_BUS.unregister(rejectWest);
        }
        h.assertTrue(HammerItem.charge(hammer) == 0 && p.level().isEmptyBlock(center) && p.level().isEmptyBlock(center.east())
                && !p.level().isEmptyBlock(center.west()) && !p.level().isEmptyBlock(center.east(2)), "Protected block or charge limit ignored");
        RemoteOreStrike.hit(p, hammer, center.west());
        h.assertTrue(!p.level().isEmptyBlock(center.west()), "An empty Hammer mined ore");
        h.succeed();
    }

    private static void flight(GameTestHelper h, boolean creative) {
        var p = player(h, creative);
        var center = h.absolutePos(new BlockPos(8, 12, 8));
        rock(p, center, Blocks.DIAMOND_ORE);
        rock(p, center.east(), Blocks.DIAMOND_ORE);
        p.setPos(Vec3.atCenterOf(center.north(4)));
        ChargedThrowPayload.key(p, ChargedThrowPayload.PRESS);
        h.runAfterDelay(ChargedThrowPayload.HOLD_TICKS + 1, () -> {
            ChargedThrowPayload.key(p, ChargedThrowPayload.RELEASE);
            ChargedThrowPayload.key(p, ChargedThrowPayload.RELEASE);
            var entities = p.level().getEntities(FloraFaunaRegistry.THROWN_ITEM.get(), e -> e.getOwner() == p);
            h.assertTrue(entities.size() == 1 && p.getMainHandItem().isEmpty(), "The charged throw must transfer exactly one Hammer");
            var projectile = entities.getFirst();
            var save = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, p.level().registryAccess());
            projectile.saveWithoutId(save);
            projectile.discard();
            var restored = FloraFaunaRegistry.THROWN_ITEM.get().create(p.level(), EntitySpawnReason.LOAD);
            restored.load(TagValueInput.create(ProblemReporter.DISCARDING, p.level().registryAccess(), save.buildResult()));
            restored.setOwner(p);
            restored.setPos(Vec3.atCenterOf(center.north(2)));
            restored.setDeltaMovement(0, 0, 2.5);
            var held = new ItemStack(Items.STICK, 4);
            p.setItemInHand(InteractionHand.MAIN_HAND, held);
            restored.tick();
            h.assertTrue(p.level().isEmptyBlock(center) && p.level().isEmptyBlock(center.east()), "Reloaded charged throw did not strike the vein");
            h.assertTrue(HammerItem.charge(restored.getThrownItem()) == HammerItem.MAX_CHARGE - 2, "Projectile did not retain the debit");
            restored.setPos(p.position());
            restored.tick();
            h.assertTrue(restored.isRemoved() && p.getMainHandItem() == held, "Return displaced the current stack");
            int count = 0;
            for (ItemStack stack : p.getInventory().getNonEquipmentItems()) {
                if (stack.is(FloraFaunaRegistry.HAMMER.get())) {
                    h.assertTrue(HammerItem.charge(stack) == HammerItem.MAX_CHARGE - 2, "Charge changed on return");
                    count += stack.getCount();
                }
            }
            h.assertTrue(count == 1, "Expected exactly one returned Hammer, got " + count);
            h.succeed();
        });
    }

    private static void firstSurface(GameTestHelper h) {
        var p = player(h, false);
        var center = h.absolutePos(new BlockPos(8, 12, 8));
        rock(p, center, Blocks.STONE);
        rock(p, center.south(), Blocks.DIAMOND_ORE);
        p.setPos(Vec3.atCenterOf(center.north(4)));
        var original = p.getMainHandItem();
        ChargedThrowPayload.key(p, ChargedThrowPayload.PRESS);
        h.runAfterDelay(ChargedThrowPayload.HOLD_TICKS + 1, () -> {
            Consumer<EntityJoinLevelEvent> deny = event -> {
                if (event.getEntity() instanceof ThrownItemEntity e && e.getOwner() == p) event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, EntityJoinLevelEvent.class, deny);
            try {
                ChargedThrowPayload.key(p, ChargedThrowPayload.RELEASE);
            } finally {
                NeoForge.EVENT_BUS.unregister(deny);
            }
            h.assertTrue(p.getMainHandItem() == original && original.getCount() == 1 && HammerItem.charge(original) == 64,
                    "Rejected charged projectile consumed the Hammer or its charge");
            ChargedThrowPayload.key(p, ChargedThrowPayload.PRESS);
            h.runAfterDelay(ChargedThrowPayload.HOLD_TICKS + 1, () -> {
                ChargedThrowPayload.key(p, ChargedThrowPayload.RELEASE);
                var thrown = p.level().getEntities(FloraFaunaRegistry.THROWN_ITEM.get(), e -> e.getOwner() == p).getFirst();
                thrown.setPos(Vec3.atCenterOf(center.north(2)));
                thrown.setDeltaMovement(0, 0, 3.5);
                thrown.tick();
                h.assertTrue(thrown.isReturning() && HammerItem.charge(thrown.getThrownItem()) == 64
                        && p.level().getBlockState(center).is(Blocks.STONE) && p.level().getBlockState(center.south()).is(Blocks.DIAMOND_ORE),
                        "First non-ore surface must return the Hammer without spending charge");
                h.succeed();
            });
        });
    }

    private static void tap(GameTestHelper h) {
        var p = player(h, false);
        var center = h.absolutePos(new BlockPos(8, 12, 8));
        rock(p, center, Blocks.DIAMOND_ORE);
        p.setPos(Vec3.atCenterOf(center.north(4)));
        var hammer = p.getMainHandItem();
        hammer.set(FloraFaunaRegistry.HAMMER_CHARGE, 40);
        for (int tick = 1; tick <= 20; tick++) {
            p.tickCount = tick;
            hammer.getItem().inventoryTick(hammer, p.level(), p, EquipmentSlot.MAINHAND);
            hammer.getItem().inventoryTick(hammer, p.level(), p, null);
        }
        h.assertTrue(HammerItem.charge(hammer) == 41, "Held recharge must add one per second");
        ChargedThrowPayload.key(p, ChargedThrowPayload.PRESS);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FloraFaunaRegistry.HAMMER.get()));
        ChargedThrowPayload.key(p, ChargedThrowPayload.RELEASE);
        h.assertTrue(!p.getMainHandItem().isEmpty(), "Switching Hammers must cancel the windup");
        p.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        ChargedThrowPayload.key(p, ChargedThrowPayload.PRESS);
        ChargedThrowPayload.key(p, ChargedThrowPayload.RELEASE);
        var thrown = p.level().getEntities(FloraFaunaRegistry.THROWN_ITEM.get(), e -> e.getOwner() == p).getFirst();
        thrown.setPos(Vec3.atCenterOf(center.north(2)));
        thrown.setDeltaMovement(0, 0, 2.5);
        thrown.tick();
        h.assertTrue(!p.level().isEmptyBlock(center) && HammerItem.charge(thrown.getThrownItem()) == 41, "A tap must be a normal throw");
        h.succeed();
    }
}