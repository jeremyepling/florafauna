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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Block entity for MobOutput - destination where captured mobs emerge.
 * <p>
 * Features:
 * - Receives tickets from paired MobInputs
 * - Spawns mobs when tickets are ready for release
 * - Tracks all paired MobInputs
 * - Can grow new MobInputs when bonemealed
 */
public class MobOutputBlockEntity extends BlockEntity {
    private static final String KEY_PAIRED_INPUTS = "paired_inputs";
    private static final String KEY_PENDING = "pending_release";

    // Paired MobInput positions
    private final Set<BlockPos> pairedInputs = new HashSet<>();

    // Tickets waiting for release
    private final List<CapturedMobTicket> pendingRelease = new ArrayList<>();

    // Release cooldown
    private int releaseCooldown = 0;

    public MobOutputBlockEntity(BlockPos pos, BlockState state) {
        super(FloraFaunaRegistry.MOB_OUTPUT_BE.get(), pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }

        tickRelease(level, pos);
    }

    private void tickRelease(Level level, BlockPos pos) {
        releaseCooldown--;
        if (releaseCooldown > 0) {
            return;
        }
        releaseCooldown = Config.releaseCheckIntervalTicks;

        if (pendingRelease.isEmpty()) {
            return;
        }

        long currentTick = level.getGameTime();

        // Find ready tickets
        Iterator<CapturedMobTicket> iter = pendingRelease.iterator();
        while (iter.hasNext()) {
            CapturedMobTicket ticket = iter.next();
            if (ticket.isReady(currentTick)) {
                // Spawn the mob
                spawnMob(level, pos, ticket);
                iter.remove();
                setChanged();
                break; // One release per tick
            }
        }
    }

    private void spawnMob(Level level, BlockPos pos, CapturedMobTicket ticket) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Create entity from NBT
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(ticket.entityTypeId());
        if (type == null) {
            return;
        }

        Entity entity = type.create(serverLevel, EntitySpawnReason.TRIGGERED);
        if (entity == null) {
            return;
        }

        // Load entity data from NBT
        ValueInput input = TagValueInput.create(
                ProblemReporter.DISCARDING,
                serverLevel.registryAccess(),
                ticket.entityNbt()
        );
        entity.load(input);

        // Spawn slightly above the block
        entity.snapTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0, 0);
        serverLevel.addFreshEntity(entity);

        // Mark as recently released (capture immunity) and remove any lure goal
        if (entity instanceof Mob mob) {
            long immunityUntil = level.getGameTime() + Config.recentlyReleasedImmunityTicks;
            MobSymbioteHelper.markRecentlyReleased(mob, immunityUntil);
            MobSymbioteHelper.stopLuring(mob);
        }

        // Play emergence sound
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 1.0f, 0.8f + level.random.nextFloat() * 0.4f);
    }

    /**
     * Accepts a ticket from a MobInput.
     *
     * @param ticket The captured mob ticket
     * @return true if accepted (always accepts currently)
     */
    public boolean acceptTicket(CapturedMobTicket ticket) {
        pendingRelease.add(ticket);
        setChanged();
        return true;
    }

    /**
     * Registers a MobInput as paired.
     *
     * @param inputPos The MobInput position
     */
    public void pairInput(BlockPos inputPos) {
        if (pairedInputs.add(inputPos)) {
            setChanged();
        }
    }

    /**
     * Unregisters a MobInput.
     *
     * @param inputPos The MobInput position
     */
    public void unpairInput(BlockPos inputPos) {
        if (pairedInputs.remove(inputPos)) {
            setChanged();
        }
    }

    /**
     * Gets the number of paired inputs.
     */
    public int getInputCount() {
        return pairedInputs.size();
    }

    /**
     * Gets the number of mobs pending release.
     */
    public int getPendingReleaseCount() {
        return pendingRelease.size();
    }

    /**
     * Gets a copy of all paired input positions.
     */
    public Set<BlockPos> getPairedInputs() {
        return new HashSet<>(pairedInputs);
    }

    /**
     * Called when the block is removed.
     * Releases all pending mobs and notifies paired inputs.
     */
    public void onRemoved() {
        // Release all pending mobs at this position
        if (level != null && !level.isClientSide() && level instanceof ServerLevel serverLevel) {
            for (CapturedMobTicket ticket : pendingRelease) {
                spawnMob(serverLevel, worldPosition, ticket);
            }
        }
        pendingRelease.clear();

        // Unpair all inputs
        if (level != null) {
            for (BlockPos inputPos : new HashSet<>(pairedInputs)) {
                BlockEntity be = level.getBlockEntity(inputPos);
                if (be instanceof MobInputBlockEntity input) {
                    input.unpairOutput();
                }
            }
        }
        pairedInputs.clear();
    }

    // ==================== SERIALIZATION ====================

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        output.store(KEY_PAIRED_INPUTS, BlockPos.CODEC.listOf(), pairedInputs.stream().toList());
        output.store(KEY_PENDING, CapturedMobTicket.CODEC.listOf(), pendingRelease);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        pairedInputs.clear();
        input.read(KEY_PAIRED_INPUTS, BlockPos.CODEC.listOf()).ifPresent(pairedInputs::addAll);

        pendingRelease.clear();
        input.read(KEY_PENDING, CapturedMobTicket.CODEC.listOf()).ifPresent(pendingRelease::addAll);
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
