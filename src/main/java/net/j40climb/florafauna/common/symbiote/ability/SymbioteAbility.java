package net.j40climb.florafauna.common.symbiote.ability;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntFunction;

/**
 * Defines all symbiote abilities that can be unlocked by feeding items.
 * Each ability has an ID, name, required item, and count needed to unlock.
 */
public enum SymbioteAbility implements StringRepresentable {
    // Combat abilities
    VENOM_STRIKE(0, "venom_strike", Items.SPIDER_EYE, 64,
            "Adds poison damage to attacks"),
    TENDRIL_WHIP(1, "tendril_whip", Items.LEAD, 32,
            "Extended melee reach with tendrils"),
    SYMBIOTE_ARMOR(2, "symbiote_armor", Items.IRON_INGOT, 128,
            "Provides damage resistance"),
    RAZOR_CLAWS(3, "razor_claws", Items.FLINT, 48,
            "Increases melee damage"),
    BERSERKER_RAGE(4, "berserker_rage", Items.NETHER_WART, 64,
            "Temporary damage boost when low health"),

    // Movement abilities
    WALL_CRAWL(5, "wall_crawl", Items.SLIME_BALL, 48,
            "Climb walls like a spider"),
    GLIDE(6, "glide", Items.PHANTOM_MEMBRANE, 24,
            "Glide through the air"),
    DASH(7, "dash", Items.SUGAR, 32,
            "Quick burst of speed"),
    LEAP(8, "leap", Items.RABBIT_FOOT, 16,
            "Enhanced jump height"),
    SWIM_SPEED(9, "swim_speed", Items.PRISMARINE_SHARD, 48,
            "Faster swimming"),

    // Defensive abilities
    REGENERATION(10, "regeneration", Items.GHAST_TEAR, 8,
            "Slowly regenerate health"),
    FIRE_RESISTANCE(11, "fire_resistance", Items.MAGMA_CREAM, 32,
            "Resist fire and lava damage"),
    CAMOUFLAGE(12, "camouflage", Items.INK_SAC, 64,
            "Become partially invisible"),
    FALL_IMMUNITY(13, "fall_immunity", Items.FEATHER, 64,
            "Negate fall damage"),
    THORNS(14, "thorns", Items.CACTUS, 48,
            "Reflect damage to attackers"),

    // Utility abilities
    NIGHT_VISION(15, "night_vision", Items.GOLDEN_CARROT, 64,
            "See in the dark"),
    WATER_BREATHING(16, "water_breathing", Items.PUFFERFISH, 16,
            "Breathe underwater"),
    HUNGER_SUPPRESSION(17, "hunger_suppression", Items.GOLDEN_APPLE, 8,
            "Reduced hunger drain"),
    ORE_SENSE(18, "ore_sense", Items.AMETHYST_SHARD, 32,
            "Highlight nearby ores"),
    ENDER_STEP(19, "ender_step", Items.ENDER_PEARL, 16,
            "Short-range teleportation");

    private static final IntFunction<SymbioteAbility> BY_ID = ByIdMap.continuous(
            ability -> ability.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO
    );

    public static final Codec<SymbioteAbility> CODEC = StringRepresentable.fromEnum(SymbioteAbility::values);
    public static final StreamCodec<ByteBuf, SymbioteAbility> STREAM_CODEC =
            ByteBufCodecs.idMapper(BY_ID, ability -> ability.id);

    private final int id;
    private final String name;
    private final Item requiredItem;
    private final int requiredCount;
    private final String description;

    SymbioteAbility(int id, String name, Item requiredItem, int requiredCount, String description) {
        this.id = id;
        this.name = name;
        this.requiredItem = requiredItem;
        this.requiredCount = requiredCount;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public Item getRequiredItem() {
        return requiredItem;
    }

    public int getRequiredCount() {
        return requiredCount;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Gets an ability by its numeric ID.
     */
    public static SymbioteAbility byId(int id) {
        return BY_ID.apply(id);
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }
}