package net.j40climb.florafauna.common.item.symbiote.event;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.RegisterAttachmentTypes;
import net.j40climb.florafauna.common.item.symbiote.SymbioteData;
import net.j40climb.florafauna.common.item.symbiote.dialogue.SymbioteDialogue;
import net.j40climb.florafauna.common.item.symbiote.dialogue.SymbioteDialogueTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Event handler for symbiote dialogue triggers.
 * Listens to various game events and triggers appropriate symbiote messages.
 */
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public class SymbioteDialogueEvents {

    /**
     * Handle damage events to trigger appropriate dialogue
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

        DamageSource source = event.getSource();

        // Check for freeze damage (cold water via freeze damage)
        if (source.is(DamageTypeTags.IS_FREEZING)) {
            SymbioteDialogue.tryTrigger(player, SymbioteDialogueTrigger.COLD_WATER);
        }
        // Check for fall damage
        else if (source.is(DamageTypeTags.IS_FALL)) {
            SymbioteDialogue.tryTrigger(player, SymbioteDialogueTrigger.FALL_DAMAGE);
        }
        // Check for drowning
        else if (source.is(DamageTypeTags.IS_DROWNING)) {
            SymbioteDialogue.tryTrigger(player, SymbioteDialogueTrigger.DROWNING);
        }
        // Check for mob attack (entity damage from a living entity)
        else if (source.getEntity() != null && source.getEntity() != player) {
            SymbioteDialogue.tryTrigger(player, SymbioteDialogueTrigger.MOB_ATTACK);
        }
    }

    /**
     * Handle player tick events for cold water detection
     * (biome-based detection for cold water without freeze damage)
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        // Only check on server side
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Only check every 20 ticks (1 second) to reduce overhead
        if (serverPlayer.tickCount % 20 != 0) {
            return;
        }

        SymbioteData symbioteData = serverPlayer.getData(RegisterAttachmentTypes.SYMBIOTE_DATA);
        if (!symbioteData.bonded()) {
            return;
        }

        // Check if player is in water in a cold biome
        if (serverPlayer.isInWater()) {
            BlockPos pos = serverPlayer.blockPosition();
            Biome biome = serverPlayer.level().getBiome(pos).value();

            // Check if biome is cold enough to snow at player's position
            if (biome.coldEnoughToSnow(pos, serverPlayer.level().getSeaLevel())) {
                SymbioteDialogue.tryTrigger(serverPlayer, SymbioteDialogueTrigger.COLD_WATER);
            }
        }
    }

    /**
     * Handle sleep events - triggers when player wakes up from sleeping
     */
    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        Player player = event.getEntity();

        // Only process on server side
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        SymbioteData symbioteData = serverPlayer.getData(RegisterAttachmentTypes.SYMBIOTE_DATA);
        if (!symbioteData.bonded()) {
            return;
        }

        // Trigger dialogue when player wakes up from sleep
        SymbioteDialogue.tryTrigger(serverPlayer, SymbioteDialogueTrigger.SLEEPING);
    }
}
