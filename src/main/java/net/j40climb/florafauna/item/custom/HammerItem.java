package net.j40climb.florafauna.item.custom;

import net.j40climb.florafauna.component.MiningModeData;
import net.j40climb.florafauna.component.MiningShape;
import net.j40climb.florafauna.component.ModDataComponentTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Tier;

public class HammerItem extends DiggerItem {
    public HammerItem(Tier pTier, Properties pProperties) {
        // New tags in 1.21.30 make this easier #minecraft:iron_tier_destructible for all diggers or
        // #minecraft:is_pickaxe_item_destructible for pickaxe

        // TODO Changing this to ModTags.Blocks.MINEABLE_WITH_PAXEL causes a max networking error
        // io.netty.handler.codec.EncoderException: java.io.UTFDataFormatException: encoded string (Tool[rul...Block=1]) too long: 81675 bytes
        super(pTier, BlockTags.MINEABLE_WITH_PICKAXE, pProperties
                .component(ModDataComponentTypes.MINING_MODE_DATA, new MiningModeData(MiningShape.FLAT_3X3, 1, 64))
        );
    }
}
