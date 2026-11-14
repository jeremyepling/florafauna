package net.j40climb.florafauna.common.entity.frenchie;

import net.minecraft.util.ByIdMap;

import java.util.function.IntFunction;

public enum FrenchieVariant {
    FAWN(0),
    BRINDLE(1);

    private static final IntFunction<FrenchieVariant> BY_ID = ByIdMap.continuous(
            FrenchieVariant::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    private final int id;

    FrenchieVariant(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static FrenchieVariant byId(int id) {
        return BY_ID.apply(id);
    }
}
