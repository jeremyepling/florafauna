package net.j40climb.florafauna.common.item.symbiote.dialogue;

import net.j40climb.florafauna.common.item.symbiote.dream.DreamLevel;

import java.util.*;

/**
 * Repository for symbiote dialogue lines.
 * Provides weighted random selection with repetition prevention.
 */
public class SymbioteDialogueRepository {
    /**
     * All loaded dialogue entries
     */
    private final List<SymbioteDialogueEntry> entries;

    /**
     * Recently used lines to prevent repetition
     */
    private static final int RECENT_LINES_LIMIT = 10;
    private static final Deque<String> recentlyUsedLines = new ArrayDeque<>(RECENT_LINES_LIMIT);

    /**
     * Random for weighted selection
     */
    private static final Random random = new Random();

    /**
     * Empty repository singleton
     */
    public static final SymbioteDialogueRepository EMPTY = new SymbioteDialogueRepository(List.of());

    /**
     * The current active repository (set by the loader)
     */
    private static SymbioteDialogueRepository INSTANCE = EMPTY;

    public SymbioteDialogueRepository(List<SymbioteDialogueEntry> entries) {
        this.entries = new ArrayList<>(entries);
    }

    /**
     * Set the active repository instance (called by loader)
     */
    public static void setInstance(SymbioteDialogueRepository repository) {
        INSTANCE = repository;
    }

    /**
     * Get the current repository instance
     */
    public static SymbioteDialogueRepository getInstance() {
        return INSTANCE;
    }

    /**
     * Select a dialogue matching the given context using weighted random selection.
     *
     * @param context The selection context
     * @return The selected dialogue key, or empty if no matching lines
     */
    public static Optional<String> selectDialogue(DialogueSelectionContext context) {
        return INSTANCE.selectDialogueFromEntries(context);
    }

    /**
     * Select a dream dialogue for the given level.
     *
     * @param level The dream level
     * @return The selected dialogue key, or empty if no matching lines
     */
    public static Optional<String> selectDreamDialogue(DreamLevel level) {
        return INSTANCE.selectDreamDialogueFromEntries(level);
    }

    /**
     * Internal selection logic
     */
    private Optional<String> selectDialogueFromEntries(DialogueSelectionContext context) {
        // Filter matching entries
        List<SymbioteDialogueEntry> matching = entries.stream()
            .filter(entry -> entry.matches(context))
            .filter(entry -> !recentlyUsedLines.contains(entry.dialogueKey()))
            .toList();

        if (matching.isEmpty()) {
            // Fall back to allowing recently used if nothing else matches
            matching = entries.stream()
                .filter(entry -> entry.matches(context))
                .toList();
        }

        if (matching.isEmpty()) {
            return Optional.empty();
        }

        // Weighted random selection
        String selected = weightedRandomSelect(matching);
        markAsUsed(selected);
        return Optional.of(selected);
    }

    /**
     * Select a dream dialogue by level category
     */
    private Optional<String> selectDreamDialogueFromEntries(DreamLevel level) {
        String categoryKey = level.getLineCategory();

        // Filter by dream category
        List<SymbioteDialogueEntry> matching = entries.stream()
            .filter(entry -> entry.category().equalsIgnoreCase(categoryKey))
            .filter(entry -> !recentlyUsedLines.contains(entry.dialogueKey()))
            .toList();

        if (matching.isEmpty()) {
            matching = entries.stream()
                .filter(entry -> entry.category().equalsIgnoreCase(categoryKey))
                .toList();
        }

        if (matching.isEmpty()) {
            return Optional.empty();
        }

        String selected = weightedRandomSelect(matching);
        markAsUsed(selected);
        return Optional.of(selected);
    }

    /**
     * Perform weighted random selection
     */
    private String weightedRandomSelect(List<SymbioteDialogueEntry> entries) {
        if (entries.size() == 1) {
            return entries.get(0).dialogueKey();
        }

        int totalWeight = entries.stream().mapToInt(SymbioteDialogueEntry::weight).sum();
        int roll = random.nextInt(totalWeight);

        int cumulative = 0;
        for (SymbioteDialogueEntry entry : entries) {
            cumulative += entry.weight();
            if (roll < cumulative) {
                return entry.dialogueKey();
            }
        }

        // Fallback (shouldn't happen)
        return entries.get(0).dialogueKey();
    }

    /**
     * Mark a dialogue as recently used
     */
    private static void markAsUsed(String dialogueKey) {
        if (recentlyUsedLines.size() >= RECENT_LINES_LIMIT) {
            recentlyUsedLines.removeFirst();
        }
        recentlyUsedLines.addLast(dialogueKey);
    }

    /**
     * Clear recently used lines (for testing)
     */
    public static void clearRecentlyUsed() {
        recentlyUsedLines.clear();
    }

    /**
     * Get the number of loaded entries
     */
    public int size() {
        return entries.size();
    }

    /**
     * Check if repository is empty
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Get all entries (for debugging)
     */
    public List<SymbioteDialogueEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }
}
