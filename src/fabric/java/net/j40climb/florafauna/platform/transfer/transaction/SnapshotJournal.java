package net.j40climb.florafauna.platform.transfer.transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
public abstract class SnapshotJournal<T> extends SnapshotParticipant<T> {
 protected abstract void revertToSnapshot(T snapshot);
 protected void onRootCommit(T originalState) {}
 protected final void readSnapshot(T snapshot) { revertToSnapshot(snapshot); }
 protected final void onFinalCommit() { onRootCommit(null); }
 public final void updateSnapshots(TransactionContext context) { super.updateSnapshots(context.fabric()); }
}

