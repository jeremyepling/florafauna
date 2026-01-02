package net.j40climb.florafauna.common;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.entity.frontpack.FrontpackData;
import net.j40climb.florafauna.common.item.symbiote.PlayerSymbioteData;
import net.j40climb.florafauna.common.item.symbiote.progress.ProgressSignalTracker;
import net.j40climb.florafauna.common.item.symbiote.voice.VoiceCooldownState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Registry for attachment types that store data on entities (specifically players).
 * Attachments are the modern NeoForge way to add custom data to existing objects.
 */
public class RegisterAttachmentTypes {
    /**
     * Deferred register for attachment types.
     */
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, FloraFauna.MOD_ID);

    /**
     * Player symbiote data attachment.
     * Combines symbiote bond state with cocoon chamber state:
     * - Bonding status, evolution tier, bond time, ability toggles
     * - Cocoon spawn position/dimension
     * - Previous bed spawn (for restoration)
     * - Progression flags
     */
    public static final Supplier<AttachmentType<PlayerSymbioteData>> PLAYER_SYMBIOTE_DATA =
            ATTACHMENT_TYPES.register("player_symbiote_data", () ->
                    AttachmentType.builder(() -> PlayerSymbioteData.DEFAULT)
                            .serialize(PlayerSymbioteData.CODEC.fieldOf("player_symbiote_data"))
                            .sync(PlayerSymbioteData.STREAM_CODEC)
                            .build()
            );

    /**
     * Frenchie frontpack data attachment for players.
     * Stores the NBT data of a carried Frenchie (despawned entity).
     * Used for the frontpack carrying feature - shift-right-click to pickup/put down.
     * Includes:
     * - hasCarriedFrenchie flag
     * - frenchieNBT (full entity state)
     * - pickupTimestamp
     */
    public static final Supplier<AttachmentType<FrontpackData>> FRENCH_FRONTPACK_DATA =
            ATTACHMENT_TYPES.register("french_frontpack_data", () ->
                    AttachmentType.builder(() -> FrontpackData.DEFAULT)
                            .serialize(FrontpackData.CODEC.fieldOf("french_frontpack_data"))
                            .sync(FrontpackData.STREAM_CODEC)
                            .build()
            );

    /**
     * Symbiote progress signal tracker for dream/commentary system.
     * Tracks concept states (SEEN, TOUCHED, STABILIZED, INTEGRATED, NEGLECTED),
     * dream escalation level, and stall detection metrics.
     */
    public static final Supplier<AttachmentType<ProgressSignalTracker>> SYMBIOTE_PROGRESS =
            ATTACHMENT_TYPES.register("symbiote_progress", () ->
                    AttachmentType.builder(() -> ProgressSignalTracker.DEFAULT)
                            .serialize(ProgressSignalTracker.CODEC.fieldOf("symbiote_progress"))
                            .sync(ProgressSignalTracker.STREAM_CODEC)
                            .build()
            );

    /**
     * Voice cooldown state for tiered commentary system.
     * Tracks global tier cooldowns, per-category dampening, and post-tier-2 lockout.
     * Ensures the symbiote voice remains rare and impactful.
     */
    public static final Supplier<AttachmentType<VoiceCooldownState>> VOICE_COOLDOWNS =
            ATTACHMENT_TYPES.register("voice_cooldowns", () ->
                    AttachmentType.builder(() -> VoiceCooldownState.DEFAULT)
                            .serialize(VoiceCooldownState.CODEC.fieldOf("voice_cooldowns"))
                            .sync(VoiceCooldownState.STREAM_CODEC)
                            .build()
            );

    /**
     * Registers all attachment types to the mod event bus.
     *
     * @param eventBus the mod event bus
     */
    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}