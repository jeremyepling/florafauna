package net.j40climb.florafauna.common.entity.frenchie;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
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
    private final @Nullable BlockPos nearbyBed;
    private final Map<String, Component> destinations = new LinkedHashMap<>();
    private @Nullable String selected;

    DeliveryDialog(FrenchieEntity dog, ServerPlayer owner, InteractionHand hand) {
        this.dog = dog;
        this.hand = hand;
        previewStack = owner.getItemInHand(hand).copy();
        expires = dog.level().getGameTime() + 20 * 120;
        var level = owner.level();
        var origin = dog.blockPosition();
        nearbyBed = BlockPos.findClosestMatch(origin, 8, 8, pos -> origin.distSqr(pos) <= 64
                && DeliveryGoal.isBedHead(level, pos)).map(BlockPos::immutable).orElse(null);
        List<DeliveryPopupPayload.Choice> choices = new ArrayList<>();
        var home = dog.delivery().home();
        if (home != null && level.getServer().getLevel(home.dimension()) != null) {
            String address = home.pos().toShortString();
            if (!home.dimension().equals(level.dimension())) address += " (" + home.dimension().identifier() + ")";
            destinations.put("home", label("home", address));
            choices.add(new DeliveryPopupPayload.Choice("home", label("send_home", address)));
        }
        level.getServer().getPlayerList().getPlayers().stream().filter(p -> eligiblePlayer(owner, p))
                .sorted(Comparator.comparing(p -> p.getName().getString(), String.CASE_INSENSITIVE_ORDER))
                .forEach(p -> {
                    String id = p.getUUID().toString();
                    destinations.put(id, p.getName());
                    choices.add(new DeliveryPopupPayload.Choice(id, label("send_player", p.getName())));
                });
        if (nearbyBed != null && (home == null || !home.dimension().equals(level.dimension()) || !nearbyBed.equals(home.pos()))) {
            choices.add(new DeliveryPopupPayload.Choice("bed", label("assign", nearbyBed.toShortString())));
        }
        PacketDistributor.sendToPlayer(owner, new DeliveryPopupPayload(dog.getUUID(), choices, Optional.empty()));
    }

    private static boolean eligiblePlayer(ServerPlayer owner, ServerPlayer candidate) {
        return candidate != owner && candidate.isAlive() && !candidate.isSpectator();
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var dogId = Commands.argument("dog", UuidArgument.uuid())
                .then(Commands.literal("bed").executes(ctx -> reply(ctx, "bed", "")))
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
        if (action.equals("bed")) {
            if (nearbyBed == null || dog.blockPosition().distSqr(nearbyBed) > 64
                    || !DeliveryGoal.isBedHead(owner.level(), nearbyBed)) return false;
            dog.delivery().assignBed(nearbyBed);
            dog.deliveryDialog = null;
            tell(owner, "assigned", nearbyBed.toShortString());
            return true;
        }
        Component name = destinations.get(destination);
        if (name == null) return false;
        ServerPlayer recipient = null;
        ServerLevel targetLevel;
        Vec3 target;
        if (destination.equals("home")) {
            var home = dog.delivery().home();
            targetLevel = home == null ? null : owner.level().getServer().getLevel(home.dimension());
            if (targetLevel == null || (targetLevel.hasChunkAt(home.pos()) && !DeliveryGoal.isBedHead(targetLevel, home.pos()))) {
                tell(owner, "reassign");
                return true;
            }
            target = home.pos().getBottomCenter();
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
                    DeliveryGoal.travelSeconds(owner.level(), dog.position(), targetLevel, target));
            PacketDistributor.sendToPlayer(owner, new DeliveryPopupPayload(dog.getUUID(), List.of(), Optional.of(preview)));
        } else {
            if (!destination.equals(selected)) return false;
            dog.deliveryDialog = null;
            dog.delivery().start(owner, hand, recipient);
            dog.syncCompanionJob();
        }
        return true;
    }
}
