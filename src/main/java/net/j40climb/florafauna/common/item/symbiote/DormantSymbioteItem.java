package net.j40climb.florafauna.common.item.symbiote;

import net.j40climb.florafauna.common.item.symbiote.progress.ProgressSignalTracker;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * A dormant symbiote item that can be bound to a player via the Cocoon Chamber.
 *
 * This item no longer binds on consume. Instead:
 * 1. Player consumes symbiote_stew to become "bindable"
 * 2. Player uses Cocoon Chamber's Bind action with this item in inventory
 * 3. Cocoon Chamber consumes this item and binds the symbiote to the player
 *
 * The item stores symbiote data (tier, abilities) and progress tracking,
 * which persist across bind/unbind cycles.
 */
public class DormantSymbioteItem extends Item {

    public DormantSymbioteItem(Properties properties) {
        super(properties
                .component(FloraFaunaRegistry.SYMBIOTE_DATA, SymbioteData.DEFAULT)
                .component(FloraFaunaRegistry.SYMBIOTE_PROGRESS, ProgressSignalTracker.DEFAULT)
        );
    }

    /**
     * Prevents direct consumption of the dormant symbiote.
     * The player must use the Cocoon Chamber to bind.
     */
    @Override
    public @NotNull InteractionResult use(Level level, Player player, InteractionHand hand) {
        // Show message that this item must be used via Cocoon Chamber
        if (level.isClientSide()) {
            player.displayClientMessage(
                    Component.translatable("symbiote.florafauna.use_cocoon")
                            .withStyle(style -> style.withColor(0x9B59B6).withItalic(true)),
                    true
            );
        }
        return InteractionResult.FAIL;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay tooltipDisplay,
                                Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.accept(Component.translatable("tooltip.florafauna.dormant_symbiote"));

        // Show tier if advanced
        SymbioteData data = itemStack.getOrDefault(FloraFaunaRegistry.SYMBIOTE_DATA, SymbioteData.DEFAULT);
        if (data.tier() > 1) {
            tooltipComponents.accept(Component.translatable("tooltip.florafauna.symbiote_tier", data.tier())
                    .withStyle(style -> style.withColor(0x9B59B6)));
        }

        super.appendHoverText(itemStack, context, tooltipDisplay, tooltipComponents, tooltipFlag);
    }
}
