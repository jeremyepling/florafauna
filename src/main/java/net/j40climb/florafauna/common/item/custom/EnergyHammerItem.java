package net.j40climb.florafauna.common.item.custom;

import net.j40climb.florafauna.common.component.MiningModeData;
import net.j40climb.florafauna.common.component.MiningSpeed;
import net.j40climb.florafauna.common.component.ModDataComponentTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EnergyHammerItem extends DiggerItem {
    public EnergyHammerItem() {
        // New tags in 1.21.30 make this easier #minecraft:iron_tier_destructible for all diggers or
        // #minecraft:is_pickaxe_item_destructible for pickaxe

        // TODO Changing this to ModTags.Blocks.MINEABLE_WITH_PAXEL causes a max networking error
        // BlockTags.MINEABLE_WITH_PICKAXE doesn't do anything - I don't think - since isCorrectTool is overridden
        // io.netty.handler.codec.EncoderException: java.io.UTFDataFormatException: encoded string (Tool[rul...Block=1]) too long: 81675 bytes
        super(Tiers.NETHERITE, BlockTags.MINEABLE_WITH_PICKAXE, new Properties()
                .attributes(EnergyHammerItem.createAttributes(Tiers.NETHERITE, 8, -3.3f))
                .component(ModDataComponentTypes.MINING_MODE_DATA, MiningModeData.DEFAULT)
                .component(ModDataComponentTypes.MINING_SPEED, MiningSpeed.EFFICIENCY)
        );
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext useOnContext) {
        if(!useOnContext.getLevel().isClientSide()) {
            Level level = useOnContext.getLevel();
            BlockPos blockpos = useOnContext.getClickedPos();
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

    @Override
    public boolean isCorrectToolForDrops(ItemStack itemStack, BlockState blockState) {
        // TODO what is the downside of just returning true?
        return (blockState.is(BlockTags.MINEABLE_WITH_PICKAXE) || blockState.is(BlockTags.MINEABLE_WITH_SHOVEL) || blockState.is(BlockTags.MINEABLE_WITH_AXE) || blockState.is(BlockTags.MINEABLE_WITH_HOE) || blockState.is(BlockTags.SWORD_EFFICIENT));
    }

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

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if(itemStack.get(ModDataComponentTypes.MINING_MODE_DATA.get()) != null) {
            MiningModeData miningModeData = itemStack.getOrDefault(ModDataComponentTypes.MINING_MODE_DATA, MiningModeData.DEFAULT);
            tooltipComponents.add(Component.literal(miningModeData.getMiningModeString()));
        }
        super.appendHoverText(itemStack, tooltipContext, tooltipComponents, tooltipFlag);
    }
}
