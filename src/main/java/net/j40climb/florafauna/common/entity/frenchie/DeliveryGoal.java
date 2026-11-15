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
    private static final int REMINDER_TICKS = 20 * 30;
    private final FrenchieEntity dog;
    private @Nullable GlobalPos home;
    private @Nullable UUID recipient;
    private ItemStack parcel = ItemStack.EMPTY;
    private boolean goingHome;
    private boolean returning;
    private boolean atDestination;
    private boolean announced;
    private long nextReminder;
    private @Nullable BlockPos departure;
    private int departureTicks;
    private int plannedSeconds;

    DeliveryGoal(FrenchieEntity dog) {
        this.dog = dog;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    boolean goingHome() {
        return goingHome;
    }

    boolean hasParcel() {
        return !parcel.isEmpty();
    }

    @Nullable UUID waitingRecipient() {
        return atDestination && !returning && hasParcel() ? recipient : null;
    }

    static boolean isBedHead(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        var state = level.getBlockState(pos);
        return state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.HEAD;
    }

    static int travelSeconds(ServerLevel from, Vec3 start, ServerLevel to, Vec3 end) {
        if (from != to) {
            double fromScale = from.dimensionType().coordinateScale();
            double toScale = to.dimensionType().coordinateScale();
            start = start.multiply(fromScale, 1, fromScale);
            end = end.multiply(toScale, 1, toScale);
        }
        return (int) (3 + Math.ceil(start.distanceTo(end) / 16));
    }

    void start(ServerPlayer owner, InteractionHand hand, @Nullable ServerPlayer target, @Nullable GlobalPos bed) {
        beginOrder(owner, owner.getItemInHand(hand).split(1), target, bed);
    }

    boolean startHome(ServerPlayer owner, GlobalPos bed) {
        if (!dog.isAlive() || !dog.isOwnedBy(owner) || dog.hasCompanionJob()) return false;
        var level = owner.level().getServer().getLevel(bed.dimension());
        if (level == null || (level.hasChunkAt(bed.pos()) && !isBedHead(level, bed.pos()))) return false;
        beginOrder(owner, ItemStack.EMPTY, null, bed);
        return true;
    }

    private void beginOrder(ServerPlayer owner, ItemStack bundle, @Nullable ServerPlayer target, @Nullable GlobalPos bed) {
        parcel = bundle;
        goingHome = bundle.isEmpty();
        home = bed;
        recipient = target == null ? null : target.getUUID();
        returning = false;
        atDestination = false;
        announced = false;
        nextReminder = 0;
        var level = (ServerLevel) dog.level();
        var destination = target != null ? target.level() : level.getServer().getLevel(bed.dimension());
        var end = target != null ? target.position() : Vec3.atBottomCenterOf(bed.pos());
        plannedSeconds = CourierTrips.plannedSeconds(level, dog.position(), destination, end);
        beginDeparture(level);
        dog.standUp();
        dog.setFrenchieSleeping(false);
        dog.getNavigation().stop();
        dog.syncCompanionJob();
        CourierTrips.track(dog);
        if (goingHome) tell(owner, "home_started", dog.getName());
        else tell(owner, "started", target == null ? label("home") : target.getName());
    }

    private void beginDeparture(ServerLevel level) {
        departure = dog.blockPosition();
        departureTicks = 0;
        // Only the brief send-off needs the dog to tick if the sender immediately leaves.
        level.getChunkSource().addTicketWithRadius(FloraFaunaRegistry.COURIER_STOP.get(), dog.chunkPosition(), 2);
    }

    void collect(ServerPlayer owner, InteractionHand emptyHand) {
        boolean delivered = !returning && recipient == null && atDestination;
        boolean homeOrder = goingHome;
        CourierTrips.forget(dog);
        if (hasParcel()) owner.setItemInHand(emptyHand, parcel);
        parcel = ItemStack.EMPTY;
        goingHome = false;
        dog.getNavigation().stop();
        dog.sitDown();
        tell(owner, homeOrder ? "home_stopped" : delivered ? "collected" : "returned");
    }

    boolean recall(ServerPlayer owner) {
        if (!canUse() || !dog.isAlive() || !dog.isOwnedBy(owner)) return false;
        if (returning && atDestination && dog.level() == owner.level() && owner.isWithinEntityInteractionRange(dog, 0)) {
            tell(owner, "recover");
            return true;
        }
        CourierTrips.depart(dog, null, null, true, 0);
        return true;
    }

    void dropOnDeath(ServerLevel level) {
        if (canUse()) CourierTrips.forget(dog);
        if (hasParcel()) dog.spawnAtLocation(level, parcel);
        parcel = ItemStack.EMPTY;
    }

    @Override
    public boolean canUse() {
        return hasParcel() || goingHome;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (!canUse()) return;
        CourierTrips.track(dog);
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
            if (!announced || dog.tickCount % 10 == 0) handOff(level, owner, player);
            return;
        }
        ServerLevel destinationLevel = player != null ? player.level() : home == null ? null : server.getLevel(home.dimension());
        if (destinationLevel == null) {
            sendBack(owner, goingHome ? "home_missing" : "bed_missing");
            return;
        }
        if (departure == null || dog.position().distanceToSqr(Vec3.atBottomCenterOf(departure)) > 24 * 24) {
            beginDeparture(level); // Legacy order or the courier was carried elsewhere.
        }
        Vec3 end = player != null ? player.position() : Vec3.atBottomCenterOf(home.pos());
        Vec3 start = Vec3.atBottomCenterOf(departure);
        double scale = DimensionType.getTeleportationScale(destinationLevel.dimensionType(), level.dimensionType());
        Vec3 direction = new Vec3(end.x * scale - start.x, 0, end.z * scale - start.z);
        if (direction.lengthSqr() < 1) direction = dog.getForward().multiply(1, 0, 1);
        double walk = Math.min(WALK_OFF, Math.max(2, direction.length()));
        if (++departureTicks >= MAX_DEPARTURE_TICKS || (departureTicks >= 10 && dog.position().distanceTo(start) >= walk - 0.5)) {
            long remainingTicks = plannedSeconds > 0 ? Math.max(0, 20L * plannedSeconds - departureTicks)
                    : 20L * travelSeconds(level, start, destinationLevel, end);
            CourierTrips.departAfterTicks(dog, recipient, home, false, remainingTicks);
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
        if (goingHome) {
            if (back) {
                goingHome = false;
                dog.standUp();
            } else {
                dog.sitDown();
            }
        }
        dog.syncCompanionJob();
    }

    private void handOff(ServerLevel level, @Nullable ServerPlayer owner, @Nullable ServerPlayer player) {
        if (returning) {
            if (!announced) tell(owner, "recover");
        } else if (recipient == null) {
            var bedLevel = home == null ? null : level.getServer().getLevel(home.dimension());
            if (bedLevel == null) {
                sendBack(owner, goingHome ? "home_missing" : "bed_missing");
                return;
            }
            if (bedLevel != level || dog.position().distanceToSqr(Vec3.atBottomCenterOf(home.pos())) > 16) {
                // A waiting courier can be picked up and released somewhere else.
                CourierTrips.depart(dog, null, home, false, travelSeconds(level, dog.position(), bedLevel, Vec3.atBottomCenterOf(home.pos())));
                return;
            }
            if (!isBedHead(level, home.pos())) {
                sendBack(owner, goingHome ? "home_missing" : "bed_missing");
                return;
            }
            if (!announced) tell(owner, goingHome ? "home_waiting" : "at_bed");
        } else if (player != null) {
            if (player.level() != level || dog.distanceToSqr(player) > 36 || !dog.hasLineOfSight(player)) {
                // They moved while making room. Use another timed hop to their current location.
                CourierTrips.depart(dog, recipient, home, false,
                        travelSeconds(level, dog.position(), player.level(), player.position()));
                return;
            }
            dog.getLookControl().setLookAt(player, 30, 30);
            if (level.getGameTime() >= nextReminder) announce(player);
            // A disconnect or teleport must still wake the courier on its next handoff check.
            level.getChunkSource().addTicketWithRadius(FloraFaunaRegistry.COURIER_STOP.get(), dog.chunkPosition(), 2);
        }
        announced = true;
    }

    private void announce(ServerPlayer recipient) {
        FrenchieAwareness.bark(dog, 0.95F);
        tell(recipient, "accept", dog.getName());
        nextReminder = dog.level().getGameTime() + REMINDER_TICKS;
    }

    void accept(ServerPlayer player) {
        if (!player.getUUID().equals(waitingRecipient()) || !available(player)
                || dog.level() != player.level() || !player.isWithinEntityInteractionRange(dog, 0)) return;
        if (dog.level().getGameTime() >= nextReminder) announce(player);
        int slot = player.getInventory().getFreeSlot();
        if (slot < 0) {
            tell(player, "need_slot", dog.getName());
            return;
        }
        player.getInventory().setItem(slot, parcel);
        parcel = ItemStack.EMPTY;
        tell(player, "received", dog.getName());
        var owner = player.level().getServer().getPlayerList().getPlayer(dog.getOwnerReference().getUUID());
        tell(owner, "complete", player.getName());
        sendBack(owner, null);
    }

    private void sendBack(@Nullable ServerPlayer owner, @Nullable String message) {
        if (message != null) tell(owner, message);
        var level = (ServerLevel) dog.level();
        int seconds = owner == null ? 3 : travelSeconds(level, dog.position(), owner.level(), owner.position());
        CourierTrips.depart(dog, null, null, true, seconds);
    }

    void save(ValueOutput output) {
        if (!canUse()) return;
        if (home != null) output.store("FrenchieHomeBed", GlobalPos.CODEC, home);
        if (hasParcel()) output.store("FrenchieParcel", ItemStack.CODEC, parcel);
        if (goingHome) output.putBoolean("FrenchieGoingHome", true);
        if (recipient != null) output.store("FrenchieRecipient", UUIDUtil.CODEC, recipient);
        output.putBoolean("FrenchieReturning", returning);
        output.putBoolean("FrenchieAtHandoff", atDestination);
        if (departure != null) output.store("FrenchieDeparture", BlockPos.CODEC, departure);
        output.putInt("FrenchieDepartureTicks", departureTicks);
        output.putInt("FrenchiePlannedSeconds", plannedSeconds);
        output.putLong("FrenchieNextParcelReminder", nextReminder);
    }

    void load(ValueInput input) {
        home = input.read("FrenchieHomeBed", GlobalPos.CODEC).orElse(null);
        parcel = input.read("FrenchieParcel", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        goingHome = parcel.isEmpty() && input.getBooleanOr("FrenchieGoingHome", false);
        recipient = input.read("FrenchieRecipient", UUIDUtil.CODEC).orElse(null);
        returning = input.getBooleanOr("FrenchieReturning", false);
        atDestination = input.getBooleanOr("FrenchieAtHandoff", false);
        departure = input.read("FrenchieDeparture", BlockPos.CODEC).orElse(null);
        departureTicks = input.getIntOr("FrenchieDepartureTicks", 0);
        plannedSeconds = input.getIntOr("FrenchiePlannedSeconds", 0);
        nextReminder = input.getLongOr("FrenchieNextParcelReminder", 0);
    }

    static Component label(String key, Object... args) {
        return Component.translatable("frenchie.florafauna.delivery." + key, args);
    }

    static void tell(@Nullable ServerPlayer player, String key, Object... args) {
        if (player != null) player.sendOverlayMessage(label(key, args));
    }
}
