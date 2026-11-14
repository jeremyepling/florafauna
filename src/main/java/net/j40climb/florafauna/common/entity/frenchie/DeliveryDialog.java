package net.j40climb.florafauna.common.entity.frenchie;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.j40climb.florafauna.common.entity.frenchie.DeliveryGoal.label;
import static net.j40climb.florafauna.common.entity.frenchie.DeliveryGoal.tell;

/** Server-owned preview. Only the live dog's owner can confirm the held bundle and offered address. */
public final class DeliveryDialog {
    private final FrenchieEntity dog;
    private final InteractionHand hand;
    private final ItemStack previewStack;
    private final long expires;
    private final @Nullable GlobalPos offeredBed;
    private final Map<String, Component> destinations = new LinkedHashMap<>();
    private @Nullable String selected;

    DeliveryDialog(FrenchieEntity dog, ServerPlayer owner, InteractionHand hand) {
        this.dog = dog;
        this.hand = hand;
        previewStack = owner.getItemInHand(hand).copy();
        expires = dog.level().getGameTime() + 20 * 120;
        var level = owner.level();
        offeredBed = ownerBed(owner);
        if (offeredBed != null) destinations.put("home", label("home"));
        level.getServer().getPlayerList().getPlayers().stream().filter(p -> eligiblePlayer(owner, p))
                .sorted(Comparator.comparing(p -> p.getName().getString(), String.CASE_INSENSITIVE_ORDER))
                .forEach(p -> destinations.put(p.getStringUUID(), p.getName()));
        var choices = destinations.entrySet().stream()
                .map(entry -> new DeliveryPopupPayload.Choice(entry.getKey(), entry.getValue())).toList();
        PacketDistributor.sendToPlayer(owner, new DeliveryPopupPayload(dog.getUUID(), choices, Optional.empty()));
    }

    static @Nullable GlobalPos ownerBed(ServerPlayer owner) {
        var respawn = owner.getRespawnConfig();
        if (respawn == null || respawn.forced()) return null;
        var address = respawn.respawnData().globalPos();
        var level = owner.level().getServer().getLevel(address.dimension());
        if (level == null) return null;
        // Vanilla saves the position for beds and anchors alike. The HOME POI indexes bed heads
        // even in unloaded chunks, so opening a delivery card needn't load a remote bed's terrain.
        boolean bed = level.hasChunkAt(address.pos()) ? DeliveryGoal.isBedHead(level, address.pos())
                : level.getPoiManager().existsAtPosition(PoiTypes.HOME, address.pos());
        return bed ? address : null;
    }

    private static boolean eligiblePlayer(ServerPlayer owner, ServerPlayer candidate) {
        return candidate != owner && candidate.isAlive() && !candidate.isSpectator();
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var dogId = Commands.argument("dog", UuidArgument.uuid())
                .then(Commands.literal("dismiss").executes(ctx -> reply(ctx, "dismiss", "")));
        for (String action : List.of("choose", "send")) {
            dogId.then(Commands.literal(action)
                    .then(Commands.literal("home").executes(ctx -> reply(ctx, action, "home")))
                    .then(Commands.argument("player", UuidArgument.uuid()).executes(ctx ->
                            reply(ctx, action, UuidArgument.getUuid(ctx, "player").toString()))));
        }
        dispatcher.register(Commands.literal("frenchiedelivery").then(dogId));
    }

    private static int reply(CommandContext<CommandSourceStack> ctx, String action, String destination) throws CommandSyntaxException {
        var owner = ctx.getSource().getPlayerOrException();
        if (owner.level().getEntity(UuidArgument.getUuid(ctx, "dog")) instanceof FrenchieEntity dog
                && dog.isAlive() && dog.isOwnedBy(owner) && owner.isAlive() && !owner.isSpectator()
                && owner.isWithinEntityInteractionRange(dog, 0) && !dog.hasCompanionJob()) {
            DeliveryDialog dialog = dog.deliveryDialog;
            if (dialog != null && dog.level().getGameTime() <= dialog.expires
                    && ItemStack.matches(owner.getItemInHand(dialog.hand), dialog.previewStack)
                    && dialog.handle(owner, action, destination)) return 1;
        }
        tell(owner, "expired");
        return 0;
    }

    private boolean handle(ServerPlayer owner, String action, String destination) {
        if (action.equals("dismiss")) {
            dog.deliveryDialog = null;
            return true;
        }
        Component name = destinations.get(destination);
        if (name == null) return false;
        ServerPlayer recipient = null;
        ServerLevel targetLevel;
        Vec3 target;
        if (destination.equals("home")) {
            targetLevel = offeredBed == null ? null : owner.level().getServer().getLevel(offeredBed.dimension());
            if (targetLevel == null || !offeredBed.equals(ownerBed(owner))) {
                dog.deliveryDialog = null;
                tell(owner, "home_changed");
                return true;
            }
            target = offeredBed.pos().getBottomCenter();
        } else {
            recipient = owner.level().getServer().getPlayerList().getPlayer(UUID.fromString(destination));
            if (recipient == null || !eligiblePlayer(owner, recipient)) {
                tell(owner, "recipient_unavailable");
                return true;
            }
            targetLevel = recipient.level();
            target = recipient.position();
        }
        if (action.equals("choose")) {
            selected = destination;
            var preview = new DeliveryPopupPayload.Preview(new DeliveryPopupPayload.Choice(destination, name),
                    targetLevel.dimension().identifier(), CourierTrips.arrivalSeconds(owner.level(), dog.position(), targetLevel, target));
            PacketDistributor.sendToPlayer(owner, new DeliveryPopupPayload(dog.getUUID(), List.of(), Optional.of(preview)));
        } else {
            if (!destination.equals(selected)) return false;
            dog.deliveryDialog = null;
            dog.delivery().start(owner, hand, recipient, recipient == null ? offeredBed : null);
            dog.syncCompanionJob();
        }
        return true;
    }
}
