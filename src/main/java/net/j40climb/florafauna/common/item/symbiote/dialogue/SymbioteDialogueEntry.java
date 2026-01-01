package net.j40climb.florafauna.common.item.symbiote.dialogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.j40climb.florafauna.common.item.symbiote.observation.ObservationCategory;
import net.j40climb.florafauna.common.item.symbiote.voice.VoiceTier;

import java.util.List;

/**
 * A single dialogue entry from the datapack.
 * Contains metadata for filtering and selection.
 */
public record SymbioteDialogueEntry(
    String dialogueKey,
    String tier,
    String category,
    int severityMin,
    int severityMax,
    int weight,
    List<String> requiredConcepts,
    List<String> tags
) {
    /**
     * Codec for JSON parsing
     */
    public static final Codec<SymbioteDialogueEntry> CODEC = RecordCodecBuilder.create(builder ->
        builder.group(
            Codec.STRING.fieldOf("key").forGetter(SymbioteDialogueEntry::dialogueKey),
            Codec.STRING.optionalFieldOf("tier", "TIER_1_AMBIENT").forGetter(SymbioteDialogueEntry::tier),
            Codec.STRING.fieldOf("category").forGetter(SymbioteDialogueEntry::category),
            Codec.INT.optionalFieldOf("severity_min", 0).forGetter(SymbioteDialogueEntry::severityMin),
            Codec.INT.optionalFieldOf("severity_max", 100).forGetter(SymbioteDialogueEntry::severityMax),
            Codec.INT.optionalFieldOf("weight", 100).forGetter(SymbioteDialogueEntry::weight),
            Codec.STRING.listOf().optionalFieldOf("required_concepts", List.of()).forGetter(SymbioteDialogueEntry::requiredConcepts),
            Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(SymbioteDialogueEntry::tags)
        ).apply(builder, SymbioteDialogueEntry::new));

    /**
     * Check if this entry matches the given context
     */
    public boolean matches(DialogueSelectionContext context) {
        // Check tier
        if (!tier.equalsIgnoreCase(context.tier().name())) {
            return false;
        }

        // Check category
        if (!category.equalsIgnoreCase(context.category().getKey())) {
            return false;
        }

        // Check severity range
        if (!context.severityInRange(severityMin, severityMax)) {
            return false;
        }

        // Check required concepts (all must be met)
        for (String requirement : requiredConcepts) {
            if (!checkConceptRequirement(requirement, context)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check a concept requirement string like "first_combat:STABILIZED"
     */
    private boolean checkConceptRequirement(String requirement, DialogueSelectionContext context) {
        if (!requirement.contains(":")) {
            // Just check if concept exists
            return context.progressTracker().getSignal(requirement).isPresent();
        }

        String[] parts = requirement.split(":", 2);
        String conceptId = parts[0];
        String requiredState = parts[1];

        return context.progressTracker().getSignal(conceptId)
            .map(signal -> signal.state().name().equalsIgnoreCase(requiredState))
            .orElse(false);
    }

    /**
     * Parse tier string to enum
     */
    public VoiceTier getTier() {
        try {
            return VoiceTier.valueOf(tier.toUpperCase());
        } catch (IllegalArgumentException e) {
            return VoiceTier.TIER_1_AMBIENT;
        }
    }

    /**
     * Parse category string to enum
     */
    public ObservationCategory getCategory() {
        for (ObservationCategory cat : ObservationCategory.values()) {
            if (cat.getKey().equalsIgnoreCase(category)) {
                return cat;
            }
        }
        return ObservationCategory.PLAYER_STATE;
    }
}
