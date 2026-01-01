package net.j40climb.florafauna.common.item.energyhammer;

import net.j40climb.florafauna.common.RegisterDataComponentTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * A multi-mode mining tool that can mine blocks with different mining speeds and patterns.
 * This hammer supports multiple mining modes and speed settings configurable via data components.
 */
public class EnergyHammerItem extends Item {
    /**
     * The tool material configuration for the Energy Hammer.
     * Note: Durability and enchantability are unused since the hammer is unbreakable and not enchantable.
     * Attack damage and speed are set via .tool() in RegisterItems.
     */
    public static final ToolMaterial HAMMER_MATERIAL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, // What can't be mined (netherite tier)
            1, // Durability (unused - item is unbreakable)
            1.0F, // Base mining speed (overridden by getDestroySpeed)
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
    public EnergyHammerItem(Properties properties) {
        super(removeEnchantable(properties)
                .component(RegisterDataComponentTypes.MINING_MODE_DATA, MiningModeData.DEFAULT)
                .component(RegisterDataComponentTypes.ENERGY_HAMMER_CONFIG, EnergyHammerConfig.DEFAULT)
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
     * Handles right-click interactions with blocks.
     * Cycles through available mining modes and displays the current mode to the player.
     * This operation only executes server-side to prevent client-server desync.
     *
     * @param useOnContext the context of the block interaction
     * @return {@link InteractionResult#PASS} to allow other interactions to proceed
     */
    @Override
    public @NotNull InteractionResult useOn(UseOnContext useOnContext) {
        if(!useOnContext.getLevel().isClientSide()) {
            Player player = useOnContext.getPlayer();
            if (player != null) {
                ItemStack hammerItemStack = player.getMainHandItem();

                // Go to next shape
                hammerItemStack.update(
                        RegisterDataComponentTypes.MINING_MODE_DATA,
                        MiningModeData.DEFAULT,
                        MiningModeData::getNextMode
                );

                // Output the new shape and default was set via Update so a get() instead of getOrDefault()
                MiningModeData currentMiningMode = hammerItemStack.get(RegisterDataComponentTypes.MINING_MODE_DATA);
                player.displayClientMessage(Component.literal(currentMiningMode.getMiningModeString()), true);
            }
        }
        return InteractionResult.PASS;
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
     * Gets the mining speed for this hammer based on its current mining speed setting.
     * The speed varies depending on the mining speed data component:
     * <ul>
     *   <li>STANDARD: Uses default speed for mineable blocks, 1.0F otherwise</li>
     *   <li>EFFICIENCY: Returns 35.0F for fast mining</li>
     *   <li>INSTABREAK: Returns 100.0F for instant breaking</li>
     * </ul>
     *
     * @param itemStack the hammer item stack
     * @param blockState the block state being mined
     * @return the mining speed multiplier
     * @throws IllegalStateException if the mining speed component has an unexpected value
     */
    @Override
    public float getDestroySpeed(ItemStack itemStack, BlockState blockState) {
        EnergyHammerConfig config = itemStack.getOrDefault(RegisterDataComponentTypes.ENERGY_HAMMER_CONFIG, EnergyHammerConfig.DEFAULT);
        return switch (config.miningSpeed()) {
            case MiningSpeed.STANDARD ->
                    (blockState.is(BlockTags.MINEABLE_WITH_PICKAXE) || blockState.is(BlockTags.MINEABLE_WITH_SHOVEL) || blockState.is(BlockTags.MINEABLE_WITH_AXE)) ? super.getDestroySpeed(itemStack, Blocks.COBBLESTONE.defaultBlockState()) : 1.0F;
            case MiningSpeed.EFFICIENCY -> 35.0F;
            case MiningSpeed.INSTABREAK -> 100.0F;
        };
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
        MiningModeData miningModeData = itemStack.getOrDefault(RegisterDataComponentTypes.MINING_MODE_DATA, MiningModeData.DEFAULT);
        tooltipComponents.accept(Component.literal(miningModeData.getMiningModeString()));
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