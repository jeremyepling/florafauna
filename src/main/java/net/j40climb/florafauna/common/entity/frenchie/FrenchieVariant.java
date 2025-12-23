package net.j40climb.florafauna.common.entity.frenchie;

import java.util.Arrays;
import java.util.Comparator;

public enum FrenchieVariant {
    FAWN(0),
    BRINDLE(1);

    private static final FrenchieVariant[] BY_ID = Arrays.stream(values()).sorted(
            Comparator.comparingInt(FrenchieVariant::getId)).toArray(FrenchieVariant[]::new);
    private final int id;

    FrenchieVariant(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static FrenchieVariant byId(int id) {
        return BY_ID[id % BY_ID.length];
    }
}