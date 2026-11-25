package net.j40climb.florafauna.platform.transfer;
import net.j40climb.florafauna.platform.transfer.item.ItemResource;
public final class TransferPreconditions {
 public static void checkNonEmptyNonNegative(ItemResource resource, int amount) { if(resource.isEmpty()||amount<0) throw new IllegalArgumentException("Empty item or negative transfer amount"); }
}
