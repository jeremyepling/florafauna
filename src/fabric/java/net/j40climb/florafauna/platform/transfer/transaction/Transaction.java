package net.j40climb.florafauna.platform.transfer.transaction;
public final class Transaction implements TransactionContext, AutoCloseable {
 private final net.fabricmc.fabric.api.transfer.v1.transaction.Transaction transaction;
 private Transaction(net.fabricmc.fabric.api.transfer.v1.transaction.Transaction transaction) { this.transaction=transaction; }
 public static Transaction openRoot() { return new Transaction(net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()); }
 public static Transaction open(TransactionContext parent) { return new Transaction(net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openNested(parent.fabric())); }
 public Transaction openNested() { return new Transaction(transaction.openNested()); }
 public void commit() { transaction.commit(); }
 public void abort() { transaction.abort(); }
 public void close() { transaction.close(); }
 public net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext fabric() { return transaction; }
}
