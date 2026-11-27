package net.j40climb.florafauna.common.symbiote.abilities;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
//? if neoforge {
import net.neoforged.bus.api.SubscribeEvent;
//?}
//? if neoforge {
import net.neoforged.fml.common.EventBusSubscriber;
//?}
//? if neoforge {
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
//?}
//? if neoforge {
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
//?}

import java.util.List;

/**
 * Event handler for symbiote abilities.
 * Handles creeper detection, passive stat bonuses, and fall damage negation.
 */
//? if neoforge {
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
//?}
public class SymbioteAbilityEvents {

    /**
     * Handle player tick events for abilities that need periodic checking
     */
    //? if neoforge {
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        onPlayerTick(event.getEntity());
    }
    //?}

    public static void onPlayerTick(net.minecraft.world.entity.player.Player subject) {
        Player player = subject;

        // Only process on server side
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        PlayerSymbioteData symbioteData = net.j40climb.florafauna.platform.AttachmentAccess.get(serverPlayer, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
        SymbiotePassiveStats.updateAttributes(serverPlayer, symbioteData);
        if (!symbioteData.symbioteState().areAbilitiesActive()) {
            return;
        }

        // Check every 20 ticks (1 second) for performance
        if (serverPlayer.tickCount % 20 == 0) {
            // Creeper detection
            detectNearbyCreepers(serverPlayer);
        }
    }

    /**
     * Detects creepers within 10 blocks and warns the player
     */
    private static void detectNearbyCreepers(ServerPlayer player) {
        // Create bounding box 10 blocks in all directions
        AABB searchBox = player.getBoundingBox().inflate(10.0);

        // Find all creepers in range
        List<Creeper> nearbyCreeepers = player.level().getEntitiesOfClass(
                Creeper.class,
                searchBox,
                creeper -> creeper.isAlive() && !creeper.isRemoved()
        );

        if (!nearbyCreeepers.isEmpty()) {
            Creeper closestCreeper = nearbyCreeepers.get(0);
            double closestDistance = player.distanceTo(closestCreeper);

            // Find the closest creeper
            for (Creeper creeper : nearbyCreeepers) {
                double distance = player.distanceTo(creeper);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestCreeper = creeper;
                }
            }

            // Send warning message
            Component direction = Component.translatable("direction.florafauna." + getDirectionToEntity(player, closestCreeper));
            player.sendOverlayMessage(
                    Component.translatable("message.florafauna.creeper_detected", direction,
                            String.format(java.util.Locale.ROOT, "%.1f", closestDistance))
                            .withStyle(style -> style.withColor(0xFF6B6B))
            );
        }
    }

    /**
     * Gets a rough directional description (N, S, E, W, etc.) from player to entity
     */
    private static String getDirectionToEntity(Player player, Creeper creeper) {
        double dx = creeper.getX() - player.getX();
        double dz = creeper.getZ() - player.getZ();

        // Determine primary direction
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? "east" : "west";
        } else {
            return dz > 0 ? "south" : "north";
        }
    }

    /**
     * Handle fall damage to negate it when featherFalling is enabled
     */
    //? if neoforge {
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        event.setNewDamage(onLivingDamage(event.getEntity(), event.getSource(), event.getNewDamage()));
    }
    //?}

    public static float onLivingDamage(net.minecraft.world.entity.LivingEntity subject, net.minecraft.world.damagesource.DamageSource source, float damage) {
        // Only process for server-side players with bonded symbiotes
        if (!(subject instanceof ServerPlayer player)) {
            return damage;
        }

        PlayerSymbioteData symbioteData = net.j40climb.florafauna.platform.AttachmentAccess.get(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
        if (!symbioteData.symbioteState().areAbilitiesActive()) {
            return damage;
        }

        // Check if this is fall damage and featherFalling is enabled
        if (symbioteData.featherFalling() && source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
            // Cancel the fall damage
            return 0;
        }
        return damage;
    }
}
