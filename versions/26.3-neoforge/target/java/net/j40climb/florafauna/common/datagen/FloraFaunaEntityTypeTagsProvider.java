package net.j40climb.florafauna.common.datagen;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.setup.FloraFaunaTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityTypeIds;
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
                .add(EntityTypeIds.ELDER_GUARDIAN)
                .add(EntityTypeIds.WARDEN);

        // Allowlist for Level 2 MobSymbiote eligibility (future feature)
        tag(FloraFaunaTags.EntityTypes.MOB_SYMBIOTE_LEVEL2_ELIGIBLE)
                // Include vanilla entity type tags
                .addTag(EntityTypeTags.RAIDERS)
                .addTag(EntityTypeTags.SKELETONS)
                .addTag(EntityTypeTags.ZOMBIES)
                .addTag(EntityTypeTags.ARTHROPOD)
                // Passive animals
                .add(EntityTypeIds.COW)
                .add(EntityTypeIds.PIG)
                .add(EntityTypeIds.SHEEP)
                .add(EntityTypeIds.CHICKEN)
                .add(EntityTypeIds.RABBIT)
                .add(EntityTypeIds.HORSE)
                .add(EntityTypeIds.DONKEY)
                .add(EntityTypeIds.MULE)
                .add(EntityTypeIds.LLAMA)
                .add(EntityTypeIds.TRADER_LLAMA)
                .add(EntityTypeIds.MOOSHROOM)
                .add(EntityTypeIds.GOAT)
                .add(EntityTypeIds.CAMEL)
                .add(EntityTypeIds.SNIFFER)
                .add(EntityTypeIds.ARMADILLO)
                // Tameable/friendly mobs
                .add(EntityTypeIds.CAT)
                .add(EntityTypeIds.WOLF)
                .add(EntityTypeIds.OCELOT)
                .add(EntityTypeIds.FOX)
                .add(EntityTypeIds.PARROT)
                .add(EntityTypeIds.PANDA)
                .add(EntityTypeIds.POLAR_BEAR)
                .add(EntityTypeIds.ALLAY)
                // Aquatic mobs
                .add(EntityTypeIds.TURTLE)
                .add(EntityTypeIds.FROG)
                .add(EntityTypeIds.TADPOLE)
                .add(EntityTypeIds.AXOLOTL)
                .add(EntityTypeIds.DOLPHIN)
                .add(EntityTypeIds.SQUID)
                .add(EntityTypeIds.GLOW_SQUID)
                .add(EntityTypeIds.COD)
                .add(EntityTypeIds.SALMON)
                .add(EntityTypeIds.TROPICAL_FISH)
                .add(EntityTypeIds.PUFFERFISH)
                // Nether mobs
                .add(EntityTypeIds.BAT)
                .add(EntityTypeIds.STRIDER)
                .add(EntityTypeIds.HOGLIN)
                .add(EntityTypeIds.ZOGLIN)
                .add(EntityTypeIds.PIGLIN)
                .add(EntityTypeIds.PIGLIN_BRUTE)
                // Villagers/golems
                .add(EntityTypeIds.VILLAGER)
                .add(EntityTypeIds.WANDERING_TRADER)
                .add(EntityTypeIds.IRON_GOLEM)
                .add(EntityTypeIds.SNOW_GOLEM)
                // Hostile mobs
                .add(EntityTypeIds.CREEPER)
                .add(EntityTypeIds.ENDERMAN)
                .add(EntityTypeIds.SLIME)
                .add(EntityTypeIds.MAGMA_CUBE)
                .add(EntityTypeIds.BLAZE)
                .add(EntityTypeIds.GHAST)
                .add(EntityTypeIds.GUARDIAN)
                .add(EntityTypeIds.ELDER_GUARDIAN)
                .add(EntityTypeIds.SHULKER)
                .add(EntityTypeIds.PHANTOM)
                .add(EntityTypeIds.VEX)
                .add(EntityTypeIds.WARDEN)
                .add(EntityTypeIds.BREEZE);

        // Mobs that can experience fear in the fear ecosystem
        // Creepers fear cats/ocelots, endermen fear staring faces and reflections
        // Blazes fear snow golems and cold environments
        tag(FloraFaunaTags.EntityTypes.FEARFUL_MOBS)
                .add(EntityTypeIds.CREEPER)
                .add(EntityTypeIds.ENDERMAN)
                .add(EntityTypeIds.BLAZE);
    }
}
