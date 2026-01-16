package net.j40climb.florafauna.common.item.hammer;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna.common.item.abilities.data.MultiToolAbilityData;
import net.j40climb.florafauna.common.item.abilities.data.RightClickAction;
import net.j40climb.florafauna.common.item.abilities.data.ThrowableAbilityData;
import net.j40climb.florafauna.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.function.Consumer;

/**
 * A multi-mode mining tool that can mine blocks with different mining speeds and patterns.
 * This hammer supports multiple mining modes and speed settings configurable via data components.
 */
public class HammerItem extends Item {
    /**
     * The tool material configuration for the Energy Hammer.
     * Note: Durability and enchantability are unused since the hammer is unbreakable and not enchantable.
     * Attack damage and speed are set via .tool() in RegisterItems.
     * Mining speed is controlled by ToolAbilityEventHandlers via the TOOL_CONFIG component.
     */
    public static final ToolMaterial HAMMER_MATERIAL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, // What can't be mined (netherite tier)
            1, // Durability (unused - item is unbreakable)
            1.0F, // Base mining speed (modified by ToolAbilityEventHandlers)
            0.0F, // Attack damage bonus (base damage set in .tool())
            1, // Enchantability (minimum required, but ENCHANTABLE component is removed)
            ItemTags.NETHERITE_TOOL_MATERIALS // Repair ingredient (unused - item is unbreakable)
    );

    /**
     * Constructs a new EnergyHammerItem with the specified properties.
     * Automatically adds mining mode and mining speed data components with default values.
     *
     * @param properties the item properties to configure this hammer
     */
    public HammerItem(Properties properties) {
        super(removeEnchantable(properties)
                // Tool ability components (composable - these enable the abilities)
                .component(FloraFaunaRegistry.MULTI_BLOCK_MINING, MiningModeData.DEFAULT)
                .component(FloraFaunaRegistry.TOOL_CONFIG, ToolConfig.DEFAULT)
                .component(FloraFaunaRegistry.LIGHTNING_ABILITY, Unit.INSTANCE)
                .component(FloraFaunaRegistry.TELEPORT_SURFACE_ABILITY, Unit.INSTANCE)
                .component(FloraFaunaRegistry.THROWABLE_ABILITY, ThrowableAbilityData.DEFAULT)
                .component(FloraFaunaRegistry.RIGHT_CLICK_ACTION, new RightClickAction(Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "throwable_ability")))
                .component(FloraFaunaRegistry.MULTI_TOOL_ABILITY, MultiToolAbilityData.DEFAULT)
                .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
        );
    }

    /**
     * Removes the ENCHANTABLE component from properties to prevent enchanting via table/anvil.
     * This is called before the parent constructor to strip out the enchantability added by .tool().
     *
     * @param properties the item properties
     * @return properties with ENCHANTABLE component removed
     */
    private static Properties removeEnchantable(Properties properties) {
        return properties.component(DataComponents.ENCHANTABLE, null);
    }

    /**
     * Prevents any enchantments from being applied to the Energy Hammer.
     * This blocks the /enchant command and other programmatic enchantment attempts.
     * The hammer has its own built-in Fortune/Silk Touch toggle instead.
     *
     * @param stack the item stack
     * @param enchantment the enchantment being checked
     * @return false to reject all enchantments
     */
    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return false;
    }

    /**
     * Declares which tool actions this item can perform based on its MULTI_TOOL_ABILITY component.
     * This is required for getToolModifiedState to work properly.
     *
     * @param stack the item stack
     * @param itemAbility the tool action being checked
     * @return true if this item can perform the action
     */
    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        MultiToolAbilityData multiTool = stack.get(FloraFaunaRegistry.MULTI_TOOL_ABILITY);
        if (multiTool == null) {
            return false;
        }
        if (itemAbility == ItemAbilities.AXE_STRIP && multiTool.strip()) {
            return true;
        }
        if (itemAbility == ItemAbilities.SHOVEL_FLATTEN && multiTool.flatten()) {
            return true;
        }
        if (itemAbility == ItemAbilities.HOE_TILL && multiTool.till()) {
            return true;
        }
        return false;
    }

    /**
     * Determines if this hammer is the correct tool for mining the given block state.
     * The hammer can mine blocks that are mineable with pickaxe, shovel, axe, hoe, or are sword efficient.
     *
     * @param itemStack the hammer item stack
     * @param blockState the block state being mined
     * @return true if this hammer can properly mine the block, false otherwise
     */
    @Override
    public boolean isCorrectToolForDrops(ItemStack itemStack, BlockState blockState) {
        // TODO what is the downside of just returning true?
        return (blockState.is(BlockTags.MINEABLE_WITH_PICKAXE) || blockState.is(BlockTags.MINEABLE_WITH_SHOVEL) || blockState.is(BlockTags.MINEABLE_WITH_AXE) || blockState.is(BlockTags.MINEABLE_WITH_HOE) || blockState.is(BlockTags.SWORD_EFFICIENT));
    }

    /**
     * Appends tooltip text to display the current mining mode when hovering over the item.
     *
     * @param itemStack the hammer item stack
     * @param context the tooltip context
     * @param tooltipDisplay the tooltip display configuration
     * @param tooltipComponents consumer to accept tooltip components
     * @param tooltipFlag flags controlling tooltip behavior
     */
    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        MiningModeData miningModeData = itemStack.getOrDefault(FloraFaunaRegistry.MULTI_BLOCK_MINING, MiningModeData.DEFAULT);
        tooltipComponents.accept(Component.literal(miningModeData.getMiningModeString()));
        ToolConfig toolConfig = itemStack.getOrDefault(FloraFaunaRegistry.TOOL_CONFIG, ToolConfig.DEFAULT);
        tooltipComponents.accept(Component.literal(toolConfig.getMiningSppedString()));
        super.appendHoverText(itemStack, context, tooltipDisplay, tooltipComponents, tooltipFlag);
    }

    /**
     * Determines whether this item should display an enchantment glint effect.
     *
     * @param itemStack the hammer item stack
     * @return false to disable the enchantment glint effect
     */
    @Override
    public boolean isFoil(ItemStack itemStack) {
        return false;
    }
}