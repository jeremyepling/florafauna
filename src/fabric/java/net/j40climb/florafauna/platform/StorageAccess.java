package net.j40climb.florafauna.platform;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.j40climb.florafauna.platform.transfer.ResourceHandler;
import net.j40climb.florafauna.platform.transfer.item.ItemResource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import java.util.*;
public final class StorageAccess {
 public static ResourceHandler<ItemResource> get(Level level,BlockPos pos,Direction side) {
  Storage<ItemVariant> storage=ItemStorage.SIDED.find(level,pos,side);return storage==null?null:new StorageHandler(storage);
 }
 private record StorageHandler(Storage<ItemVariant> storage) implements ResourceHandler<ItemResource> {
  private List<StorageView<ItemVariant>> views() { List<StorageView<ItemVariant>> list=new ArrayList<>();storage.forEach(list::add);return list; }
  public int size(){return views().size();}
  public ItemResource getResource(int i){return new ItemResource(views().get(i).getResource());}
  public long getAmountAsLong(int i){return views().get(i).getAmount();}
  public long getCapacityAsLong(int i,ItemResource r){return views().get(i).getCapacity();}
  public boolean isValid(int i,ItemResource r){return storage.supportsInsertion();}
  public int insert(int i,ItemResource r,int amount,net.j40climb.florafauna.platform.transfer.transaction.TransactionContext tx){
   if(storage instanceof SlottedStorage<ItemVariant> slotted)return (int)slotted.getSlot(i).insert(r.variant(),amount,tx.fabric());
   var view=views().get(i);if(view instanceof net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage<ItemVariant> slot)return (int)slot.insert(r.variant(),amount,tx.fabric());
   return 0;
  }
  public int extract(int i,ItemResource r,int amount,net.j40climb.florafauna.platform.transfer.transaction.TransactionContext tx){return (int)views().get(i).extract(r.variant(),amount,tx.fabric());}
  public long insert(ItemResource r,long amount,net.j40climb.florafauna.platform.transfer.transaction.TransactionContext tx){return storage.insert(r.variant(),amount,tx.fabric());}
  public long extract(ItemResource r,long amount,net.j40climb.florafauna.platform.transfer.transaction.TransactionContext tx){return storage.extract(r.variant(),amount,tx.fabric());}
 }
 public static SlottedStorage<ItemVariant> expose(ResourceHandler<ItemResource> handler) {
  return new SlottedStorage<>() {
   public long insert(ItemVariant r,long amount,TransactionContext tx){return handler.insert(new ItemResource(r),amount,()->tx);}
   public long extract(ItemVariant r,long amount,TransactionContext tx){return handler.extract(new ItemResource(r),amount,()->tx);}
   public int getSlotCount(){return handler.size();}
   public net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage<ItemVariant> getSlot(int slot){
    java.util.Objects.checkIndex(slot,handler.size());
    return new net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage<>() {
     public long insert(ItemVariant r,long amount,TransactionContext tx){return handler.insert(slot,new ItemResource(r),(int)Math.min(Integer.MAX_VALUE,amount),()->tx);}
     public long extract(ItemVariant r,long amount,TransactionContext tx){return handler.extract(slot,new ItemResource(r),(int)Math.min(Integer.MAX_VALUE,amount),()->tx);}
     public boolean isResourceBlank(){return handler.getResource(slot).isEmpty();}
     public ItemVariant getResource(){return handler.getResource(slot).variant();}
     public long getAmount(){return handler.getAmountAsLong(slot);}
     public long getCapacity(){return handler.getCapacityAsLong(slot,handler.getResource(slot));}
    };
   }
   public Iterator<StorageView<ItemVariant>> iterator(){
    List<StorageView<ItemVariant>> views=new ArrayList<>();
    for(int i=0;i<handler.size();i++)views.add(getSlot(i));
    return views.iterator();
   }
  };
 }
}
