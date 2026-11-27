package net.j40climb.florafauna.common.symbiote.onboarding;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.common.symbiote.data.SymbioteData;
import net.j40climb.florafauna.common.symbiote.progress.ConceptSignal;
import net.j40climb.florafauna.common.symbiote.progress.ProgressSignalTracker;
import net.j40climb.florafauna.common.symbiote.progress.ProgressSignalUpdater;
import net.j40climb.florafauna.common.symbiote.progress.SignalState;
import net.j40climb.florafauna.common.symbiote.voice.SymbioteVoiceService;
import net.j40climb.florafauna.platform.AttachmentAccess;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.WeakHashMap;
//? if neoforge {
import net.neoforged.bus.api.SubscribeEvent;
//?}
//? if neoforge {
import net.neoforged.fml.common.EventBusSubscriber;
//?}
//? if neoforge {
import net.neoforged.neoforge.event.entity.living.LivingEvent;
//?}
//? if neoforge {
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
//?}

/** The first playable route from a wild bloom to an earned adaptation. */
//? if neoforge {
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
//?}
public final class SymbioteOnboarding {
    public static final String BLOOM_CONTACT = "resonant_bloom_contact";
    public static final String ADAPTIVE_JUMP = "adaptive_jump";
    private static final String JUMP_BIOME_PREFIX = "adaptive_jump_biome:";
    private static final String ADAPTIVE_SPEED = "adaptive_speed";
    private static final String ADAPTIVE_DASH = "adaptive_dash";
    private static final String ADAPTIVE_FALL = "adaptive_fall";
    public static final int JUMPS_REQUIRED = 25;
    public static final int BIOMES_REQUIRED = 2;
    public static final int SPEED_ONE_CM = 10_000;
    public static final int SPEED_TWO_CM = 50_000;
    public static final int DASH_JUMPS_REQUIRED = 20;
    public static final int FALLS_REQUIRED = 5;
    private static final Map<ServerPlayer, Integer> PENDING_SPRINT_CM = new WeakHashMap<>();

    private SymbioteOnboarding() {}

    public static boolean hasBloomContact(ServerPlayer player) {
        return net.j40climb.florafauna.platform.AttachmentAccess.get(player, FloraFaunaRegistry.SYMBIOTE_PROGRESS_ATTACHMENT)
                .getSignal(BLOOM_CONTACT).isPresent();
    }

    public static void onBloomBroken(ServerPlayer player) {
        if (hasBloomContact(player)) {
            return;
        }
        ProgressSignalUpdater.recordObservation(player, BLOOM_CONTACT);
        if (net.j40climb.florafauna.platform.AttachmentAccess.get(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA).symbioteState().isBonded()) {
            return;
        }
        SymbioteVoiceService.forceSpeakUnbonded(player, "symbiote.onboarding.first_contact");
        SymbioteVoiceService.forceSpeakUnbonded(player, "symbiote.onboarding.pollen_hint");
    }

