package net.j40climb.florafauna.common.item.energyhammer;

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

public enum MiningShape implements StringRepresentable {
    SINGLE(0, "single", 0),
    FLAT_3X3(1, "flat_3x3", 1),
    FLAT_5X5(2, "flat_5x5", 2),
    FLAT_7X7(3, "flat_7x7", 3),
    SHAPELESS(4, "shapeless", 1),
    TUNNEL_UP(5, "stairs_up", 0),
    TUNNEL_DOWN(6, "stairs_down", 0);

    private static final IntFunction<MiningShape> BY_ID = ByIdMap.continuous(
            p_348119_ -> p_348119_.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO
    );
    static final Codec<MiningShape> CODEC = StringRepresentable.fromEnum(MiningShape::values);
    static final StreamCodec<ByteBuf, MiningShape> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, p_348120_ -> p_348120_.id);

    private final int id;
    private final int radius;
    private final String name;

    MiningShape(int id, String name, int radius) {
        this.name = name;
        this.id = id;
        this.radius = radius;
    }

    // Static map for lookup by id
    private static final Map<Integer, MiningShape> shapeByID = new HashMap<>();

    // Static block to populate the map
    static {
        for (MiningShape shape : values()) {
            shapeByID.put(shape.id, shape);
        }
    }

    // TODO I think I can delete this
    public int getId() {
        return this.id;
    }

    public int getRadius() { return this.radius; }

    public static MiningShape getShapeByID(int id) {
        return shapeByID.get(id);
    }

    public int id() {
        return this.id;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.name;
    }
}
