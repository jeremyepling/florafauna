package net.j40climb.florafauna.common.item.symbiote;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * Represents the state of a player's symbiote bond.
 *
 * State transitions:
 * - Drink symbiote_stew → UNBOUND → READY_TO_BIND
 * - Bind in Cocoon → READY_TO_BIND → BONDED_ACTIVE
 * - Die while active → BONDED_ACTIVE → BONDED_WEAKENED
 * - Interact with Restoration Husk → BONDED_WEAKENED → BONDED_ACTIVE
 */
public enum SymbioteState implements StringRepresentable {
    UNBOUND("unbound"),
    READY_TO_BIND("ready_to_bind"),
    BONDED_ACTIVE("bonded_active"),
    BONDED_WEAKENED("bonded_weakened");

    public static final Codec<SymbioteState> CODEC = StringRepresentable.fromEnum(SymbioteState::values);
    public static final StreamCodec<ByteBuf, SymbioteState> STREAM_CODEC =
            ByteBufCodecs.idMapper(i -> values()[i], SymbioteState::ordinal);

    private final String name;

    SymbioteState(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /**
     * Returns true if the player has a bonded symbiote (active or weakened).
     */
    public boolean isBonded() {
        return this == BONDED_ACTIVE || this == BONDED_WEAKENED;
    }

    /**
     * Returns true if symbiote abilities are currently active.
     * Abilities are disabled while in BONDED_WEAKENED state.
     */
    public boolean areAbilitiesActive() {
        return this == BONDED_ACTIVE;
    }
}
