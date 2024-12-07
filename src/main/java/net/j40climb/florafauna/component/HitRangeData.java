package net.j40climb.florafauna.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;

public record HitRangeData(Integer range, Integer size) {

    public static final Codec<HitRangeData> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    Codec.INT.fieldOf("range").forGetter(HitRangeData::range), // range of mineable area
                    Codec.INT.fieldOf("size").forGetter(HitRangeData::size) // not used yet
            ).apply(builder, HitRangeData::new));

    @Override
    public int hashCode() {
        return Objects.hash(this.range, this.size);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        } else {
            return obj instanceof HitRangeData(Integer range1, Integer size1)
                    && Objects.equals(this.range, range1)
                    && Objects.equals(this.size, size1);
        }
    }
}
