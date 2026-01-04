package net.j40climb.florafauna.common.block.cocoonchamber;

import com.mojang.serialization.MapCodec;
import net.j40climb.florafauna.common.block.cocoonchamber.networking.OpenCocoonScreenPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The Cocoon Chamber block - a place for symbiote binding/unbinding and spawn setting.
 * Right-click opens a button-only screen (no inventory slots).
 * <p>
 * Uses {@link OpenCocoonScreenPayload} to open the screen on the client,
 * following the vanilla pattern for non-inventory GUIs.
 */
public class CocoonChamberBlock extends BaseEntityBlock {

    public static final MapCodec<CocoonChamberBlock> CODEC = simpleCodec(CocoonChamberBlock::new);

    public CocoonChamberBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CocoonChamberBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Allows the player to respawn at this block when it's set as a forced spawn point.
     * This is required for the cocoon spawn feature to work.
     */
    @Override
    public boolean isPossibleToRespawnInThis(BlockState state) {
        return true;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            // Send packet to open the screen on client (no menu system needed for button-only GUI)
            PacketDistributor.sendToPlayer(serverPlayer, new OpenCocoonScreenPayload(pos));
        }
        return InteractionResult.SUCCESS;
    }
}
