package net.j40climb.florafauna.common.menu;

import net.j40climb.florafauna.common.block.entity.SymbioteContainmentChamberBlockEntity;
import net.j40climb.florafauna.common.symbiote.ability.SymbioteAbility;
import net.j40climb.florafauna.common.symbiote.ability.SymbioteAbilityData;
import net.minecraft.world.inventory.ContainerData;

/**
 * Container data for syncing symbiote ability progress from server to client.
 * Data layout:
 * - Slot 0: Tick counter (0-19) for progress animation
 * - Slots 1-2: Unlocked abilities as bitflags (20 abilities = 20 bits)
 * - Slots 3-22: Current count for each ability (20 ints)
 * Total: 23 data slots
 */
public class SymbioteContainerData implements ContainerData {
    private static final int DATA_SIZE = 23;
    private static final int TICK_COUNTER_SLOT = 0;
    private static final int UNLOCKED_FLAGS_START = 1;
    private static final int ABILITY_COUNTS_START = 3;

    private final SymbioteContainmentChamberBlockEntity blockEntity;

    public SymbioteContainerData(SymbioteContainmentChamberBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public int get(int index) {


        return 0;
    }

    @Override
    public void set(int index, int value) {
        // Read-only data, no setting needed
    }

    @Override
    public int getCount() {
        return DATA_SIZE;
    }

    /**
     * Packs unlocked status as bitflags.
     * flagIndex 0 = abilities 0-15
     * flagIndex 1 = abilities 16-19 (only 4 bits used)
     */
    private int getUnlockedFlags(SymbioteAbilityData data, int flagIndex) {
        int flags = 0;
        int startAbility = flagIndex * 16;
        int endAbility = Math.min(startAbility + 16, SymbioteAbility.values().length);

        for (int i = startAbility; i < endAbility; i++) {
            SymbioteAbility ability = SymbioteAbility.byId(i);
            if (data.isUnlocked(ability)) {
                flags |= (1 << (i - startAbility));
            }
        }

        return flags;
    }

    /**
     * Helper method to check if an ability is unlocked from the synced data.
     * Used by the client GUI.
     */
    public static boolean isAbilityUnlocked(ContainerData data, int abilityId) {
        int flagIndex = abilityId / 16;
        int bitPosition = abilityId % 16;
        int flags = data.get(UNLOCKED_FLAGS_START + flagIndex);
        return (flags & (1 << bitPosition)) != 0;
    }

    /**
     * Helper method to get ability progress count from the synced data.
     * Used by the client GUI.
     */
    public static int getAbilityProgress(ContainerData data, int abilityId) {
        return data.get(ABILITY_COUNTS_START + abilityId);
    }
}
