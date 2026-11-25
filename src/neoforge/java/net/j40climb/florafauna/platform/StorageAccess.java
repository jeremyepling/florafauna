package net.j40climb.florafauna.platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
public final class StorageAccess {
 public static ResourceHandler<ItemResource> get(Level level,BlockPos pos,Direction side){return level.getCapability(Capabilities.Item.BLOCK,pos,side);}
}
