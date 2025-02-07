package net.j40climb.florafauna.common.item.custom;

import net.j40climb.florafauna.common.component.FoundBlockData;
import net.j40climb.florafauna.common.component.ModDataComponentTypes;
import net.j40climb.florafauna.common.item.ModItems;
import net.j40climb.florafauna.common.util.InventoryUtil;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
public class MetalDetectorItem extends Item {
    public MetalDetectorItem(Properties pProperties) {
        super(pProperties);
    }
    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        if(!pContext.getLevel().isClientSide()) {
            BlockPos positionClicked = pContext.getClickedPos();
            Player player = pContext.getPlayer();
            boolean foundBlock = false;
            int lowestYHeight = -64;

            for(int i = lowestYHeight; i <= positionClicked.getY(); i++) {
                BlockState blockState = pContext.getLevel().getBlockState(positionClicked.below(i));
                if(isValuableBlock(blockState)) {
                    outputValuableCoordinates(positionClicked.below(i), player, blockState.getBlock());
                    foundBlock = true;
                    if(InventoryUtil.hasPlayerStackInInventory(player, ModItems.DATA_TABLET.get())) {
                        addDataToDataTablet(player, positionClicked.below(i), blockState.getBlock());
                    }
                    pContext.getLevel().playSeededSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ANVIL_STEP, SoundSource.BLOCKS, 1f, 1f, 0);
                    break;
                }
            }
            if(!foundBlock) {
                outputNoValuableFound(player);
            }
        }
        return InteractionResult.SUCCESS;
    }
    private void addDataToDataTablet(Player player, BlockPos below, Block block) {
        ItemStack dataTablet = player.getInventory().getItem(InventoryUtil.getFirstInventoryIndex(player, ModItems.DATA_TABLET.get()));
        FoundBlockData data = new FoundBlockData(block.defaultBlockState(), below);
        dataTablet.set(ModDataComponentTypes.FOUND_BLOCK, data);
    }
    private void outputNoValuableFound(Player player) {
        player.sendSystemMessage(Component.translatable("item.florafauna.metal_detector.no_valuables"));
    }
    private void outputValuableCoordinates(BlockPos below, Player player, Block block) {
        player.sendSystemMessage(Component.literal("Valuable Found: " + I18n.get(block.getDescriptionId())
                + " at (" + below.getX() + ", " + below.getY() + ", " + below.getZ() + ")"));
    }
    private boolean isValuableBlock(BlockState blockState) {
        return blockState.is(Blocks.IRON_ORE);
    }
}