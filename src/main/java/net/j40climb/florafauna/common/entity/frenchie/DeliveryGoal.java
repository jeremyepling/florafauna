package net.j40climb.florafauna.common.entity.frenchie;

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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/** The nonempty parcel is the job. Its single stack stays here until a handoff succeeds. */
final class DeliveryGoal extends Goal {
    private final FrenchieEntity dog;
    private @Nullable GlobalPos home;
    private @Nullable UUID recipient;
    private ItemStack parcel = ItemStack.EMPTY;
    private boolean returning;
    private boolean waiting;
    private int nextAttempt;
    private int stalledTicks;
    private double closestDistance;

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

    void start(ServerPlayer owner, InteractionHand hand, @Nullable ServerPlayer target) {
        parcel = owner.getItemInHand(hand).split(1);
        recipient = target == null ? null : target.getUUID();
        returning = false;
        resetRoute();
        dog.standUp();
        dog.setFrenchieSleeping(false);
        dog.getNavigation().stop();
        tell(owner, "started", target == null ? label("home", home.pos().toShortString()) : target.getName());
    }

    void collect(ServerPlayer owner, InteractionHand emptyHand) {
        boolean delivered = !returning && recipient == null && waiting;
        owner.setItemInHand(emptyHand, takeParcel());
        dog.sitDown();
        tell(owner, delivered ? "collected" : "returned");
    }

    private ItemStack takeParcel() {
        var item = parcel;
        parcel = ItemStack.EMPTY;
        recipient = null;
        returning = false;
        resetRoute();
        dog.getNavigation().stop();
        return item;
    }

    void dropOnDeath(ServerLevel level) {
        if (canUse()) dog.spawnAtLocation(level, takeParcel());
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
        if (!canUse() || --nextAttempt > 0) return;
        nextAttempt = 20;
        ServerLevel level = (ServerLevel) dog.level();
        ServerPlayer owner = dog.getOwner() instanceof ServerPlayer p ? p : null;
        ServerPlayer player = recipient == null ? null : level.getServer().getPlayerList().getPlayer(recipient);
        Vec3 destination;
        if (returning) {
            if (owner == null || !owner.isAlive()) {
                dog.getNavigation().stop();
                return;
            }
            destination = owner.position();
        } else if (recipient != null) {
            if (player == null || player.level() != level || !player.isAlive() || player.isSpectator()) {
                returnToOwner(owner, "unavailable");
                return;
            }
            destination = player.position();
        } else {
            if (home == null || !home.dimension().equals(level.dimension())
                    || level.hasChunkAt(home.pos()) && !isBedHead(level, home.pos())) {
                returnToOwner(owner, "bed_missing");
                return;
            }
            destination = Vec3.atBottomCenterOf(home.pos());
        }

        Vec3 delta = destination.subtract(dog.position());
        double distance = delta.length();
        if (distance <= 2.5 && (player == null || returning || dog.hasLineOfSight(player))) {
            dog.getNavigation().stop();
            if (returning) {
                if (!waiting) tell(owner, "recover");
            } else if (player != null) {
                int slot = player.getInventory().getFreeSlot();
                if (slot >= 0) {
                    player.getInventory().setItem(slot, takeParcel());
                    tell(player, "received", dog.getName());
                    tell(owner, "complete", player.getName());
                    return;
                }
                if (!waiting) {
                    tell(player, "need_slot", dog.getName());
                    tell(owner, "full", player.getName());
                }
            } else if (!waiting) {
                tell(owner, "at_bed", home.pos().toShortString());
            }
            resetRoute();
            waiting = true;
            return;
        }

        waiting = false;
        if (distance < closestDistance - 0.5) {
            closestDistance = distance;
            stalledTicks = 0;
        } else if (!returning && (stalledTicks += 20) >= 300) {
            returnToOwner(owner, "blocked");
            return;
        }
        Vec3 waypoint = dog.position().add(delta.scale(Math.min(distance, 14) / Math.max(distance, 0.01)));
        dog.getNavigation().moveTo(dog.getNavigation().createPath(BlockPos.containing(waypoint), 1), 1.2);
    }

    private void returnToOwner(@Nullable ServerPlayer owner, String reason) {
        returning = true;
        resetRoute();
        dog.getNavigation().stop();
        tell(owner, reason);
    }

    private void resetRoute() {
        waiting = false;
        stalledTicks = 0;
        closestDistance = Double.POSITIVE_INFINITY;
    }

    void save(ValueOutput output) {
        if (home != null) output.store("FrenchieHomeBed", GlobalPos.CODEC, home);
        if (!canUse()) return;
        output.store("FrenchieParcel", ItemStack.CODEC, parcel);
        if (recipient != null) output.store("FrenchieRecipient", UUIDUtil.CODEC, recipient);
        output.putBoolean("FrenchieReturning", returning);
        output.putBoolean("FrenchieAtHandoff", waiting);
    }

    void load(ValueInput input) {
        home = input.read("FrenchieHomeBed", GlobalPos.CODEC).orElse(null);
        parcel = input.read("FrenchieParcel", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        recipient = input.read("FrenchieRecipient", UUIDUtil.CODEC).orElse(null);
        returning = input.getBooleanOr("FrenchieReturning", false);
        resetRoute();
        waiting = input.getBooleanOr("FrenchieAtHandoff", false);
        nextAttempt = 0;
    }

    static Component label(String key, Object... args) {
        return Component.translatable("frenchie.florafauna.delivery." + key, args);
    }

    static void tell(@Nullable ServerPlayer player, String key, Object... args) {
        if (player != null) player.sendOverlayMessage(label(key, args));
    }
}
