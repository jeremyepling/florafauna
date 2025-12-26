package net.j40climb.florafauna.common.entity.frontpack;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.RegisterAttachmentTypes;
import net.j40climb.florafauna.common.entity.frenchie.FrenchieEntity;
import net.j40climb.florafauna.common.entity.frontpack.networking.PutDownFrenchiePayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Event handler for Frenchie frontpack pickup/putdown interactions.
 *
 * Pickup: Shift-right-click on tamed Frenchie (owned by you)
 * Put down: Shift-right-click air while carrying a Frenchie
 */
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public class FrontpackEvents {

    /**
     * Handle shift-right-click on Frenchie to pick it up.
     */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();

        // Pickup conditions
        if (!player.isShiftKeyDown()) return;
        if (!(target instanceof FrenchieEntity frenchie)) return;
        if (!frenchie.isTame()) return;
        if (!frenchie.isOwnedBy(player)) return;
        if (frenchie.isBaby()) return;

        // Check if already carrying a Frenchie
        FrontpackData currentData = player.getData(RegisterAttachmentTypes.FRENCH_FRONTPACK_DATA);
        if (currentData.hasCarriedFrenchie()) return;

        // Client: just cancel event and return success
        if (player.level().isClientSide()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        // Server: serialize Frenchie to NBT and store in attachment
        TagValueOutput output = TagValueOutput.createWithContext(
            ProblemReporter.DISCARDING,
            player.level().registryAccess()
        );
        frenchie.saveWithoutId(output);
        CompoundTag frenchieNBT = output.buildResult();

        FrontpackData newData = new FrontpackData(
            true,
            frenchieNBT,
            player.level().getGameTime()
        );

        player.setData(RegisterAttachmentTypes.FRENCH_FRONTPACK_DATA, newData);

        // Despawn the entity
        frenchie.discard();

        // Play pickup sound
        player.playSound(SoundEvents.ITEM_PICKUP, 1.0f, 1.0f);

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /**
     * Handle shift-right-click air to put down Frenchie.
     * This only fires on client, so we send a packet to server.
     */
    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        Player player = event.getEntity();

        if (!player.isShiftKeyDown()) return;

        FrontpackData data = player.getData(RegisterAttachmentTypes.FRENCH_FRONTPACK_DATA);
        if (!data.hasCarriedFrenchie()) return;

        // Send packet to server to handle put-down
        ClientPacketDistributor.sendToServer(PutDownFrenchiePayload.INSTANCE);
    }
}
