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
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
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

    private static final SavedDataType<CourierTrips> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "courier_trips"), () -> new CourierTrips(List.of()),
            Trip.CODEC.listOf().fieldOf("trips").codec().xmap(CourierTrips::new, data -> List.copyOf(data.trips)));
    private final List<Trip> trips;

    private CourierTrips(List<Trip> trips) {
        this.trips = new ArrayList<>(trips);
    }

    static void depart(FrenchieEntity dog, @Nullable UUID player, @Nullable GlobalPos bed, boolean returning, int seconds) {
        if (dog.isRemoved()) return;
        var server = ((ServerLevel) dog.level()).getServer();
        dog.stopRiding();
        dog.ejectPassengers();
        if (dog.isLeashed()) dog.dropLeash();
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, dog.registryAccess());
        dog.saveWithoutId(output);
        var data = server.getDataStorage().computeIfAbsent(TYPE);
        data.trips.add(new Trip(output.buildResult(), dog.getOwnerReference().getUUID(),
                Optional.ofNullable(returning ? null : player), Optional.ofNullable(returning || player != null ? null : bed),
                GlobalPos.of(dog.level().dimension(), dog.blockPosition()), server.overworld().getGameTime() + 20L * seconds));
        data.setDirty();
        dog.discard();
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        var server = event.getServer();
        long now = server.overworld().getGameTime();
        if (now % 10 != 0) return;
        var data = server.getDataStorage().get(TYPE);
        if (data != null) data.deliverDue(server, now);
    }

    private void deliverDue(MinecraftServer server, long now) {
        var iterator = trips.listIterator();
        while (iterator.hasNext()) {
            Trip trip = iterator.next();
            if (now < trip.due) continue;
            var owner = server.getPlayerList().getPlayer(trip.owner);
            ServerLevel level;
            BlockPos destination;
            if (trip.bed.isPresent()) {
                GlobalPos bed = trip.bed.get();
                level = server.getLevel(bed.dimension());
                destination = bed.pos();
            } else {
                var target = trip.returning() ? owner : server.getPlayerList().getPlayer(trip.player.orElseThrow());
                level = DeliveryGoal.available(target) ? target.level() : null;
                destination = level == null ? null : target.blockPosition();
            }

            if (level != null && destination != null) {
                var chunk = ChunkPos.containing(destination);
                // An isolated bed only needs its endpoint loaded for arrival. The ticket expires on its own.
                level.getChunkSource().addTicketWithRadius(FloraFaunaRegistry.COURIER_STOP.get(), chunk, 2);
                if (!level.areEntitiesActuallyLoadedAndTicking(chunk)) continue;
                if (trip.bed.isEmpty() || DeliveryGoal.isBedHead(level, destination)) {
                    Vec3 landing = findLanding(level, destination, trip.bed.isPresent());
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
                        : DeliveryGoal.travelSeconds(fromLevel, from.getBottomCenter(), owner.level(), owner.position());
                iterator.set(new Trip(trip.dog, trip.owner, Optional.empty(), Optional.empty(), trip.from, now + 20L * seconds));
                setDirty();
                DeliveryGoal.tell(owner, trip.bed.isPresent() ? "bed_unavailable" : "unavailable");
            }
            // An absent owner or unsafe return point leaves the dog and cargo in transit until a safe arrival.
        }
    }

    private static @Nullable Vec3 findLanding(ServerLevel level, BlockPos target, boolean besideBed) {
        for (BlockPos pos : BlockPos.withinManhattan(target, 2, 2, 2)) {
            if (besideBed && pos.getX() == target.getX() && pos.getZ() == target.getZ()) continue;
            if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos) || !level.getFluidState(pos).isEmpty()) continue;
            Vec3 safe = DismountHelper.findSafeDismountLocation(FloraFaunaRegistry.FRENCHIE.get(), level, pos, true);
            if (safe != null) return safe;
        }
        return null;
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
        if (trip.returning() && !dog.isDelivering()) DeliveryGoal.tell(owner, "back", dog.getName());
        return true;
    }
}
