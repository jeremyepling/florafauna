package net.j40climb.florafauna.common.item.symbiote.event;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.RegisterAttachmentTypes;
import net.j40climb.florafauna.common.item.symbiote.SymbioteData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Event handler for symbiote abilities.
 * Handles creeper detection, speed effect, fall damage negation, and jump boost.
 */
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public class SymbioteAbilityEvents {

    // Track jump state for each player: UUID -> ticks since jump started
    private static final Map<UUID, Integer> jumpTicksMap = new HashMap<>();
    // Track whether player is holding jump key: UUID -> isJumping
    private static final Map<UUID, Boolean> playerJumpingMap = new HashMap<>();

    /**
     * Set the jump state for a player (called from network packet handler)
     */
    public static void setPlayerJumping(ServerPlayer player, boolean isJumping) {
        playerJumpingMap.put(player.getUUID(), isJumping);
    }

    /**
     * Get whether a player is currently holding the jump key
     */
    private static boolean isPlayerJumping(ServerPlayer player) {
        return playerJumpingMap.getOrDefault(player.getUUID(), false);
    }

    /**
     * Handle player tick events for abilities that need periodic checking
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        // Only process on server side
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        SymbioteData symbioteData = serverPlayer.getData(RegisterAttachmentTypes.SYMBIOTE_DATA);
        if (!symbioteData.bonded()) {
            return;
        }

        // Check every 20 ticks (1 second) for performance
        if (serverPlayer.tickCount % 20 == 0) {
            // Creeper detection
            detectNearbyCreepers(serverPlayer);
        }

        // Apply speed effect continuously when enabled
        if (symbioteData.speed()) {
            applySpeedEffect(serverPlayer);
        }

        // Handle variable jump boost
        handleVariableJump(serverPlayer, symbioteData);
    }

    /**
     * Handle jump event to initialize jump tracking
     */
    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        SymbioteData symbioteData = player.getData(RegisterAttachmentTypes.SYMBIOTE_DATA);
        if (!symbioteData.bonded() || symbioteData.jumpHeight() <= 0) {
            return;
        }

        // Initialize jump tracking
        jumpTicksMap.put(player.getUUID(), 0);
    }

    /**
     * Handles variable jump height based on how long jump is held
     * Max jump height: 4 blocks (jumpHeight determines strength)
     */
    private static void handleVariableJump(ServerPlayer player, SymbioteData symbioteData) {
        UUID playerUUID = player.getUUID();

        // Only process if player has jump height ability
        if (symbioteData.jumpHeight() <= 0) {
            jumpTicksMap.remove(playerUUID);
            return;
        }

        // Check if player is currently in a jump
        if (!jumpTicksMap.containsKey(playerUUID)) {
            return;
        }

        int jumpTicks = jumpTicksMap.get(playerUUID);

        // If player is on ground AND has been airborne for at least 1 tick, reset jump tracking
        // This prevents immediately canceling the jump
        if (player.onGround() && jumpTicks > 0) {
            jumpTicksMap.remove(playerUUID);
            return;
        }

        // If player released jump key, stop boosting immediately
        if (!isPlayerJumping(player)) {
            jumpTicksMap.remove(playerUUID);
            return;
        }

        // Calculate boost based on jumpHeight (max 4 blocks)
        // Normal jump is ~1.25 blocks, we want up to 4 blocks total
        // Apply boost for up to 15 ticks (0.75 seconds) to allow reaching 4 blocks
        int maxBoostTicks = 15;

        if (jumpTicks < maxBoostTicks) {
            // Scale boost by jumpHeight (1-4 blocks max)
            // Higher jumpHeight = stronger boost per tick
            double boostPerTick = (symbioteData.jumpHeight() / 4.0) * 0.065;

            // Get current movement direction (horizontal only - x and z)
            Vec3 currentMovement = player.getDeltaMovement();
            double horizontalSpeed = Math.sqrt(currentMovement.x * currentMovement.x + currentMovement.z * currentMovement.z);

            // Apply upward velocity boost
            double newY = currentMovement.y + boostPerTick;

            // Apply horizontal boost in the direction player is moving (if moving)
            double newX = currentMovement.x;
            double newZ = currentMovement.z;

            if (horizontalSpeed > 0.001) {
                // Normalize horizontal direction and apply boost
                // Boost scales proportionally with vertical boost divided by 7 so it's more high and not a dash
                double horizontalBoost = boostPerTick / 7;
                double directionX = currentMovement.x / horizontalSpeed;
                double directionZ = currentMovement.z / horizontalSpeed;

                newX += directionX * horizontalBoost;
                newZ += directionZ * horizontalBoost;
            }

            player.setDeltaMovement(newX, newY, newZ);
            ((ServerPlayer) player).connection.send(new ClientboundSetEntityMotionPacket(player));
            player.resetFallDistance();
            jumpTicksMap.put(playerUUID, jumpTicks + 1);
        } else {
            // Max boost reached, stop tracking
            jumpTicksMap.remove(playerUUID);
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
            String direction = getDirectionToEntity(player, closestCreeper);
            player.displayClientMessage(
                    Component.literal("⚠ Symbiote: Creeper detected " + direction + " - " +
                            String.format("%.1f", closestDistance) + " blocks away!")
                            .withStyle(style -> style.withColor(0xFF6B6B)),
                    true // actionBar = true for less intrusive message
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
            return dx > 0 ? "to the East" : "to the West";
        } else {
            return dz > 0 ? "to the South" : "to the North";
        }
    }

    /**
     * Applies Speed 2 effect to the player
     */
    private static void applySpeedEffect(ServerPlayer player) {
        // Apply Speed 2 for 2 seconds (40 ticks), refresh every tick
        // Using ambient=true and showParticles=false for a cleaner effect
        MobEffectInstance speedEffect = new MobEffectInstance(
                MobEffects.SPEED,
                40,    // duration: 2 seconds
                1,     // amplifier: 1 = Speed 2
                true,  // ambient
                false  // showParticles
        );
        player.addEffect(speedEffect);
    }

    /**
     * Handle fall damage to negate it when featherFalling is enabled
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        // Only process for server-side players with bonded symbiotes
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        SymbioteData symbioteData = player.getData(RegisterAttachmentTypes.SYMBIOTE_DATA);
        if (!symbioteData.bonded()) {
            return;
        }

        // Check if this is fall damage and featherFalling is enabled
        if (symbioteData.featherFalling() && event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
            // Cancel the fall damage
            event.setNewDamage(0);
        }
    }
}
