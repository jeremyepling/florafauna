package net.j40climb.florafauna.common.block.mobbarrier;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
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

import java.util.List;

/**
 * Block entity for MobBarrierBlock that stores the configuration
 * of which mobs should be blocked.
 */
public class MobBarrierBlockEntity extends BlockEntity {

    private static final String ENTITY_IDS_KEY = "mob_barrier.entity_ids";
    private static final String ENTITY_TAGS_KEY = "mob_barrier.entity_tags";
    private static final Gson GSON = new Gson();

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
        // Serialize lists as JSON strings
        output.putString(ENTITY_IDS_KEY, GSON.toJson(config.entityIds()));
        output.putString(ENTITY_TAGS_KEY, GSON.toJson(config.entityTags()));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        String entityIdsJson = input.getStringOr(ENTITY_IDS_KEY, "[]");
        String entityTagsJson = input.getStringOr(ENTITY_TAGS_KEY, "[]");

        try {
            List<String> entityIds = GSON.fromJson(entityIdsJson, List.class);
            List<String> entityTags = GSON.fromJson(entityTagsJson, List.class);
            config = new MobBarrierConfig(
                    entityIds != null ? entityIds : List.of(),
                    entityTags != null ? entityTags : List.of()
            );
        } catch (JsonSyntaxException e) {
            config = MobBarrierConfig.DEFAULT;
        }
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
