package net.j40climb.florafauna.common.block.mobbarrier;

import net.j40climb.florafauna.common.block.mobbarrier.data.MobBarrierConfig;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for MobBarrierBlock that stores the configuration
 * of which mobs should be blocked.
 */
public class MobBarrierBlockEntity extends BlockEntity {

    private static final String CONFIG_KEY = "mob_barrier.config";

    private MobBarrierConfig config = MobBarrierConfig.DEFAULT;

    public MobBarrierBlockEntity(BlockPos pos, BlockState state) {
        super(FloraFaunaRegistry.MOB_BARRIER_BE.get(), pos, state);
    }

    public MobBarrierConfig getConfig() {
        return config;
    }

    public void setConfig(MobBarrierConfig config) {
        this.config = config;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store(CONFIG_KEY, MobBarrierConfig.CODEC, config);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        config = input.read(CONFIG_KEY, MobBarrierConfig.CODEC).orElse(MobBarrierConfig.DEFAULT);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
