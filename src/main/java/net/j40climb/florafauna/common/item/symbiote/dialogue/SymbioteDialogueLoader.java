package net.j40climb.florafauna.common.item.symbiote.dialogue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.j40climb.florafauna.FloraFauna;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reload listener for loading symbiote dialogue from datapacks.
 * Loads JSON files from data/florafauna/symbiote_dialogue/
 */
public class SymbioteDialogueLoader extends SimplePreparableReloadListener<List<SymbioteDialogueEntry>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DIRECTORY = "symbiote_dialogue";

    @Override
    protected List<SymbioteDialogueEntry> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        List<SymbioteDialogueEntry> allEntries = new ArrayList<>();

        // Find all JSON files in data/*/symbiote_dialogue/
        String path = DIRECTORY;
        Map<Identifier, Resource> resources = resourceManager.listResources(path,
                location -> location.getPath().endsWith(".json"));

        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier location = entry.getKey();
            Resource resource = entry.getValue();

            try (Reader reader = resource.openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                parseJsonFile(json, location, allEntries);
            } catch (Exception e) {
                LOGGER.error("Failed to load symbiote dialogue from {}: {}", location, e.getMessage());
            }
        }

        return allEntries;
    }

    @Override
    protected void apply(List<SymbioteDialogueEntry> entries, ResourceManager resourceManager, ProfilerFiller profiler) {
        // Set the loaded repository
        SymbioteDialogueRepository repository = new SymbioteDialogueRepository(entries);
        SymbioteDialogueRepository.setInstance(repository);

        LOGGER.info("Loaded {} symbiote dialogue entries", entries.size());
    }

    /**
     * Parse a JSON file that may contain a single entry or a "lines" array
     */
    private void parseJsonFile(JsonElement json, Identifier source, List<SymbioteDialogueEntry> entries) {
        if (!json.isJsonObject()) {
            LOGGER.warn("Expected JSON object in {}", source);
            return;
        }

        JsonObject obj = json.getAsJsonObject();

        // Check for "lines" array format (backwards compatible)
        if (obj.has("lines") && obj.get("lines").isJsonArray()) {
            for (JsonElement lineElement : obj.getAsJsonArray("lines")) {
                parseEntry(lineElement, source, entries);
            }
        } else if (obj.has("dialogue") && obj.get("dialogue").isJsonArray()) {
            // New "dialogue" array format
            for (JsonElement dialogueElement : obj.getAsJsonArray("dialogue")) {
                parseEntry(dialogueElement, source, entries);
            }
        } else {
            // Single entry format
            parseEntry(json, source, entries);
        }
    }

    /**
     * Parse a single entry from JSON
     */
    private void parseEntry(JsonElement json, Identifier source, List<SymbioteDialogueEntry> entries) {
        var result = SymbioteDialogueEntry.CODEC.parse(JsonOps.INSTANCE, json);

        result.resultOrPartial(error ->
            LOGGER.warn("Error parsing symbiote dialogue from {}: {}", source, error)
        ).ifPresent(entries::add);
    }

    /**
     * Resource location for this reload listener
     */
    private static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(
            FloraFauna.MOD_ID, "symbiote_dialogue"
    );

    /**
     * Register this loader as a reload listener.
     * Call from NeoForge.EVENT_BUS.
     *
     * @param event The add server reload listeners event
     */
    public static void registerReloadListener(AddServerReloadListenersEvent event) {
        event.addListener(LISTENER_ID, new SymbioteDialogueLoader());
        LOGGER.debug("Registered symbiote dialogue reload listener");
    }

    /**
     * Get the current repository (convenience method)
     */
    public static SymbioteDialogueRepository getRepository() {
        return SymbioteDialogueRepository.getInstance();
    }
}