    public static void onPrepared(ServerPlayer player) {
        if (hasBloomContact(player) && !net.j40climb.florafauna.platform.AttachmentAccess.get(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA)
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

    public static int sprintDistanceCm(ProgressSignalTracker progress) {
        return count(progress, ADAPTIVE_SPEED);
    }

    public static int sprintJumpCount(ProgressSignalTracker progress) {
        return count(progress, ADAPTIVE_DASH);
    }

    public static int damagingLandingCount(ProgressSignalTracker progress) {
        return count(progress, ADAPTIVE_FALL);
    }

    private static int count(ProgressSignalTracker progress, String key) {
        return progress.getSignal(key).map(ConceptSignal::interactionCount).orElse(0);
    }

    // The dedicated movement stat excludes vertical distance, flying and riding.
    public static void onSprintDistance(ServerPlayer player, int centimeters) {
        var data = AttachmentAccess.get(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
        if (centimeters <= 0 || data.speedLevel() >= SymbioteData.MAX_SPEED_LEVEL || !canAdapt(player, data)) return;

        int pending = PENDING_SPRINT_CM.merge(player, Math.min(centimeters, SPEED_TWO_CM), Integer::sum);
        var tracker = AttachmentAccess.get(player, FloraFaunaRegistry.SYMBIOTE_PROGRESS_ATTACHMENT);
        int nextUnlock = data.speedLevel() == 0 ? SPEED_ONE_CM : SPEED_TWO_CM;
        if (sprintDistanceCm(tracker) + pending >= nextUnlock) flushSprintProgress(player);
    }

    public static void flushSprintProgress(ServerPlayer player) {
        Integer pending = PENDING_SPRINT_CM.remove(player);
        if (pending == null) return;

        int total = advanceAdaptation(player, ADAPTIVE_SPEED, pending, SPEED_TWO_CM);
        var data = AttachmentAccess.get(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
        int level = total >= SPEED_TWO_CM ? SymbioteData.MAX_SPEED_LEVEL : total >= SPEED_ONE_CM ? 1 : 0;
        if (level > data.speedLevel() && canAdapt(player, data)) {
            AttachmentAccess.set(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA, data.withSpeedLevel(level));
            SymbioteVoiceService.forceSpeak(player, level == 1
                    ? "symbiote.onboarding.speed_adapted" : "symbiote.onboarding.speed_improved");
        }
    }

    public static void onSurvivedDamagingLanding(ServerPlayer player) {
        var data = AttachmentAccess.get(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
        if (!data.featherFalling() && canAdapt(player, data)
                && advanceAdaptation(player, ADAPTIVE_FALL, 1, FALLS_REQUIRED) >= FALLS_REQUIRED) {
            AttachmentAccess.set(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA, data.withFeatherFalling(true));
            SymbioteVoiceService.forceSpeak(player, "symbiote.onboarding.fall_adapted");
        }
    }

    private static PlayerSymbioteData learnDash(ServerPlayer player, PlayerSymbioteData data) {
        if (!data.dash() && player.isSprinting() && canAdapt(player, data)
                && advanceAdaptation(player, ADAPTIVE_DASH, 1, DASH_JUMPS_REQUIRED) >= DASH_JUMPS_REQUIRED) {
            var adapted = data.withDash(true);
            AttachmentAccess.set(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA, adapted);
            SymbioteVoiceService.forceSpeak(player, "symbiote.onboarding.dash_adapted");
            return adapted;
        }
        return data;
    }

    private static boolean canAdapt(ServerPlayer player, PlayerSymbioteData data) {
        return data.symbioteState().areAbilitiesActive() && !player.isCreative() && !player.isSpectator();
    }

    private static int advanceAdaptation(ServerPlayer player, String key, int amount, int max) {
        var tracker = AttachmentAccess.get(player, FloraFaunaRegistry.SYMBIOTE_PROGRESS_ATTACHMENT);
        int previous = count(tracker, key);
        int total = Math.min(max, previous + amount);
        if (total != previous) {
            long now = player.level().getGameTime();
            var state = total >= max ? SignalState.INTEGRATED : SignalState.SEEN;
            var signal = new ConceptSignal(key, state, now, total, 0);
            AttachmentAccess.set(player, FloraFaunaRegistry.SYMBIOTE_PROGRESS_ATTACHMENT,
                    tracker.withSignalUpdated(key, signal).withProgressTick(now));
        }
        return total;
    }

    //? if neoforge {
    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        onJump(event.getEntity());
    }
    //?}

    public static void onJump(net.minecraft.world.entity.LivingEntity subject) {
        if (!(subject instanceof ServerPlayer player)) {
            return;
        }
        PlayerSymbioteData data = learnDash(player, AttachmentAccess.get(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA));
        if (!data.symbioteState().areAbilitiesActive() || data.jumpBoost() > 0) {
            return;
        }

        ProgressSignalUpdater.recordObservation(player, ADAPTIVE_JUMP);
        ProgressSignalTracker observedProgress = net.j40climb.florafauna.platform.AttachmentAccess.get(player, FloraFaunaRegistry.SYMBIOTE_PROGRESS_ATTACHMENT);
        player.level().getBiome(player.blockPosition()).unwrapKey().ifPresent(biome -> {
            String key = JUMP_BIOME_PREFIX + biome.identifier();
            if (observedProgress.getSignal(key).isEmpty()) {
                net.j40climb.florafauna.platform.AttachmentAccess.set(player, FloraFaunaRegistry.SYMBIOTE_PROGRESS_ATTACHMENT,
                        observedProgress.withSignalUpdated(key, ConceptSignal.firstSeen(key, player.level().getGameTime())));
            }
        });
        ProgressSignalTracker progress = net.j40climb.florafauna.platform.AttachmentAccess.get(player, FloraFaunaRegistry.SYMBIOTE_PROGRESS_ATTACHMENT);
        if (jumpCount(progress) >= JUMPS_REQUIRED && jumpBiomeCount(progress) >= BIOMES_REQUIRED) {
            net.j40climb.florafauna.platform.AttachmentAccess.set(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA, data.withJumpBoost(1));
            SymbioteVoiceService.forceSpeak(player, "symbiote.onboarding.jump_adapted");
        } else if (jumpCount(progress) == 10) {
            SymbioteVoiceService.forceSpeak(player, "symbiote.onboarding.jump_progress");
        } else if (jumpCount(progress) == JUMPS_REQUIRED && jumpBiomeCount(progress) < BIOMES_REQUIRED) {
            SymbioteVoiceService.forceSpeak(player, "symbiote.onboarding.new_biome_hint");
        }
    }

    /** A small nearby spore shimmer is the first, always-on perception cue. */
    //? if neoforge {
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        onPlayerTick(event.getEntity());
    }
    //?}

    public static void onPlayerTick(Player subject) {
        if (!(subject instanceof ServerPlayer player)) return;

        if (player.tickCount % 10 == 0 || !player.isSprinting()) flushSprintProgress(player);
        if (player.tickCount % 80 != 0
                || !net.j40climb.florafauna.platform.AttachmentAccess.get(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA).symbioteState().isBonded()
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
