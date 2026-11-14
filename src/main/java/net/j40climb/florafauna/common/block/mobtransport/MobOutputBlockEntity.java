package net.j40climb.florafauna.common.block.mobtransport;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
                if (level instanceof ServerLevel serverLevel && spawnMob(serverLevel, pos, ticket)) {
                    iter.remove();
                    setChanged();
                    break; // One successful release per interval
                }
            }
        }
    }

    private boolean spawnMob(ServerLevel serverLevel, BlockPos pos, CapturedMobTicket ticket) {

        if (!ticket.release(serverLevel, pos)) return false;

        // Play emergence sound
        serverLevel.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 1.0f, 0.8f + serverLevel.getRandom().nextFloat() * 0.4f);
        return true;
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
     * Release each mob immediately and keep failed tickets and their links for a retry.
     */
    boolean releaseBeforeRemoval(ServerLevel level) {
        var tickets = pendingRelease.iterator();
        while (tickets.hasNext()) {
            if (spawnMob(level, worldPosition, tickets.next())) {
                tickets.remove();
                setChanged();
            }
        }
        if (!pendingRelease.isEmpty()) return false;

        // Unpair all inputs
        for (BlockPos inputPos : new HashSet<>(pairedInputs)) {
            BlockEntity be = level.getBlockEntity(inputPos);
            if (be instanceof MobInputBlockEntity input) {
                input.unpairOutput();
            }
        }
        pairedInputs.clear();
        return true;
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
