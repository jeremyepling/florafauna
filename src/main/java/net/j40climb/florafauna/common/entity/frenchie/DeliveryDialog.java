package net.j40climb.florafauna.common.entity.frenchie;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.*;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.j40climb.florafauna.common.entity.frenchie.DeliveryGoal.label;
import static net.j40climb.florafauna.common.entity.frenchie.DeliveryGoal.tell;

/** Server-owned preview. Only the live dog's owner can confirm the held bundle and offered address. */
public final class DeliveryDialog {
    private static final int PLAYER_RANGE = 32;
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
        List<ActionButton> buttons = new ArrayList<>();
        var home = dog.delivery().home();
        if (home != null && home.dimension().equals(level.dimension())) {
            String address = home.pos().toShortString();
            destinations.put("home", label("home", address));
            buttons.add(button(label("send_home", address), "choose home"));
        }
        level.players().stream().filter(p -> eligiblePlayer(owner, p))
                .sorted(Comparator.comparingDouble(p -> dog.distanceToSqr(p)))
                .forEach(p -> {
                    String id = p.getUUID().toString();
                    destinations.put(id, p.getName());
                    buttons.add(button(label("send_player", p.getName()), "choose " + id));
                });
        if (nearbyBed != null && (home == null || !home.dimension().equals(level.dimension()) || !nearbyBed.equals(home.pos()))) {
            buttons.add(button(label("assign", nearbyBed.toShortString()), "bed"));
        }
        buttons.add(closeButton());
        owner.openDialog(Holder.direct(new MultiActionDialog(
                common(label("title"), label("instructions")), buttons, Optional.empty(), 1)));
    }

    private static CommonDialogData common(Component title, Component body) {
        return new CommonDialogData(title, Optional.empty(), true, false, DialogAction.CLOSE,
                List.of(new PlainMessage(body, 290)), List.of());
    }

    private ActionButton button(Component text, String action) {
        return new ActionButton(new CommonButtonData(text, 235),
                Optional.of(new StaticAction(new ClickEvent.RunCommand("/frenchiedelivery " + dog.getStringUUID() + " " + action))));
    }

    private static ActionButton closeButton() {
        return new ActionButton(new CommonButtonData(Component.translatable("gui.cancel"), 120), Optional.empty());
    }

    private boolean eligiblePlayer(ServerPlayer owner, ServerPlayer candidate) {
        return candidate != owner && candidate.isAlive() && !candidate.isSpectator()
                && dog.distanceToSqr(candidate) <= PLAYER_RANGE * PLAYER_RANGE;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var dogId = Commands.argument("dog", UuidArgument.uuid())
                .then(Commands.literal("bed").executes(ctx -> reply(ctx, "bed", "")));
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
        if (destination.equals("home")) {
            var home = dog.delivery().home();
            if (home == null || !home.dimension().equals(owner.level().dimension())
                    || (owner.level().hasChunkAt(home.pos()) && !DeliveryGoal.isBedHead(owner.level(), home.pos()))) {
                tell(owner, "reassign");
                return true;
            }
        } else {
            if (!(owner.level().getPlayerByUUID(UUID.fromString(destination)) instanceof ServerPlayer player)
                    || !eligiblePlayer(owner, player)) {
                tell(owner, "out_of_range");
                return true;
            }
            recipient = player;
        }
        if (action.equals("choose")) {
            selected = destination;
            owner.openDialog(Holder.direct(new ConfirmationDialog(common(label("confirm_title"),
                    label(recipient == null ? "preview_bed" : "preview_player", name)),
                    button(label("confirm"), "send " + destination), closeButton())));
        } else {
            if (!destination.equals(selected)) return false;
            dog.deliveryDialog = null;
            dog.delivery().start(owner, hand, recipient);
            dog.syncCompanionJob();
        }
        return true;
    }
}
