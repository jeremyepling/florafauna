package net.j40climb.florafauna.common.block.husk;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.item.symbiote.PlayerSymbioteData;
import net.j40climb.florafauna.common.item.symbiote.SymbioteState;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Client-side particle handler for Restoration Husks.
 *
 * Spawns firefly-style glow particles around the restoration husk
 * when the player is in BONDED_WEAKENED state, guiding them back
 * to recover their items and abilities.
 *
 * Only spawns particles when:
 * - Player is BONDED_WEAKENED
 * - Restoration husk is active
 * - Same dimension as husk
 * - Within render distance (64 blocks)
 */
@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
public class HuskParticleHandler {

    private static final double MAX_PARTICLE_DISTANCE = 64.0;
    private static final double FIREFLY_CHANCE_PER_TICK = 0.7; // Match FireflyBushBlock

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }

        Level level = player.level();
        if (level == null || !level.isClientSide()) {
            return;
        }

        PlayerSymbioteData data = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);

        // Only show particles if player is weakened and has an active restoration husk
        if (data.symbioteState() != SymbioteState.BONDED_WEAKENED) {
            return;
        }

        if (!data.restorationHuskActive()) {
            return;
        }

        BlockPos huskPos = data.restorationHuskPos();
        if (huskPos == null) {
            return;
        }

        // Check same dimension
        if (data.restorationHuskDim() != null &&
                !level.dimension().equals(data.restorationHuskDim())) {
            return;
        }

        // Check distance
        double distance = player.blockPosition().distSqr(huskPos);
        if (distance > MAX_PARTICLE_DISTANCE * MAX_PARTICLE_DISTANCE) {
            return;
        }

        // Spawn particles with random chance (matching FireflyBushBlock's 70% chance)
        RandomSource random = level.getRandom();
        if (random.nextDouble() > FIREFLY_CHANCE_PER_TICK) {
            return;
        }

        spawnHuskParticles(level, huskPos, random);
    }

    /**
     * Spawn firefly particles around the husk position, matching FireflyBushBlock behavior.
     */
    private static void spawnHuskParticles(Level level, BlockPos huskPos, RandomSource random) {
        // Match FireflyBushBlock: 10 block horizontal range, 5 block vertical range
        double x = huskPos.getX() + random.nextDouble() * 10.0 - 5.0;
        double y = huskPos.getY() + random.nextDouble() * 5.0;
        double z = huskPos.getZ() + random.nextDouble() * 10.0 - 5.0;

        // FireflyBushBlock uses zero velocity - the particle handles its own movement
        level.addParticle(ParticleTypes.FIREFLY, x, y, z, 0.0, 0.0, 0.0);
    }
}
