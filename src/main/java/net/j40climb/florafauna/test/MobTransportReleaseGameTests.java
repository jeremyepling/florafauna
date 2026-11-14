package net.j40climb.florafauna.test;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.common.block.mobtransport.CapturedMobTicket;
import net.j40climb.florafauna.common.block.mobtransport.MobInputBlockEntity;
import net.j40climb.florafauna.common.block.mobtransport.MobOutputBlockEntity;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static net.j40climb.florafauna.test.FloraFaunaGameTests.createTestPlayer;
import static net.j40climb.florafauna.test.FloraFaunaGameTests.registerTest;

final class MobTransportReleaseGameTests {
    private static final BlockPos CENTER = new BlockPos(1, 1, 1);

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env) {
        registerTest(event, env, "mob_output_retries_canceled_spawn", MobTransportReleaseGameTests::regularRetry);
        registerTest(event, env, "mob_output_retains_failed_release_during_break", h -> breakRetry(h, false));
        registerTest(event, env, "mob_input_retains_failed_release_during_break", h -> breakRetry(h, true));
        registerTest(event, env, "mob_output_explosion_release_retry", h -> explosionRetry(h, false));
        registerTest(event, env, "mob_input_explosion_release_retry", h -> explosionRetry(h, true));
    }

    private static CapturedMobTicket cow(GameTestHelper h, UUID id, long readyAt) {
        var level = h.getLevel();
        var cow = EntityTypes.COW.create(level, EntitySpawnReason.TRIGGERED);
        cow.setUUID(id);
        cow.setCustomName(Component.literal("Passenger"));
        var tag = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        cow.saveWithoutId(tag);
        return CapturedMobTicket.create(EntityTypes.COW, tag.buildResult(), 0, readyAt, h.absolutePos(CENTER), level.dimension());
    }

    private static Consumer<EntityJoinLevelEvent> cancelPassenger(GameTestHelper h, UUID id) {
        Consumer<EntityJoinLevelEvent> cancel = event -> {
            if (event.getLevel() == h.getLevel() && id.equals(event.getEntity().getUUID())) {
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, EntityJoinLevelEvent.class, cancel);
        return cancel;
    }

    private static void regularRetry(GameTestHelper h) {
        h.setBlock(CENTER, FloraFaunaRegistry.MOB_OUTPUT.get());
        var level = h.getLevel();
        var output = h.getBlockEntity(CENTER, MobOutputBlockEntity.class);
        UUID id = UUID.randomUUID();
        output.acceptTicket(cow(h, id, level.getGameTime()));
        var listener = cancelPassenger(h, id);
        try {
            output.tick(level, output.getBlockPos(), output.getBlockState());
            require(h, output.getPendingReleaseCount() == 1 && level.getEntity(id) == null,
                    "Rejected spawn removed the ticket");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        for (int i = 0; i < Config.releaseCheckIntervalTicks; i++) {
            output.tick(level, output.getBlockPos(), output.getBlockState());
        }
        var released = level.getEntity(id);
        require(h, output.getPendingReleaseCount() == 0 && released != null
                && Component.literal("Passenger").equals(released.getCustomName()), "Retry lost the original passenger");
        h.succeed();
    }

    private static void breakRetry(GameTestHelper h, boolean breakInput) {
        h.setBlock(CENTER, FloraFaunaRegistry.MOB_INPUT.get());
        h.setBlock(CENTER.east(), FloraFaunaRegistry.MOB_OUTPUT.get());
        var level = h.getLevel();
        var input = h.getBlockEntity(CENTER, MobInputBlockEntity.class);
        var output = h.getBlockEntity(CENTER.east(), MobOutputBlockEntity.class);
        input.pairWithOutput(output.getBlockPos());
        var transport = breakInput ? input : output;
        var player = createTestPlayer(h);
        player.setGameMode(GameType.SURVIVAL);
        UUID first = UUID.randomUUID();
        UUID blocked = UUID.randomUUID();
        for (UUID id : new UUID[]{first, blocked}) {
            var ticket = cow(h, id, Long.MAX_VALUE);
            if (breakInput) {
                input.getBuffer().add(ticket);
            } else {
                output.acceptTicket(ticket);
            }
        }
        var listener = cancelPassenger(h, blocked);
        try {
            player.gameMode.destroyBlock(transport.getBlockPos());
            require(h, level.getBlockEntity(transport.getBlockPos()) == transport, "Removed the block holding a rejected spawn");
            int waiting = breakInput ? input.getBuffer().size() : output.getPendingReleaseCount();
            require(h, waiting == 1 && level.getEntity(first) != null && level.getEntity(blocked) == null,
                    "Partial release lost a ticket or kept a completed ticket");
            require(h, input.isPaired() && output.getInputCount() == 1, "Failed break canceled a pairing");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        var firstEntity = level.getEntity(first);
        player.gameMode.destroyBlock(transport.getBlockPos());
        require(h, level.isEmptyBlock(transport.getBlockPos()) && level.getEntity(first) == firstEntity
                && level.getEntity(blocked) != null, "Retry failed to remove the block and release exactly the remaining passenger");
        require(h, !input.isPaired() && output.getInputCount() == 0, "Successful break left a pairing");
        h.succeed();
    }

    private static void explosionRetry(GameTestHelper h, boolean hitInput) {
        h.setBlock(CENTER, FloraFaunaRegistry.MOB_INPUT.get());
        h.setBlock(CENTER.east(), FloraFaunaRegistry.MOB_OUTPUT.get());
        var level = h.getLevel();
        var input = h.getBlockEntity(CENTER, MobInputBlockEntity.class);
        var output = h.getBlockEntity(CENTER.east(), MobOutputBlockEntity.class);
        input.pairWithOutput(output.getBlockPos());
        var target = hitInput ? input : output;
        BlockPos pos = target.getBlockPos();
        UUID first = UUID.randomUUID();
        UUID blocked = UUID.randomUUID();
        for (UUID id : new UUID[]{first, blocked}) {
            var ticket = cow(h, id, Long.MAX_VALUE);
            if (hitInput) input.getBuffer().add(ticket);
            else output.acceptTicket(ticket);
        }

        List<ItemStack> drops = new ArrayList<>();
        explosionHit(h, pos, Explosion.BlockInteraction.TRIGGER_BLOCK, drops);
        require(h, level.getBlockEntity(pos) == target && drops.isEmpty() && level.getEntity(first) == null
                && level.getEntity(blocked) == null && input.isPaired(), "Trigger-only blast disturbed transport");

        var veto = cancelPassenger(h, blocked);
        try {
            explosionHit(h, pos, Explosion.BlockInteraction.DESTROY_WITH_DECAY, drops);
            int waiting = hitInput ? input.getBuffer().size() : output.getPendingReleaseCount();
            require(h, level.getBlockEntity(pos) == target && waiting == 1 && drops.isEmpty()
                    && level.getEntity(first) != null && level.getEntity(blocked) == null && input.isPaired(),
                    "Destructive blast lost custody, pairing or emitted loot after a rejected spawn");
        } finally {
            NeoForge.EVENT_BUS.unregister(veto);
        }

        var firstEntity = level.getEntity(first);
        var expectedItem = target.getBlockState().getBlock().asItem();
        explosionHit(h, pos, Explosion.BlockInteraction.DESTROY, drops);
        require(h, level.isEmptyBlock(pos) && drops.size() == 1 && drops.getFirst().is(expectedItem)
                && drops.getFirst().getCount() == 1 && level.getEntity(first) == firstEntity && level.getEntity(blocked) != null,
                "Blast retry failed to release the remaining mob and drop one transport block");
        require(h, !input.isPaired() && output.getInputCount() == 0, "Blast left a stale pairing");
        h.succeed();
    }

    private static void explosionHit(GameTestHelper h, BlockPos pos, Explosion.BlockInteraction mode, List<ItemStack> drops) {
        var level = h.getLevel();
        var blast = new ServerExplosion(level, null, null, null, Vec3.atCenterOf(pos), 1, false, mode);
        level.getBlockState(pos).onExplosionHit(level, pos, blast, (stack, at) -> drops.add(stack));
    }

    private static void require(GameTestHelper h, boolean condition, String message) {
        if (!condition) throw h.assertionException(message);
    }
}
