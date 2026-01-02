package net.j40climb.florafauna.common.item.abilities.data;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;

public enum MiningSpeed implements StringRepresentable {
    STANDARD(0, "standard"),
    EFFICIENCY(1, "efficiency"),
    INSTABREAK(2, "instabreak");

    private static final IntFunction<MiningSpeed> BY_ID = ByIdMap.continuous(
            p_348119_ -> p_348119_.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO
    );
    public static final Codec<MiningSpeed> CODEC = StringRepresentable.fromEnum(MiningSpeed::values);
    public static final StreamCodec<ByteBuf, MiningSpeed> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, m -> m.id);

    private final int id;
    private final String name;

    MiningSpeed(int id, String name) {
        this.name = name;
        this.id = id;
    }

    private static final Map<Integer, MiningSpeed> speedByID = new HashMap<>();

    static {
        for (MiningSpeed shape : values()) {
            speedByID.put(shape.id, shape);
        }
    }

    public int id() {
        return this.id;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.name;
    }
}
