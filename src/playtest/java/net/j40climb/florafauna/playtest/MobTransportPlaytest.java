package net.j40climb.florafauna.playtest;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.common.block.mobtransport.CapturedMobTicket;
import net.j40climb.florafauna.common.block.mobtransport.MobInputBlockEntity;
import net.j40climb.florafauna.common.block.mobtransport.MobInputLinkingState;
import net.j40climb.florafauna.common.block.mobtransport.MobOutputBlockEntity;
import net.j40climb.florafauna.common.mobsymbiote.MobSymbioteHelper;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.Minecraft;
import net.j40climb.florafauna.dev.DevPlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
//? if <26.3 {
import net.minecraft.world.entity.EntityType;
//?} else {
/*import net.minecraft.world.entity.EntityTypes;*/
//?}
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static net.j40climb.florafauna.playtest.Playtest.require;

final class MobTransportPlaytest {
    private static final BlockPos IN = new BlockPos(-2, 200, 0);
    private static final BlockPos OUT = new BlockPos(2, 200, 0);
    private static final UUID COW_ID = UUID.fromString("1112b5a1-78ba-4997-8ec9-533486d6f289");
    private static final String NAME = "Clover the Courier";
    private static final Input CROUCH = new Input(false, false, false, false, false, true, false);

    static void steps(Playtest t, int phase) {
        if (phase == 1) {
            t.server("Prepare a survival platform with transport blocks and one symbiote", p -> {
                DevPlot.TRANSPORT.place(p.level(), new BlockPos(-5, 199, -7));
                p.teleportTo(0.5, 200, 2.5);
                p.setGameMode(GameType.SURVIVAL);
                p.getInventory().clearContent();
                p.getInventory().setItem(0, new ItemStack(FloraFaunaRegistry.MOB_INPUT.get()));
                p.getInventory().setItem(1, new ItemStack(FloraFaunaRegistry.MOB_OUTPUT.get()));
                p.getInventory().setItem(2, new ItemStack(FloraFaunaRegistry.MOB_SYMBIOTE.get()));
                p.inventoryMenu.broadcastChanges();
            });
            t.awaitClient("Receive both transport blocks and symbiote in the hotbar", mc ->
                    mc.level.getBlockState(IN.below()).is(Blocks.SMOOTH_STONE)
                            && mc.player.getInventory().getItem(0).is(FloraFaunaRegistry.MOB_INPUT.get().asItem())
                            && mc.player.getInventory().getItem(1).is(FloraFaunaRegistry.MOB_OUTPUT.get().asItem())
                            && mc.player.getInventory().getItem(2).is(FloraFaunaRegistry.MOB_SYMBIOTE.get()));
            t.client("Place the Mob Input", mc -> { select(mc, 0); useOnTop(mc, IN.below()); });
            t.client("Place the Mob Output", mc -> { select(mc, 1); useOnTop(mc, OUT.below()); });
            t.awaitServer("Receive both placed transport blocks", p -> p.level().getBlockEntity(IN) instanceof MobInputBlockEntity
                    && p.level().getBlockEntity(OUT) instanceof MobOutputBlockEntity);
            t.server("Spawn one identifiable unbonded cow beside the input", p -> {
                Cow cow = EntityType.COW.create(p.level(), EntitySpawnReason.COMMAND);
                require(cow != null, "Failed to create the cow");
                cow.setUUID(COW_ID);
                cow.setCustomName(Component.literal(NAME));
                cow.setPersistenceRequired();
                cow.setNoAi(true); // Stay in capture range throughout bonding and linking.
                cow.snapTo(-0.45, 200, 0.5, 0, 0);
                require(p.level().addFreshEntity(cow), "Could not spawn the fixture cow");
                require(!MobSymbioteHelper.hasMobSymbiote(cow), "Fixture cow was already bonded");
            });
            t.awaitClient("See the named cow", mc -> mc.level.getEntity(COW_ID) instanceof Cow);
            t.client("Right-click the cow with the Mob Symbiote", mc -> {
                select(mc, 2);
                var cow = mc.level.getEntity(COW_ID);
                require(mc.gameMode.interact(mc.player, cow, new EntityHitResult(cow, cow.getBoundingBox().getCenter()),
                        InteractionHand.MAIN_HAND).consumesAction(), "Bonding interaction was rejected");
            });
            t.awaitServer("Confirm the bond and survival item consumption", p -> p.level().getEntity(COW_ID) instanceof Cow cow
                    && MobSymbioteHelper.getMobSymbioteLevel(cow) == 1 && p.getInventory().countItem(FloraFaunaRegistry.MOB_SYMBIOTE.get()) == 0);
            link(t);
            t.awaitServer("Capture exactly one cow and remove it from the world", p ->
                    input(p).getBuffer().size() == 1 && p.level().getEntity(COW_ID) == null);
            // A normal unpair pauses transport regardless of how long saving and rejoining take.
            t.client("Shift-use the paired input to hold its queued mob across shutdown", MobTransportPlaytest::shiftUseInput);
            t.awaitServer("Observe the unpaired input with its captured ticket intact", p -> !input(p).isPaired() && output(p).getInputCount() == 0);
            t.server("Verify the original cow's stored identity before quitting", MobTransportPlaytest::verifyQueuedCow);
            return;
        }

        long[] lastStoredTick = new long[1];
        long[] firstReleasedTick = new long[1];
        t.server("Verify the same stored cow and paused route after relaunch", p -> {
            require(!input(p).isPaired() && output(p).getInputCount() == 0, "Paused route was not saved");
            verifyQueuedCow(p);
            lastStoredTick[0] = p.level().getGameTime();
        });
        link(t);
        t.awaitServer("Observe the original cow emerge from Mob Output", p -> {
            if (p.level().getEntity(COW_ID) instanceof Cow) {
                firstReleasedTick[0] = p.level().getGameTime();
                return true;
            }
            lastStoredTick[0] = p.level().getGameTime();
            return false;
        });
        t.server("Verify released identity, name, bond and a single release", p -> {
            Cow cow = (Cow) p.level().getEntity(COW_ID);
            require(cow.blockPosition().equals(OUT.above()), "Cow arrived at the wrong output: " + cow.blockPosition());
            require(cow.hasCustomName() && NAME.equals(cow.getCustomName().getString()), "Cow lost its custom name");
            require(MobSymbioteHelper.getMobSymbioteLevel(cow) == 1, "Released cow lost its symbiote");
            require(MobSymbioteHelper.hasReleaseImmunity(cow, p.level().getGameTime()), "Released cow has no capture immunity");
            long until = cow.getData(FloraFaunaRegistry.MOB_SYMBIOTE_DATA).recentlyReleasedUntil();
            long duration = Config.recentlyReleasedImmunityTicks;
            require(until >= lastStoredTick[0] + duration && until <= firstReleasedTick[0] + duration,
                    "Immunity expires at " + until + "; cow was released between ticks " + lastStoredTick[0]
                            + " and " + firstReleasedTick[0] + ", configured duration " + duration);
            verifyOnlyReleasedCow(p);
        });
        long[] settleUntil = new long[1];
        t.server("Wait two more output checks for a duplicate release", p ->
                settleUntil[0] = p.level().getGameTime() + 2L * Config.releaseCheckIntervalTicks);
        t.awaitServer("Allow the route to tick", p -> p.level().getGameTime() >= settleUntil[0]);
        t.server("Verify one surviving cow and empty queues after further output checks", MobTransportPlaytest::verifyOnlyReleasedCow);
        t.awaitServer("Observe immunity expire on the live cow", p ->
                p.level().getEntity(COW_ID) instanceof Cow cow && !MobSymbioteHelper.hasReleaseImmunity(cow, p.level().getGameTime()));
    }

