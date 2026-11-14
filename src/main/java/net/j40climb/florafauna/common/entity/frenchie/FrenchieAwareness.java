package net.j40climb.florafauna.common.entity.frenchie;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariants;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public final class FrenchieAwareness {
    public static final int WARNING_TICKS = 30;
    public static final int COOLDOWN_TICKS = 160;
    private static final int WARNING_RADIUS = 12;
    private final FrenchieEntity dog;
    private long lastWarning = -COOLDOWN_TICKS;
    private @Nullable Creeper threat;

    public FrenchieAwareness(FrenchieEntity dog) {
        this.dog = dog;
    }

    public void tick() {
        if (!(dog.level() instanceof ServerLevel level) || !dog.isTame() || dog.isFrenchieSleeping()) return;
        long age = level.getGameTime() - lastWarning;
        if (age == 5) bark(dog, 1.18F);
        if (age < WARNING_TICKS && threat != null && threat.isAlive()) {
            dog.getLookControl().setLookAt(threat, 45, 45);
            return;
        }
        threat = null;
        if (age < COOLDOWN_TICKS || dog.tickCount % 8 != 0) return;
        threat = findCreeper(level, dog);
        if (threat != null) {
            lastWarning = level.getGameTime();
            dog.getLookControl().setLookAt(threat, 45, 45);
            bark(dog, 1.4F);
        }
    }

    public boolean isWarning() {
        return !dog.isFrenchieSleeping() && dog.level().getGameTime() - lastWarning < WARNING_TICKS;
    }

    public boolean isBedtimeBuddy() {
        return dog.getOwner() instanceof Player owner && besideSleepingOwner(dog, owner);
    }

    private static boolean besideSleepingOwner(FrenchieEntity dog, Player owner) {
        return dog.isAlive() && !dog.isInWater() && owner.isSleeping() && dog.isOwnedBy(owner)
                && owner.getSleepingPos().filter(pos -> pos.getCenter().distanceToSqr(dog.position()) <= 9).isPresent();
    }

    @SubscribeEvent
    public static void onWake(PlayerWakeUpEvent event) {
        var owner = event.getEntity();
        // The level calls stopSleepInBed(false, false) when it finishes the night's sleep.
        if (!(owner.level() instanceof ServerLevel level) || event.wakeImmediately() || event.updateLevel()
                || !owner.isSleepingLongEnough()) return;
        boolean hasBuddy = !level.getEntitiesOfClass(FrenchieEntity.class, owner.getBoundingBox().inflate(4),
                dog -> dog.isFrenchieSleeping() && besideSleepingOwner(dog, owner)).isEmpty();
        if (hasBuddy) owner.addEffect(new MobEffectInstance(FloraFaunaRegistry.WELL_RESTED, 20 * 120, 0, false, false, true));
    }

    public static @Nullable Creeper findCreeper(ServerLevel level, LivingEntity observer) {
        return level.getEntitiesOfClass(Creeper.class, observer.getBoundingBox().inflate(WARNING_RADIUS),
                creeper -> creeper.isAlive() && observer.distanceToSqr(creeper) <= WARNING_RADIUS * WARNING_RADIUS
                        && observer.hasLineOfSight(creeper)).stream()
                .min(Comparator.comparingDouble(observer::distanceToSqr)).orElse(null);
    }

    public static void bark(LivingEntity observer, float pitch) {
        var bark = observer.registryAccess().lookupOrThrow(Registries.WOLF_SOUND_VARIANT)
                .getOrThrow(WolfSoundVariants.PUGLIN).value().adultSounds().ambientSound().value();
        observer.level().playSound(null, observer.getX(), observer.getY(), observer.getZ(), bark, SoundSource.NEUTRAL, 0.85F, pitch);
    }
}
