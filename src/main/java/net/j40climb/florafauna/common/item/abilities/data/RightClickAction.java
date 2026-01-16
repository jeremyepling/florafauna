package net.j40climb.florafauna.common.item.abilities.data;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * Stores which ability component should be triggered on right-click.
 * References an existing ability component by its Identifier.
 */
public record RightClickAction(Identifier abilityId) {

    public static final Codec<RightClickAction> CODEC = Identifier.CODEC
            .xmap(RightClickAction::new, RightClickAction::abilityId);

    public static final StreamCodec<ByteBuf, RightClickAction> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(
                    str -> new RightClickAction(Identifier.parse(str)),
                    action -> action.abilityId().toString()
            );

    /**
     * No action on right-click.
     */
    public static final RightClickAction NONE = new RightClickAction(Identifier.parse("florafauna:none"));
}
