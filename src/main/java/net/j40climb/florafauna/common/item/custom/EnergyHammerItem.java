package net.j40climb.florafauna.common.item.custom;

import net.j40climb.florafauna.common.component.MiningModeData;
import net.j40climb.florafauna.common.component.MiningSpeed;
import net.j40climb.florafauna.common.component.ModDataComponentTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
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
     * Uses netherite-level tool material with custom durability and mining speed settings.
     */
    public static final ToolMaterial HAMMER_MATERIAL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, // What can't be mined
            59, // Durability
            2.0F, // Mining speed
            0.0F, // Attack damage bonus
            15, // Enchantability
            ItemTags.NETHERITE_TOOL_MATERIALS // Repair ingredient
    );

    /**
     * Constructs a new EnergyHammerItem with the specified properties.
     * Automatically adds mining mode and mining speed data components with default values.
     *
     * @param properties the item properties to configure this hammer
     */
    public EnergyHammerItem(Properties properties) {
        super(properties
                .component(ModDataComponentTypes.MINING_MODE_DATA, MiningModeData.DEFAULT)
                .component(ModDataComponentTypes.MINING_SPEED, MiningSpeed.EFFICIENCY)
        );
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
                        ModDataComponentTypes.MINING_MODE_DATA,
                        MiningModeData.DEFAULT,
                        MiningModeData::getNextMode
                );

                // Output the new shape and default was set via Update so a get() instead of getOrDefault()
                MiningModeData currentMiningMode = hammerItemStack.get(ModDataComponentTypes.MINING_MODE_DATA);
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
        return switch (itemStack.getOrDefault(ModDataComponentTypes.MINING_SPEED, 2)) {
            case MiningSpeed.STANDARD ->
                    (blockState.is(BlockTags.MINEABLE_WITH_PICKAXE) || blockState.is(BlockTags.MINEABLE_WITH_SHOVEL) || blockState.is(BlockTags.MINEABLE_WITH_AXE)) ? super.getDestroySpeed(itemStack, Blocks.COBBLESTONE.defaultBlockState()) : 1.0F;
            case MiningSpeed.EFFICIENCY -> 35.0F;
            case MiningSpeed.INSTABREAK -> 100.0F;
            default ->
                    throw new IllegalStateException("Unexpected value: " + itemStack.get(ModDataComponentTypes.MINING_SPEED));
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
    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        MiningModeData miningModeData = itemStack.getOrDefault(ModDataComponentTypes.MINING_MODE_DATA, MiningModeData.DEFAULT);
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