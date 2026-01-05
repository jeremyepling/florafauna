package net.j40climb.florafauna.common.block.mobtransport;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Buffer for captured mob tickets (queue storage).
 * Similar to VacuumBuffer but for mobs instead of items.
 * <p>
 * Each captured mob is stored as a CapturedMobTicket containing
 * full NBT data for faithful reconstruction at the destination.
 */
public class CapturedMobBuffer {
    private static final String KEY_TICKETS = "captured_mobs";

    private final List<CapturedMobTicket> tickets;
    private final int maxSize;

    /**
     * Creates a new buffer with the specified capacity.
     *
     * @param maxSize Maximum number of mobs to hold
     */
    public CapturedMobBuffer(int maxSize) {
        this.maxSize = maxSize;
        this.tickets = new ArrayList<>();
    }

    /**
     * Checks if the buffer can accept another mob.
     *
     * @return true if not at capacity
     */
    public boolean canAccept() {
        return tickets.size() < maxSize;
    }

    /**
     * Checks if the buffer is empty.
     *
     * @return true if no mobs in queue
     */
    public boolean isEmpty() {
        return tickets.isEmpty();
    }

    /**
     * Checks if the buffer is full.
     *
     * @return true if at capacity
     */
    public boolean isFull() {
        return tickets.size() >= maxSize;
    }

    /**
     * Returns the number of mobs in the queue.
     */
    public int size() {
        return tickets.size();
    }

    /**
     * Returns the maximum queue size.
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Adds a ticket to the buffer.
     *
     * @param ticket The captured mob ticket
     * @return true if added successfully
     */
    public boolean add(CapturedMobTicket ticket) {
        if (canAccept()) {
            tickets.add(ticket);
            return true;
        }
        return false;
    }

    /**
     * Gets the first ticket that is ready for release.
     *
     * @param currentTick The current game tick
     * @return The first ready ticket, or empty if none ready
     */
    public Optional<CapturedMobTicket> getReadyTicket(long currentTick) {
        return tickets.stream()
                .filter(t -> t.isReady(currentTick))
                .findFirst();
    }

    /**
     * Gets the next ticket ready for release, prioritizing the oldest.
     *
     * @param currentTick The current game tick
     * @return The oldest ready ticket, or empty if none ready
     */
    public Optional<CapturedMobTicket> pollReadyTicket(long currentTick) {
        for (int i = 0; i < tickets.size(); i++) {
            CapturedMobTicket ticket = tickets.get(i);
            if (ticket.isReady(currentTick)) {
                tickets.remove(i);
                return Optional.of(ticket);
            }
        }
        return Optional.empty();
    }

    /**
     * Removes a specific ticket from the buffer.
     *
     * @param ticket The ticket to remove
     * @return true if removed
     */
    public boolean remove(CapturedMobTicket ticket) {
        return tickets.remove(ticket);
    }

    /**
     * Gets all tickets (read-only copy).
     *
     * @return Copy of the ticket list
     */
    public List<CapturedMobTicket> getTickets() {
        return new ArrayList<>(tickets);
    }

    /**
     * Clears all tickets from the buffer.
     */
    public void clear() {
        tickets.clear();
    }

    /**
     * Gets the time until the next release in ticks, or -1 if empty.
     *
     * @param currentTick The current game tick
     * @return Ticks until next release, or -1 if no pending releases
     */
    public long getNextReleaseEta(long currentTick) {
        long minEta = Long.MAX_VALUE;
        for (CapturedMobTicket ticket : tickets) {
            long eta = ticket.readyAtTick() - currentTick;
            if (eta < minEta) {
                minEta = eta;
            }
        }
        return minEta == Long.MAX_VALUE ? -1 : Math.max(0, minEta);
    }

    // ==================== SERIALIZATION ====================

    /**
     * Saves the buffer contents using ValueOutput.
     *
     * @param output The value output to write to
     */
    public void serialize(ValueOutput output) {
        output.store(KEY_TICKETS, CapturedMobTicket.CODEC.listOf(), tickets);
    }

    /**
     * Loads buffer contents using ValueInput.
     *
     * @param input The value input to read from
     */
    public void deserialize(ValueInput input) {
        tickets.clear();
        input.read(KEY_TICKETS, CapturedMobTicket.CODEC.listOf())
                .ifPresent(tickets::addAll);
    }
}
