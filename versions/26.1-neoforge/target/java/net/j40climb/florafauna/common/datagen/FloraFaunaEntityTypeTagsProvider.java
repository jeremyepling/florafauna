package net.j40climb.florafauna.common.datagen;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.setup.FloraFaunaTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.Tags;

import java.util.concurrent.CompletableFuture;

public class FloraFaunaEntityTypeTagsProvider extends EntityTypeTagsProvider {

    public FloraFaunaEntityTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, FloraFauna.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Deny list: Mobs that can NEVER receive a MobSymbiote
        tag(FloraFaunaTags.EntityTypes.MOB_SYMBIOTE_EXCLUDED)
                .addTag(Tags.EntityTypes.BOSSES)  // neoforge:bosses (Wither, Ender Dragon)
                .add(EntityType.ELDER_GUARDIAN)
                .add(EntityType.WARDEN);

        // Allowlist for Level 2 MobSymbiote eligibility (future feature)
        tag(FloraFaunaTags.EntityTypes.MOB_SYMBIOTE_LEVEL2_ELIGIBLE)
                // Include vanilla entity type tags
                .addTag(EntityTypeTags.RAIDERS)
                .addTag(EntityTypeTags.SKELETONS)
                .addTag(EntityTypeTags.ZOMBIES)
                .addTag(EntityTypeTags.ARTHROPOD)
                // Passive animals
                .add(EntityType.COW)
                .add(EntityType.PIG)
                .add(EntityType.SHEEP)
                .add(EntityType.CHICKEN)
                .add(EntityType.RABBIT)
                .add(EntityType.HORSE)
                .add(EntityType.DONKEY)
                .add(EntityType.MULE)
                .add(EntityType.LLAMA)
                .add(EntityType.TRADER_LLAMA)
                .add(EntityType.MOOSHROOM)
                .add(EntityType.GOAT)
                .add(EntityType.CAMEL)
                .add(EntityType.SNIFFER)
                .add(EntityType.ARMADILLO)
                // Tameable/friendly mobs
                .add(EntityType.CAT)
                .add(EntityType.WOLF)
                .add(EntityType.OCELOT)
                .add(EntityType.FOX)
                .add(EntityType.PARROT)
                .add(EntityType.PANDA)
                .add(EntityType.POLAR_BEAR)
                .add(EntityType.ALLAY)
                // Aquatic mobs
                .add(EntityType.TURTLE)
                .add(EntityType.FROG)
                .add(EntityType.TADPOLE)
                .add(EntityType.AXOLOTL)
                .add(EntityType.DOLPHIN)
                .add(EntityType.SQUID)
                .add(EntityType.GLOW_SQUID)
                .add(EntityType.COD)
                .add(EntityType.SALMON)
                .add(EntityType.TROPICAL_FISH)
                .add(EntityType.PUFFERFISH)
                // Nether mobs
                .add(EntityType.BAT)
                .add(EntityType.STRIDER)
                .add(EntityType.HOGLIN)
                .add(EntityType.ZOGLIN)
                .add(EntityType.PIGLIN)
                .add(EntityType.PIGLIN_BRUTE)
                // Villagers/golems
                .add(EntityType.VILLAGER)
                .add(EntityType.WANDERING_TRADER)
                .add(EntityType.IRON_GOLEM)
                .add(EntityType.SNOW_GOLEM)
                // Hostile mobs
                .add(EntityType.CREEPER)
                .add(EntityType.ENDERMAN)
                .add(EntityType.SLIME)
                .add(EntityType.MAGMA_CUBE)
                .add(EntityType.BLAZE)
                .add(EntityType.GHAST)
                .add(EntityType.GUARDIAN)
                .add(EntityType.ELDER_GUARDIAN)
                .add(EntityType.SHULKER)
                .add(EntityType.PHANTOM)
                .add(EntityType.VEX)
                .add(EntityType.WARDEN)
                .add(EntityType.BREEZE);

        // Mobs that can experience fear in the fear ecosystem
        // Creepers fear cats/ocelots, endermen fear staring faces and reflections
        // Blazes fear snow golems and cold environments
        tag(FloraFaunaTags.EntityTypes.FEARFUL_MOBS)
                .add(EntityType.CREEPER)
                .add(EntityType.ENDERMAN)
                .add(EntityType.BLAZE);
    }
}
