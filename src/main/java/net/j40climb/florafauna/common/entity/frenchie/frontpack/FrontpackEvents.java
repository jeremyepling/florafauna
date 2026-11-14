package net.j40climb.florafauna.common.entity.frenchie.frontpack;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.entity.frenchie.FrenchieEntity;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
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
        FrontpackData currentData = player.getData(FloraFaunaRegistry.FRENCH_FRONTPACK_DATA);
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

        player.setData(FloraFaunaRegistry.FRENCH_FRONTPACK_DATA, newData);

        // Despawn the entity
        frenchie.discard();

        // Play pickup sound
        player.playSound(SoundEvents.ITEM_PICKUP, 1.0f, 1.0f);

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    public static void putDown(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        FrontpackData data = player.getData(FloraFaunaRegistry.FRENCH_FRONTPACK_DATA);
        if (!data.hasCarriedFrenchie()) return;

        var type = FloraFaunaRegistry.FRENCHIE.get();
        // Use yaw for the ground target so looking down never puts the dog inside the floor.
        Vec3 forward = Vec3.directionFromRotation(0, player.getYRot());
        BlockPos target = BlockPos.containing(player.position().add(forward.scale(2)));
        if (!level.isInWorldBounds(target)) return;
        Vec3 position = DismountHelper.findSafeDismountLocation(type, level, target, true);
        if (position == null) return;

        var dog = type.create(level, EntitySpawnReason.TRIGGERED);
        if (dog == null) return;
        dog.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data.frenchieNBT()));
        dog.snapTo(position, player.getYRot(), 0);
        if (!level.noCollision(dog, dog.getBoundingBox()) || !level.addFreshEntity(dog)) return;

        player.setData(FloraFaunaRegistry.FRENCH_FRONTPACK_DATA, FrontpackData.DEFAULT);
        player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0f, 1.0f);
    }
}
