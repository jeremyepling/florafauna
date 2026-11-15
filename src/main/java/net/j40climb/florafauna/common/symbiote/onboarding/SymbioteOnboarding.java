package net.j40climb.florafauna.common.symbiote.onboarding;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.common.symbiote.progress.ConceptSignal;
import net.j40climb.florafauna.common.symbiote.progress.ProgressSignalTracker;
import net.j40climb.florafauna.common.symbiote.progress.ProgressSignalUpdater;
import net.j40climb.florafauna.common.symbiote.voice.SymbioteVoiceService;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** The first playable route from a wild bloom to an earned adaptation. */
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public final class SymbioteOnboarding {
    public static final String BLOOM_CONTACT = "resonant_bloom_contact";
    public static final String ADAPTIVE_JUMP = "adaptive_jump";
    private static final String JUMP_BIOME_PREFIX = "adaptive_jump_biome:";
    public static final int JUMPS_REQUIRED = 25;
    public static final int BIOMES_REQUIRED = 2;

    private SymbioteOnboarding() {}

    public static boolean hasBloomContact(ServerPlayer player) {
        return player.getData(FloraFaunaRegistry.SYMBIOTE_PROGRESS_ATTACHMENT)
                .getSignal(BLOOM_CONTACT).isPresent();
    }

    public static void onBloomBroken(ServerPlayer player) {
        if (hasBloomContact(player)) {
            return;
        }
        ProgressSignalUpdater.recordObservation(player, BLOOM_CONTACT);
        if (player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA).symbioteState().isBonded()) {
            return;
        }
        SymbioteVoiceService.forceSpeakUnbonded(player, "symbiote.onboarding.first_contact");
        SymbioteVoiceService.forceSpeakUnbonded(player, "symbiote.onboarding.pollen_hint");
    }

    public static void onStewConsumed(ServerPlayer player) {
        if (hasBloomContact(player) && !player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA)
                .symbioteState().isBonded()) {
            SymbioteVoiceService.forceSpeakUnbonded(player, "symbiote.onboarding.chamber_hint");
        }
    }

    public static void onBonded(ServerPlayer player) {
        SymbioteVoiceService.forceSpeak(player, "symbiote.onboarding.bonded_perception");
        SymbioteVoiceService.forceSpeak(player, "symbiote.onboarding.learn_by_doing");
    }

    public static int jumpCount(ProgressSignalTracker progress) {
        return progress.getSignal(ADAPTIVE_JUMP).map(ConceptSignal::interactionCount).orElse(0);
    }

    public static int jumpBiomeCount(ProgressSignalTracker progress) {
        return (int) progress.signals().keySet().stream().filter(key -> key.startsWith(JUMP_BIOME_PREFIX)).count();
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PlayerSymbioteData data = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
        if (!data.symbioteState().areAbilitiesActive() || data.jumpBoost() > 0) {
            return;
        }

        ProgressSignalUpdater.recordObservation(player, ADAPTIVE_JUMP);
        ProgressSignalTracker observedProgress = player.getData(FloraFaunaRegistry.SYMBIOTE_PROGRESS_ATTACHMENT);
        player.level().getBiome(player.blockPosition()).unwrapKey().ifPresent(biome -> {
            String key = JUMP_BIOME_PREFIX + biome.identifier();
            if (observedProgress.getSignal(key).isEmpty()) {
                player.setData(FloraFaunaRegistry.SYMBIOTE_PROGRESS_ATTACHMENT,
                        observedProgress.withSignalUpdated(key, ConceptSignal.firstSeen(key, player.level().getGameTime())));
            }
        });
        ProgressSignalTracker progress = player.getData(FloraFaunaRegistry.SYMBIOTE_PROGRESS_ATTACHMENT);
        if (jumpCount(progress) >= JUMPS_REQUIRED && jumpBiomeCount(progress) >= BIOMES_REQUIRED) {
            player.setData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA, data.withJumpBoost(1));
            SymbioteVoiceService.forceSpeak(player, "symbiote.onboarding.jump_adapted");
        } else if (jumpCount(progress) == 10) {
            SymbioteVoiceService.forceSpeak(player, "symbiote.onboarding.jump_progress");
        } else if (jumpCount(progress) == JUMPS_REQUIRED && jumpBiomeCount(progress) < BIOMES_REQUIRED) {
            SymbioteVoiceService.forceSpeak(player, "symbiote.onboarding.new_biome_hint");
        }
    }

    /** A small nearby spore shimmer is the first, always-on perception cue. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.tickCount % 80 != 0
                || !player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA).symbioteState().isBonded()
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-9, -3, -9), center.offset(9, 3, 9))) {
            if (level.getBlockState(pos).is(FloraFaunaRegistry.RESONANT_BLOOM.get())) {
                level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                        pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                        5, 0.25, 0.3, 0.25, 0.01);
                break;
            }
        }
    }
}
