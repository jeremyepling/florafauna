package net.j40climb.florafauna.common.item.abilities.data;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum HammerAction implements StringRepresentable {
    AUTO("auto"), STRIP("strip"), PATH("path"), TILL("till"), THROW("throw");

    public static final Codec<HammerAction> CODEC = StringRepresentable.fromEnum(HammerAction::values);
    public static final StreamCodec<ByteBuf, HammerAction> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
    private final String name;

    HammerAction(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public Component label() {
        return Component.translatable("gui.florafauna.hammer_action." + name);
    }

    public MultiToolAbilityData toolActions(MultiToolAbilityData enabled) {
        return switch (this) {
            case AUTO -> enabled;
            case STRIP -> enabled.strip() ? MultiToolAbilityData.AXE_ONLY : MultiToolAbilityData.NONE;
            case PATH -> enabled.flatten() ? MultiToolAbilityData.SHOVEL_ONLY : MultiToolAbilityData.NONE;
            case TILL -> enabled.till() ? MultiToolAbilityData.HOE_ONLY : MultiToolAbilityData.NONE;
            case THROW -> MultiToolAbilityData.NONE;
        };
    }
}