    private static void link(Playtest t) {
        t.awaitClient("See the input, output, and an empty linking hand", mc ->
                mc.level.getBlockState(IN).is(FloraFaunaRegistry.MOB_INPUT.get())
                        && mc.level.getBlockState(OUT).is(FloraFaunaRegistry.MOB_OUTPUT.get())
                        && mc.player.getInventory().getItem(0).isEmpty());
        t.client("Shift-use the input with an empty hand to begin linking", MobTransportPlaytest::shiftUseInput);
        t.awaitServer("Enter linking mode for this input", p -> IN.equals(MobInputLinkingState.getLinkingFrom(p)));
        t.client("Right-click the output to complete the route", mc -> useOnTop(mc, OUT));
        t.awaitServer("Confirm both ends of the route", p -> OUT.equals(input(p).getPairedOutputPos())
                && output(p).getPairedInputs().equals(Set.of(IN)) && !MobInputLinkingState.isLinking(p));
    }

    private static void shiftUseInput(Minecraft mc) {
        select(mc, 0);
        mc.player.connection.send(new ServerboundPlayerInputPacket(CROUCH));
        useOnTop(mc, IN);
        mc.player.connection.send(new ServerboundPlayerInputPacket(Input.EMPTY));
    }

    private static void select(Minecraft mc, int slot) {
        mc.player.getInventory().setSelectedSlot(slot);
        mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
    }

    private static void useOnTop(Minecraft mc, BlockPos pos) {
        BlockHitResult hit = new BlockHitResult(Vec3.atBottomCenterOf(pos.above()), Direction.UP, pos, false);
        require(mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit).consumesAction(), "Block interaction rejected at " + pos);
    }

    private static void verifyQueuedCow(ServerPlayer p) {
        List<CapturedMobTicket> tickets = input(p).getBuffer().getTickets();
        require(tickets.size() == 1 && output(p).getPendingReleaseCount() == 0 && p.level().getEntity(COW_ID) == null,
                "Expected one stored cow and no live or pending copy");
        CapturedMobTicket ticket = tickets.getFirst();
        require(ticket.entityTypeId().toString().equals("minecraft:cow") && ticket.entityNbt().read("UUID", UUIDUtil.CODEC).filter(COW_ID::equals).isPresent(),
                "Ticket lost the original cow's identity");
        require(OUT.equals(ticket.destinationPos()) && p.level().dimension().equals(ticket.destinationDim()), "Ticket lost its destination");
    }

    private static void verifyOnlyReleasedCow(ServerPlayer p) {
        List<Cow> matching = p.level().getEntitiesOfClass(Cow.class, new AABB(OUT).inflate(16),
                c -> c.getUUID().equals(COW_ID) || c.hasCustomName() && NAME.equals(c.getCustomName().getString()));
        require(matching.size() == 1 && matching.getFirst().getUUID().equals(COW_ID), "Expected exactly one original cow, got " + matching);
        require(input(p).getBuffer().isEmpty() && output(p).getPendingReleaseCount() == 0, "A duplicate ticket remained after release");
    }

    private static MobInputBlockEntity input(ServerPlayer p) {
        return (MobInputBlockEntity) p.level().getBlockEntity(IN);
    }

    private static MobOutputBlockEntity output(ServerPlayer p) {
        return (MobOutputBlockEntity) p.level().getBlockEntity(OUT);
    }
}
