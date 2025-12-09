package net.j40climb.florafauna.common.attachments;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.symbiote.ability.SymbioteAbilityData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Registry for attachment types that store data on entities (specifically players).
 * Attachments are the modern NeoForge way to add custom data to existing objects.
 */
public class ModAttachmentTypes {
    /**
     * Deferred register for attachment types.
     */
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, FloraFauna.MOD_ID);

    /**
     * Symbiote data attachment for players.
     * Stores the current state of a player's bonded symbiote including:
     * - Bonding status
     * - Evolution tier
     * - Energy and health
     * - Bond time
     */
    public static final Supplier<AttachmentType<SymbioteData>> SYMBIOTE_DATA =
            ATTACHMENT_TYPES.register("symbiote_data", () ->
                    AttachmentType.builder(() -> SymbioteData.DEFAULT)
                            .serialize(SymbioteData.CODEC.fieldOf("symbiote_data"))
                            .sync(SymbioteData.STREAM_CODEC)
                            .build()
            );

    /**
     * Symbiote ability data attachment for players.
     * Tracks progress and unlock status for all symbiote abilities.
     */
    public static final Supplier<AttachmentType<SymbioteAbilityData>> SYMBIOTE_ABILITY_DATA =
            ATTACHMENT_TYPES.register("symbiote_ability_data", () ->
                    AttachmentType.builder(() -> SymbioteAbilityData.DEFAULT)
                            .serialize(SymbioteAbilityData.CODEC.fieldOf("symbiote_ability_data"))
                            .sync(SymbioteAbilityData.STREAM_CODEC)
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