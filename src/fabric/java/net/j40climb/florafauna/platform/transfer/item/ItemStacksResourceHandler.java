package net.j40climb.florafauna.platform.transfer.item;
import net.j40climb.florafauna.platform.transfer.ResourceHandler;
import net.j40climb.florafauna.platform.transfer.transaction.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.*;
public class ItemStacksResourceHandler implements ResourceHandler<ItemResource> {
 private final List<ItemStack> stacks;
 private final List<SnapshotJournal<ItemStack>> journals=new ArrayList<>();
 public ItemStacksResourceHandler(int count) {
  stacks=new ArrayList<>(Collections.nCopies(count,ItemStack.EMPTY));
  for(int i=0;i<count;i++) { final int slot=i; journals.add(new SnapshotJournal<>() {
   protected ItemStack createSnapshot() { return stacks.get(slot).copy(); }
   protected void revertToSnapshot(ItemStack snapshot) { stacks.set(slot,snapshot); }
   protected void onRootCommit(ItemStack original) { onContentsChanged(slot,original); }
  }); }
 }
 public int size() { return stacks.size(); }
 /** Mutable slot contents, matching vanilla Container ownership for menu edits. */
 public ItemStack getStackInSlot(int index) { return stacks.get(index); }
 public void contentsChanged(int index) { onContentsChanged(index,ItemStack.EMPTY); }
 public ItemResource getResource(int i) { return ItemResource.of(stacks.get(i)); }
 public long getAmountAsLong(int i) { return stacks.get(i).getCount(); }
 protected int getCapacity(int i,ItemResource resource) { return resource.isEmpty()?64:resource.getMaxStackSize(); }
 public long getCapacityAsLong(int i,ItemResource resource) { return getCapacity(i,resource); }
 public boolean isValid(int i,ItemResource resource) { return true; }
 protected void onContentsChanged(int i,ItemStack previous) {}
 public void set(int i,ItemResource resource,int amount) { if(amount<0||(resource.isEmpty()&&amount>0))throw new IllegalArgumentException("Invalid resource amount"); ItemStack old=stacks.set(i,resource.toStack(amount));onContentsChanged(i,old); }
 public int insert(int i,ItemResource resource,int amount,TransactionContext tx) {
  net.j40climb.florafauna.platform.transfer.TransferPreconditions.checkNonEmptyNonNegative(resource,amount);
  ItemStack current=stacks.get(i);if(!isValid(i,resource)||(!current.isEmpty()&&!resource.matches(current)))return 0;
  int moved=Math.min(amount,Math.max(0,getCapacity(i,resource)-current.getCount()));
  if(moved>0){journals.get(i).updateSnapshots(tx);stacks.set(i,resource.toStack(current.getCount()+moved));}return moved;
 }
 public int extract(int i,ItemResource resource,int amount,TransactionContext tx) {
  net.j40climb.florafauna.platform.transfer.TransferPreconditions.checkNonEmptyNonNegative(resource,amount);
  ItemStack current=stacks.get(i);if(!resource.matches(current))return 0;int moved=Math.min(amount,current.getCount());
  if(moved>0){journals.get(i).updateSnapshots(tx);stacks.set(i,resource.toStack(current.getCount()-moved));}return moved;
 }
 public void serialize(ValueOutput output) { output.store("stacks",ItemStack.OPTIONAL_CODEC.listOf(),stacks); }
 public void deserialize(ValueInput input) { var loaded=input.read("stacks",ItemStack.OPTIONAL_CODEC.listOf());if(loaded.isPresent()){Collections.fill(stacks,ItemStack.EMPTY);var values=loaded.get();for(int i=0;i<Math.min(stacks.size(),values.size());i++)stacks.set(i,values.get(i));} }
}
