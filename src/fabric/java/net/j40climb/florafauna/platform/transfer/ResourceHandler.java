package net.j40climb.florafauna.platform.transfer;
import net.j40climb.florafauna.platform.transfer.transaction.TransactionContext;
public interface ResourceHandler<R> {
 int size(); R getResource(int index); long getAmountAsLong(int index); long getCapacityAsLong(int index,R resource);
 boolean isValid(int index,R resource);
 int insert(int index,R resource,int amount,TransactionContext transaction);
 int extract(int index,R resource,int amount,TransactionContext transaction);
 default int getAmountAsInt(int index) { return Math.toIntExact(getAmountAsLong(index)); }
 default long insert(R resource,long amount,TransactionContext transaction) { long left=amount;for(int i=0;i<size()&&left>0;i++)left-=insert(i,resource,(int)Math.min(Integer.MAX_VALUE,left),transaction);return amount-left; }
 default long extract(R resource,long amount,TransactionContext transaction) { long left=amount;for(int i=0;i<size()&&left>0;i++)left-=extract(i,resource,(int)Math.min(Integer.MAX_VALUE,left),transaction);return amount-left; }
}
