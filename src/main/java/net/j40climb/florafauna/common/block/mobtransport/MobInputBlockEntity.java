package net.j40climb.florafauna.common.block.mobtransport;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.common.entity.mobsymbiote.MobSymbioteHelper;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Block entity for the MobInput "chompy plant".
 * <p>
 * Tick phases:
 * 1. LURE - Apply gentle velocity bias to eligible mobs within lure radius
 * 2. TRACK CLOSEST - Find nearest eligible mob for animation
 * 3. CAPTURE - Capture mobs within capture radius (trigger chomp animation)
 * 4. TRANSFER - Send ready tickets to paired MobOutput after delay
 */
public class MobInputBlockEntity extends BlockEntity {
    private static final String KEY_PAIRED_OUTPUT = "paired_output";
    private static final String KEY_BACKOFF = "backoff_ticks";
    private static final String KEY_TARGET_X = "targetX";
    private static final String KEY_TARGET_Y = "targetY";
    private static final String KEY_TARGET_Z = "targetZ";

    // Paired MobOutput position
    @Nullable
    private BlockPos pairedOutputPos;

    // Buffer for captured mobs
    private final CapturedMobBuffer buffer;

    // Closest mob tracking for animation
    @Nullable
    private Vec3 targetPosition;
    @Nullable
    private UUID targetMobId;

    // Tick counters
    private int lureCooldown = 0;
    private int captureCooldown = 0;
    private int transferCooldown = 0;
    private int chompAnimTicks = 0;

    // Exponential backoff
    private int backoffTicks = 0;
    private int currentBackoff = 0;

    public MobInputBlockEntity(BlockPos pos, BlockState state) {
        super(FloraFaunaRegistry.MOB_INPUT_BE.get(), pos, state);
        this.buffer = new CapturedMobBuffer(Config.maxQueueSizePerInput);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }

        // Handle backoff
        if (backoffTicks > 0) {
            backoffTicks--;
            return;
        }

