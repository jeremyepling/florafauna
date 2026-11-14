package net.j40climb.florafauna.common.entity.frenchie;

import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/** Handles the visible farewell and the handoff. CourierTrips owns dogs between those two events. */
final class DeliveryGoal extends Goal {
    private static final int WALK_OFF = 20;
    private static final int MAX_DEPARTURE_TICKS = 80;
    private final FrenchieEntity dog;
    private @Nullable GlobalPos home;
    private @Nullable UUID recipient;
    private ItemStack parcel = ItemStack.EMPTY;
    private boolean returning;
    private boolean atDestination;
    private boolean announced;
    private @Nullable BlockPos departure;
    private int departureTicks;

    DeliveryGoal(FrenchieEntity dog) {
        this.dog = dog;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Nullable GlobalPos home() {
        return home;
    }

    void assignBed(BlockPos head) {
        home = GlobalPos.of(dog.level().dimension(), head.immutable());
    }

    static boolean isBedHead(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        var state = level.getBlockState(pos);
        return state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.HEAD;
    }

    static int travelSeconds(ServerLevel from, Vec3 start, ServerLevel to, Vec3 end) {
        double scale = DimensionType.getTeleportationScale(to.dimensionType(), from.dimensionType());
        double distance = start.distanceTo(new Vec3(end.x * scale, end.y, end.z * scale));
        return 3 + (int) Math.ceil(distance / 16);
    }

    void start(ServerPlayer owner, InteractionHand hand, @Nullable ServerPlayer target) {
        parcel = owner.getItemInHand(hand).split(1);
        recipient = target == null ? null : target.getUUID();
        returning = false;
        atDestination = false;
        announced = false;
        beginDeparture(owner.level());
        dog.standUp();
        dog.setFrenchieSleeping(false);
        dog.getNavigation().stop();
        tell(owner, "started", target == null ? label("home", home.pos().toShortString()) : target.getName());
    }

    private void beginDeparture(ServerLevel level) {
        departure = dog.blockPosition();
        departureTicks = 0;
        // Only the brief send-off needs the dog to tick if the sender immediately leaves.
        level.getChunkSource().addTicketWithRadius(FloraFaunaRegistry.COURIER_STOP.get(), dog.chunkPosition(), 2);
    }

    void collect(ServerPlayer owner, InteractionHand emptyHand) {
        boolean delivered = !returning && recipient == null && atDestination;
        owner.setItemInHand(emptyHand, parcel);
        parcel = ItemStack.EMPTY;
        dog.getNavigation().stop();
        dog.sitDown();
        tell(owner, delivered ? "collected" : "returned");
    }

    void dropOnDeath(ServerLevel level) {
        if (canUse()) dog.spawnAtLocation(level, parcel);
        parcel = ItemStack.EMPTY;
    }

    @Override
    public boolean canUse() {
        return !parcel.isEmpty();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (!canUse()) return;
        ServerLevel level = (ServerLevel) dog.level();
        var server = level.getServer();
        var ownerId = dog.getOwnerReference();
        if (ownerId == null) return;
        var owner = server.getPlayerList().getPlayer(ownerId.getUUID());
        var player = recipient == null ? null : server.getPlayerList().getPlayer(recipient);
        if (returning && !atDestination) {
            sendBack(owner, null);
            return;
        }
        if (!returning && recipient != null && !available(player)) {
            sendBack(owner, "unavailable");
            return;
        }
        if (atDestination) {
            if (dog.tickCount % 10 == 0) handOff(level, owner, player);
            return;
        }
        ServerLevel destinationLevel = player != null ? player.level() : home == null ? null : server.getLevel(home.dimension());
        if (destinationLevel == null) {
            sendBack(owner, "bed_missing");
            return;
        }
        if (departure == null || dog.position().distanceToSqr(departure.getBottomCenter()) > 24 * 24) {
            beginDeparture(level); // Legacy order or the courier was carried elsewhere.
        }
        Vec3 end = player != null ? player.position() : home.pos().getBottomCenter();
        Vec3 start = departure.getBottomCenter();
        double scale = DimensionType.getTeleportationScale(destinationLevel.dimensionType(), level.dimensionType());
        Vec3 direction = new Vec3(end.x * scale - start.x, 0, end.z * scale - start.z);
        if (direction.lengthSqr() < 1) direction = dog.getForward().multiply(1, 0, 1);
        double walk = Math.min(WALK_OFF, Math.max(2, direction.length()));
        if (++departureTicks >= MAX_DEPARTURE_TICKS || (departureTicks >= 10 && dog.position().distanceTo(start) >= walk - 0.5)) {
            int seconds = travelSeconds(level, start, destinationLevel, end);
            CourierTrips.depart(dog, recipient, home, false, seconds);
            return;
        }
        if (departureTicks == 1 || departureTicks % 20 == 0) {
            Vec3 waypoint = start.add(direction.normalize().scale(walk));
            dog.getNavigation().moveTo(dog.getNavigation().createPath(BlockPos.containing(waypoint), 1), 1.2);
        }
    }

    static boolean available(@Nullable ServerPlayer p) {
        return p != null && p.isAlive() && !p.isSpectator();
    }

    void arrived(boolean back) {
        returning = back;
        atDestination = true;
        announced = false;
        departure = null;
    }

    private void handOff(ServerLevel level, @Nullable ServerPlayer owner, @Nullable ServerPlayer player) {
        if (returning) {
            if (!announced) tell(owner, "recover");
        } else if (recipient == null) {
            if (home == null || home.dimension() != level.dimension() || !isBedHead(level, home.pos())) {
                sendBack(owner, "bed_missing");
                return;
            }
            if (!announced) tell(owner, "at_bed", home.pos().toShortString());
        } else if (player != null) {
            if (player.level() != level || dog.distanceToSqr(player) > 36 || !dog.hasLineOfSight(player)) {
                // They moved while making room. Use another timed hop to their current location.
                CourierTrips.depart(dog, recipient, home, false,
                        travelSeconds(level, dog.position(), player.level(), player.position()));
                return;
            }
            int slot = player.getInventory().getFreeSlot();
            if (slot >= 0) {
                player.getInventory().setItem(slot, parcel);
                parcel = ItemStack.EMPTY;
                tell(player, "received", dog.getName());
                tell(owner, "complete", player.getName());
                sendBack(owner, null);
                return;
            }
            if (!announced) {
                tell(player, "need_slot", dog.getName());
                tell(owner, "full", player.getName());
            }
        }
        announced = true;
    }

    private void sendBack(@Nullable ServerPlayer owner, @Nullable String message) {
        if (message != null) tell(owner, message);
        var level = (ServerLevel) dog.level();
        int seconds = owner == null ? 3 : travelSeconds(level, dog.position(), owner.level(), owner.position());
        CourierTrips.depart(dog, null, null, true, seconds);
    }

    void save(ValueOutput output) {
        if (home != null) output.store("FrenchieHomeBed", GlobalPos.CODEC, home);
        if (!canUse()) return;
        output.store("FrenchieParcel", ItemStack.CODEC, parcel);
        if (recipient != null) output.store("FrenchieRecipient", UUIDUtil.CODEC, recipient);
        output.putBoolean("FrenchieReturning", returning);
        output.putBoolean("FrenchieAtHandoff", atDestination);
        if (departure != null) output.store("FrenchieDeparture", BlockPos.CODEC, departure);
        output.putInt("FrenchieDepartureTicks", departureTicks);
    }

    void load(ValueInput input) {
        home = input.read("FrenchieHomeBed", GlobalPos.CODEC).orElse(null);
        parcel = input.read("FrenchieParcel", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        recipient = input.read("FrenchieRecipient", UUIDUtil.CODEC).orElse(null);
        returning = input.getBooleanOr("FrenchieReturning", false);
        atDestination = input.getBooleanOr("FrenchieAtHandoff", false);
        departure = input.read("FrenchieDeparture", BlockPos.CODEC).orElse(null);
        departureTicks = input.getIntOr("FrenchieDepartureTicks", 0);
    }

    static Component label(String key, Object... args) {
        return Component.translatable("frenchie.florafauna.delivery." + key, args);
    }

    static void tell(@Nullable ServerPlayer player, String key, Object... args) {
        if (player != null) player.sendOverlayMessage(label(key, args));
    }
}
