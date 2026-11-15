package net.j40climb.florafauna.common.entity.frenchie;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** The world's only copy of each traveling dog, including the parcel inside its entity save. */
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public final class CourierTrips extends SavedData {
    private record Trip(CompoundTag dog, UUID owner, Optional<UUID> player, Optional<GlobalPos> bed,
                        GlobalPos from, long due) {
        private static final Codec<Trip> CODEC = RecordCodecBuilder.create(i -> i.group(
                CompoundTag.CODEC.fieldOf("dog").forGetter(Trip::dog),
                UUIDUtil.CODEC.fieldOf("owner").forGetter(Trip::owner),
                UUIDUtil.CODEC.optionalFieldOf("player").forGetter(Trip::player),
                GlobalPos.CODEC.optionalFieldOf("bed").forGetter(Trip::bed),
                GlobalPos.CODEC.fieldOf("from").forGetter(Trip::from),
                Codec.LONG.fieldOf("due").forGetter(Trip::due)
        ).apply(i, Trip::new));

        boolean returning() {
            return player.isEmpty() && bed.isEmpty();
        }
    }

    /** A locator only. A dog waiting in the world keeps its own parcel. */
    private record Stop(UUID owner, GlobalPos position, boolean recalled) {
        private static final Codec<Stop> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("owner").forGetter(Stop::owner),
                GlobalPos.CODEC.fieldOf("pos").forGetter(Stop::position),
                Codec.BOOL.optionalFieldOf("recalled", false).forGetter(Stop::recalled)
        ).apply(i, Stop::new));
    }

    private static final SavedDataType<CourierTrips> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "courier_trips"), () -> new CourierTrips(List.of(), Map.of()),
            RecordCodecBuilder.create(i -> i.group(
                    Trip.CODEC.listOf().fieldOf("trips").forGetter(data -> List.copyOf(data.trips)),
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, Stop.CODEC).optionalFieldOf("stops", Map.of())
                            .forGetter(data -> Map.copyOf(data.stops))
            ).apply(i, CourierTrips::new)));
    private final List<Trip> trips;
    private final Map<UUID, Stop> stops;

    private CourierTrips(List<Trip> trips, Map<UUID, Stop> stops) {
        this.trips = new ArrayList<>(trips);
        this.stops = new HashMap<>(stops);
    }

    static int plannedSeconds(ServerLevel from, Vec3 start, ServerLevel to, Vec3 end) {
        return 4 + DeliveryGoal.travelSeconds(from, start, to, end);
    }

    static int arrivalSeconds(ServerLevel from, Vec3 start, ServerLevel to, Vec3 end) {
        var server = from.getServer();
        long nanosPerTick = Math.max(server.tickRateManager().nanosecondsPerTick(), server.getAverageTickTimeNanos());
        return (int) Math.ceil(plannedSeconds(from, start, to, end) * 20.0 * nanosPerTick / 1_000_000_000.0);
    }

    static void track(FrenchieEntity dog) {
        if (dog.getOwnerReference() == null || !(dog.level() instanceof ServerLevel level)) return;
        var data = level.getServer().getDataStorage().computeIfAbsent(TYPE);
        var old = data.stops.get(dog.getUUID());
        if (old != null && old.position.dimension().equals(level.dimension())
                && ChunkPos.containing(old.position.pos()).equals(dog.chunkPosition())) return;
        data.stops.put(dog.getUUID(), new Stop(dog.getOwnerReference().getUUID(),
                GlobalPos.of(level.dimension(), dog.blockPosition()), old != null && old.recalled));
        data.setDirty();
    }

    public static void forget(FrenchieEntity dog) {
        if (!(dog.level() instanceof ServerLevel level)) return;
        var data = level.getServer().getDataStorage().get(TYPE);
        if (data != null && data.stops.remove(dog.getUUID()) != null) data.setDirty();
    }

    static void depart(FrenchieEntity dog, @Nullable UUID player, @Nullable GlobalPos bed, boolean returning, int seconds) {
        departAfterTicks(dog, player, bed, returning, 20L * seconds);
    }

    static void departAfterTicks(FrenchieEntity dog, @Nullable UUID player, @Nullable GlobalPos bed, boolean returning, long ticks) {
        if (dog.isRemoved()) return;
        var server = ((ServerLevel) dog.level()).getServer();
        dog.stopRiding();
        dog.ejectPassengers();
        if (dog.isLeashed()) dog.dropLeash();
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, dog.registryAccess());
        dog.saveWithoutId(output);
        var data = server.getDataStorage().computeIfAbsent(TYPE);
        var stop = data.stops.remove(dog.getUUID());
        if (stop != null && stop.recalled) {
            returning = true;
            ticks = 0;
        }
        data.trips.add(new Trip(output.buildResult(), dog.getOwnerReference().getUUID(),
                Optional.ofNullable(returning ? null : player), Optional.ofNullable(returning || player != null ? null : bed),
                GlobalPos.of(dog.level().dimension(), dog.blockPosition()), server.overworld().getGameTime() + Math.max(0, ticks)));
        data.setDirty();
        dog.discard();
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        var server = event.getServer();
        long now = server.overworld().getGameTime();
        if (now % 10 != 0) return;
        var data = server.getDataStorage().get(TYPE);
        if (data != null) {
            data.recallWaiting(server);
            data.deliverDue(server, now);
        }
    }

    /** Recall this owner's traveling or waiting couriers, including from unloaded beds. */
    static int recall(ServerPlayer owner) {
        var server = owner.level().getServer();
        var data = server.getDataStorage().computeIfAbsent(TYPE);
        UUID id = owner.getUUID();
        int count = 0;
        var it = data.trips.listIterator();
        while (it.hasNext()) {
            Trip trip = it.next();
            if (!trip.owner.equals(id)) continue;
            it.set(new Trip(trip.dog, id, Optional.empty(), Optional.empty(), trip.from, 0));
            count++;
        }
        // Also find a loaded courier from a pre-locator save, including one still on its departure walk.
        for (var level : server.getAllLevels()) {
            for (var dog : level.getEntities(FloraFaunaRegistry.FRENCHIE.get(),
                    d -> d.isAlive() && d.isOwnedBy(owner) && (d.isDelivering() || d.isGoingHome()))) {
                if (dog.recallDelivery(owner)) count++;
            }
        }
        for (var entry : data.stops.entrySet()) {
            Stop stop = entry.getValue();
            if (!stop.owner.equals(id) || owner.level().getEntityInAnyDimension(entry.getKey()) != null) continue;
            entry.setValue(new Stop(id, stop.position, true));
            count++;
        }
        if (count > 0) data.setDirty();
        return count;
    }

    private void recallWaiting(MinecraftServer server) {
        // The loaded dog may remove its own locator when departing.
        var requests = stops.entrySet().stream().filter(e -> e.getValue().recalled).map(Map.Entry::copyOf).toList();
        for (var request : requests) {
            Stop stop = request.getValue();
            var owner = server.getPlayerList().getPlayer(stop.owner);
            if (owner == null) continue;
            var level = server.getLevel(stop.position.dimension());
            if (level != null) {
                var chunk = ChunkPos.containing(stop.position.pos());
                level.getChunkSource().addTicketWithRadius(FloraFaunaRegistry.COURIER_STOP.get(), chunk, 2);
                if (!level.areEntitiesActuallyLoadedAndTicking(chunk)) continue;
                var entity = level.getEntityInAnyDimension(request.getKey());
                if (entity instanceof FrenchieEntity dog && dog.recallDelivery(owner)) continue;
            }
            stops.remove(request.getKey());
            setDirty();
        }
    }

    private void deliverDue(MinecraftServer server, long now) {
        var iterator = trips.listIterator();
        while (iterator.hasNext()) {
            Trip trip = iterator.next();
            if (now < trip.due) continue;
            var owner = server.getPlayerList().getPlayer(trip.owner);
            ServerLevel level;
            BlockPos destination;
            ServerPlayer target = null;
            if (trip.bed.isPresent()) {
                GlobalPos bed = trip.bed.get();
                level = server.getLevel(bed.dimension());
                destination = bed.pos();
            } else {
                target = trip.returning() ? owner : server.getPlayerList().getPlayer(trip.player.orElseThrow());
                level = DeliveryGoal.available(target) ? target.level() : null;
                destination = level == null ? null : target.blockPosition();
            }

            if (level != null && destination != null) {
                var chunk = ChunkPos.containing(destination);
                // An isolated bed only needs its endpoint loaded for arrival. The ticket expires on its own.
                level.getChunkSource().addTicketWithRadius(FloraFaunaRegistry.COURIER_STOP.get(), chunk, 2);
                if (!level.areEntitiesActuallyLoadedAndTicking(chunk)) continue;
                if (trip.bed.isEmpty() || DeliveryGoal.isBedHead(level, destination)) {
                    Vec3 landing = findLanding(level, destination, target);
                    if (landing != null) {
                        if (restore(level, trip, landing)) {
                            iterator.remove();
                            setDirty();
                        }
                        continue;
                    }
                }
            }
            if (!trip.returning()) {
                var fromLevel = level != null ? level : server.getLevel(trip.from.dimension());
                var from = destination == null ? trip.from.pos() : destination;
                int seconds = fromLevel == null || owner == null ? 3
                        : DeliveryGoal.travelSeconds(fromLevel, Vec3.atBottomCenterOf(from), owner.level(), owner.position());
                iterator.set(new Trip(trip.dog, trip.owner, Optional.empty(), Optional.empty(), trip.from, now + 20L * seconds));
                setDirty();
                DeliveryGoal.tell(owner, trip.dog.getBooleanOr("FrenchieGoingHome", false) ? "home_unavailable"
                        : trip.bed.isPresent() ? "bed_unavailable" : "unavailable");
            }
            // An absent owner or unsafe return point leaves the dog and cargo in transit until a safe arrival.
        }
    }

    private static @Nullable Vec3 findLanding(ServerLevel level, BlockPos target, @Nullable ServerPlayer player) {
        Vec3 fallback = null;
        for (BlockPos pos : BlockPos.withinManhattan(target, 2, 2, 2)) {
            if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos) || !level.getFluidState(pos).isEmpty()) continue;
            if (player == null && (level.getBlockState(pos).getBlock() instanceof BedBlock
                    || level.getBlockState(pos.below()).getBlock() instanceof BedBlock)) continue;
            Vec3 safe = DismountHelper.findSafeDismountLocation(FloraFaunaRegistry.FRENCHIE.get(), level, pos, true);
            if (safe == null) continue;
            if (player != null && level.clip(new ClipContext(safe.add(0, 0.4, 0), player.getEyePosition(),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) continue;
            if (pos.getX() != target.getX() || pos.getZ() != target.getZ()) return safe;
            if (player != null && fallback == null) fallback = safe;
        }
        return fallback;
    }

    private static boolean restore(ServerLevel level, Trip trip, Vec3 position) {
        var dog = FloraFaunaRegistry.FRENCHIE.get().create(level, EntitySpawnReason.LOAD);
        if (dog == null) return false;
        dog.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), trip.dog));
        dog.snapTo(position, dog.getYRot(), 0);
        dog.setDeltaMovement(Vec3.ZERO);
        dog.resetFallDistance();
        dog.delivery().arrived(trip.returning());
        if (!level.addFreshEntity(dog)) return false;
        var owner = level.getServer().getPlayerList().getPlayer(trip.owner);
        if (dog.isDelivering() || dog.isGoingHome()) track(dog);
        if (trip.returning() && !dog.isDelivering()) DeliveryGoal.tell(owner,
                trip.dog.getBooleanOr("FrenchieGoingHome", false) ? "home_back" : "back", dog.getName());
        return true;
    }
}