        // Tick phases
        tickLure(level, pos, state);
        tickCapture(level, pos, state);
        tickTransfer(level, pos, state);
    }

    // ==================== LURE PHASE ====================

    private void tickLure(Level level, BlockPos pos, BlockState state) {
        lureCooldown--;
        if (lureCooldown > 0) {
            return;
        }
        lureCooldown = Config.lureIntervalTicks;

        long currentTick = level.getGameTime();
        int radius = Config.lureRadius;
        Vec3 center = Vec3.atCenterOf(pos);

        AABB lureArea = new AABB(pos).inflate(radius);
        List<Mob> nearbyMobs = level.getEntitiesOfClass(Mob.class, lureArea);

        // Track closest eligible mob for animation
        Mob closestMob = null;
        double closestDistSq = Double.MAX_VALUE;

        // Track which mobs are in range for cleanup
        Set<UUID> mobsInRange = new HashSet<>();

        for (Mob mob : nearbyMobs) {
            // Only mobs with a MobSymbiote can be lured
            if (!MobSymbioteHelper.hasMobSymbiote(mob)) {
                continue;
            }

            // Skip mobs that aren't capture-eligible (recently released, etc.)
            if (!MobCaptureEligibility.isEligible(mob, currentTick)) {
                continue;
            }

            mobsInRange.add(mob.getUUID());
            double distSq = mob.distanceToSqr(center);

            // Track closest for animation
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closestMob = mob;
            }

            // Start luring if not full and not already lured to this block
            if (!buffer.isFull() && !MobSymbioteHelper.isBeingLuredTo(mob, pos)) {
                MobSymbioteHelper.startLuring(mob, pos);
            }
        }

        // Stop luring mobs that have left the area or are now lured elsewhere
        // We need to scan a larger area to find mobs that were lured but left
        AABB cleanupArea = new AABB(pos).inflate(radius + 10);
        List<Mob> allNearbyMobs = level.getEntitiesOfClass(Mob.class, cleanupArea);
        for (Mob mob : allNearbyMobs) {
            if (MobSymbioteHelper.isBeingLuredTo(mob, pos) && !mobsInRange.contains(mob.getUUID())) {
                // Mob was lured to this block but is no longer in range
                MobSymbioteHelper.stopLuring(mob);
            }
        }

        // Update target tracking
        Vec3 oldTarget = targetPosition;
        if (closestMob != null) {
            targetPosition = closestMob.position();
            targetMobId = closestMob.getUUID();
        } else {
            targetPosition = null;
            targetMobId = null;
        }

        // Sync if target changed significantly
        if ((oldTarget == null) != (targetPosition == null) ||
                (oldTarget != null && targetPosition != null && oldTarget.distanceToSqr(targetPosition) > 1.0)) {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }

        // Update visual state based on activity
        MobInputState currentState = state.getValue(MobInputBlock.STATE);
        if (closestMob != null && !buffer.isFull()) {
            if (currentState == MobInputState.IDLE) {
                updateVisualState(level, pos, state, MobInputState.OPEN);
            }
        } else if (currentState == MobInputState.OPEN && chompAnimTicks <= 0) {
            updateVisualState(level, pos, state, MobInputState.IDLE);
        }
    }

    // ==================== CAPTURE PHASE ====================

    private void tickCapture(Level level, BlockPos pos, BlockState state) {
        // Handle chomp animation
        if (chompAnimTicks > 0) {
            chompAnimTicks--;
            if (chompAnimTicks == 0) {
                // Animation complete, return to appropriate state
                MobInputState newState = targetPosition != null ? MobInputState.OPEN : MobInputState.IDLE;
                updateVisualState(level, pos, state, newState);
            }
            return; // Don't capture during animation
        }

        captureCooldown--;
        if (captureCooldown > 0) {
            return;
        }
        captureCooldown = 20; // Check every second

        if (buffer.isFull() || !isPaired()) {
            return;
        }

        long currentTick = level.getGameTime();
        double captureRadius = Config.captureRadius;
        Vec3 center = Vec3.atCenterOf(pos);

        AABB captureArea = new AABB(pos).inflate(captureRadius);
        List<Mob> nearbyMobs = level.getEntitiesOfClass(Mob.class, captureArea);

        // Sort by priority: mobs with MobSymbiote first
        nearbyMobs.sort(Comparator.comparing(mob ->
                MobCaptureEligibility.hasMobSymbiote(mob) ? 0 : 1));

        for (Mob mob : nearbyMobs) {
            if (!MobCaptureEligibility.isEligible(mob, currentTick)) {
                continue;
            }
            if (!buffer.canAccept()) {
                break;
            }

            // Capture this mob!
            captureMob(level, mob, currentTick);

            // Trigger chomp animation
            chompAnimTicks = Config.captureAnimTicks;
            updateVisualState(level, pos, state, MobInputState.CHOMPING);

            // Play capture sound
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.GENERIC_EAT.value(), SoundSource.BLOCKS, 1.0f, 0.8f + level.random.nextFloat() * 0.4f);

            break; // One capture per tick
        }
    }

    private void captureMob(Level level, Mob mob, long currentTick) {
        // Serialize mob to NBT using TagValueOutput
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING,
                level.registryAccess()
        );
        mob.saveWithoutId(output);
        CompoundTag nbt = output.buildResult();

        // Calculate release delay
        int minDelay = Config.minTravelDelayTicks;
        int maxDelay = Config.maxTravelDelayTicks;
        int delay = minDelay + level.random.nextInt(maxDelay - minDelay + 1);

        // Create ticket
        CapturedMobTicket ticket = CapturedMobTicket.create(
                mob.getType(),
                nbt,
                currentTick,
                currentTick + delay,
                pairedOutputPos,
                level.dimension()
        );

        buffer.add(ticket);

        // Trigger symbiote dialogue for nearby players
        if (level instanceof ServerLevel serverLevel) {
            MobTransportDialogueEvents.onMobCaptured(serverLevel, worldPosition);
        }

        // Stop luring before removing
        MobSymbioteHelper.stopLuring(mob);

        // Remove mob from world
        mob.discard();

        setChanged();
    }

    // ==================== TRANSFER PHASE ====================

    private void tickTransfer(Level level, BlockPos pos, BlockState state) {
        transferCooldown--;
        if (transferCooldown > 0) {
            return;
        }
        transferCooldown = Config.releaseCheckIntervalTicks;

        if (buffer.isEmpty() || !isPaired()) {
            return;
        }

        long currentTick = level.getGameTime();
        Optional<CapturedMobTicket> readyTicket = buffer.getReadyTicket(currentTick);

        if (readyTicket.isEmpty()) {
            return;
        }

        // Get paired output
        MobOutputBlockEntity output = getPairedOutput(level);
        if (output == null) {
            applyBackoff();
            return;
        }

        // Transfer ticket to output
        if (output.acceptTicket(readyTicket.get())) {
            buffer.remove(readyTicket.get());
            currentBackoff = 0; // Reset backoff on success
            setChanged();
        } else {
            applyBackoff();
        }
    }

    // ==================== PAIRING ====================

    public void pairWithOutput(BlockPos outputPos) {
        // Unpair from old output first
        if (pairedOutputPos != null && level != null) {
            MobOutputBlockEntity oldOutput = getPairedOutput(level);
            if (oldOutput != null) {
                oldOutput.unpairInput(worldPosition);
            }
        }

        this.pairedOutputPos = outputPos;

        // Register with new output
        if (level != null && outputPos != null) {
            BlockEntity be = level.getBlockEntity(outputPos);
            if (be instanceof MobOutputBlockEntity output) {
                output.pairInput(worldPosition);
            }
        }

        setChanged();
    }

    public void unpairOutput() {
        if (pairedOutputPos != null && level != null) {
            MobOutputBlockEntity output = getPairedOutput(level);
            if (output != null) {
                output.unpairInput(worldPosition);
            }
        }
        pairedOutputPos = null;
        setChanged();
    }

    @Nullable
    private MobOutputBlockEntity getPairedOutput(Level level) {
        if (pairedOutputPos == null) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(pairedOutputPos);
        if (be instanceof MobOutputBlockEntity output) {
            return output;
        }
        // Output was removed
        pairedOutputPos = null;
        setChanged();
        return null;
    }

    public boolean isPaired() {
        return pairedOutputPos != null;
    }

    // ==================== HELPERS ====================

    private void applyBackoff() {
        if (currentBackoff == 0) {
            currentBackoff = Config.blockedRetryBaseTicks;
        } else {
            currentBackoff = Math.min(currentBackoff * 2, Config.blockedRetryMaxTicks);
        }
        backoffTicks = currentBackoff;
    }

    private void updateVisualState(Level level, BlockPos pos, BlockState state, MobInputState newState) {
        if (state.getValue(MobInputBlock.STATE) != newState) {
            level.setBlock(pos, state.setValue(MobInputBlock.STATE, newState), Block.UPDATE_ALL);
        }
    }

    public void onRemoved() {
        // Stop luring all mobs that were being lured to this block
        if (level != null && !level.isClientSide()) {
            int radius = Config.lureRadius + 10;
            AABB cleanupArea = new AABB(worldPosition).inflate(radius);
            List<Mob> nearbyMobs = level.getEntitiesOfClass(Mob.class, cleanupArea);
            for (Mob mob : nearbyMobs) {
                if (MobSymbioteHelper.isBeingLuredTo(mob, worldPosition)) {
                    MobSymbioteHelper.stopLuring(mob);
                }
            }
        }

        // Release all captured mobs at the block position
        if (level != null && !level.isClientSide() && level instanceof ServerLevel serverLevel) {
            for (CapturedMobTicket ticket : buffer.getTickets()) {
                spawnMobFromTicket(serverLevel, worldPosition, ticket);
            }
        }
        buffer.clear();
        unpairOutput();
    }

    private void spawnMobFromTicket(ServerLevel level, BlockPos pos, CapturedMobTicket ticket) {
        // Create entity from NBT
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(ticket.entityTypeId());
        if (type == null) {
            return;
        }

        Entity entity = type.create(level, EntitySpawnReason.TRIGGERED);
        if (entity == null) {
            return;
        }

        // Load entity data from NBT
        ValueInput input = TagValueInput.create(
                ProblemReporter.DISCARDING,
                level.registryAccess(),
                ticket.entityNbt()
        );
        entity.load(input);

        entity.snapTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0, 0);
        level.addFreshEntity(entity);

        // Mark as recently released
        if (entity instanceof Mob mob) {
            long immunityUntil = level.getGameTime() + Config.recentlyReleasedImmunityTicks;
            MobSymbioteHelper.markRecentlyReleased(mob, immunityUntil);
        }
    }

    // ==================== ACCESSORS ====================

    public CapturedMobBuffer getBuffer() {
        return buffer;
    }

    @Nullable
    public BlockPos getPairedOutputPos() {
        return pairedOutputPos;
    }

    /**
     * Gets the position of the closest tracked mob for animation.
     * Client-side renderers can use this to orient "eyes" toward the target.
     *
     * @return The target position, or null if no target
     */
    @Nullable
    public Vec3 getTargetPosition() {
        return targetPosition;
    }

    // ==================== SERIALIZATION ====================

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        if (pairedOutputPos != null) {
            output.store(KEY_PAIRED_OUTPUT, BlockPos.CODEC, pairedOutputPos);
        }
        output.putInt(KEY_BACKOFF, currentBackoff);
        buffer.serialize(output);

        // Save target position for client sync
        if (targetPosition != null) {
            output.putDouble(KEY_TARGET_X, targetPosition.x);
            output.putDouble(KEY_TARGET_Y, targetPosition.y);
            output.putDouble(KEY_TARGET_Z, targetPosition.z);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        pairedOutputPos = input.read(KEY_PAIRED_OUTPUT, BlockPos.CODEC).orElse(null);
        currentBackoff = input.getIntOr(KEY_BACKOFF, 0);
        buffer.deserialize(input);

        // Load target position for client
        // Use -1 as sentinel value since world coordinates are unlikely to be exactly -1
        double targetX = input.getDoubleOr(KEY_TARGET_X, Double.NaN);
        double targetY = input.getDoubleOr(KEY_TARGET_Y, Double.NaN);
        double targetZ = input.getDoubleOr(KEY_TARGET_Z, Double.NaN);
        if (!Double.isNaN(targetX) && !Double.isNaN(targetY) && !Double.isNaN(targetZ)) {
            targetPosition = new Vec3(targetX, targetY, targetZ);
        } else {
            targetPosition = null;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
