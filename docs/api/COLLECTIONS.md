# Collection lifecycle

Collections are isolated namespaces with one configured embedding space in the
current milestone. Their definitions are managed through `CollectionCatalog`.

## Safe, restartable deletion

Applications should use `DefaultCollectionLifecycleCoordinator` for deletion
rather than calling `CollectionCatalog.delete` directly. The coordinator
cleans each registered `CollectionDataStore` before removing the catalog entry.
Vector, lexical, metadata, and asset stores can participate without the catalog
depending on their implementations.

Inject a `CollectionDeletionJournal` into the coordinator. The
`FileCollectionDeletionJournal` persists operation state and per-store
checkpoints under an application-managed directory. Keep this directory on
durable storage and include it in backup/recovery procedures. Applications may
provide a database-backed journal through the same SPI.

The coordinator records an operation before cleanup, checkpoints each store,
and marks it complete only after the catalog entry is removed. Call
`resumePendingDeletions()` during startup or worker recovery to continue
incomplete operations. Completed records are retained for audit; journal
retention/compaction is an operational policy.

## Tombstone visibility

Use `DeletionAwareCollectionCatalog` as the catalog view at application and
search read boundaries, and share the same journal instance/configuration with
the lifecycle coordinator. The coordinator writes a pending tombstone before
it begins deleting store data. The decorator then hides that collection from
`find` and `list` until the journal marks deletion `COMPLETED`. It also
rejects create/update attempts for the same ID while deletion is pending.
Unrelated collections remain visible.

The decorator is a visibility boundary, not a replacement for the lifecycle
coordinator. Direct calls to the underlying catalog or direct data-store
queries bypass it. Route search resolution through the deletion-aware view and
do not expose raw store search results without checking collection visibility.
A completed tombstone remains in the journal for audit but does not block
intentional reuse of the collection ID.

A store's `deleteCollection` operation must be idempotent and atomic within
that store. Recovery provides at-least-once execution: a process can stop after
a store deletion but before its checkpoint is committed, so that store may be
called again. The journal serializes its own writes and uses revision-based
compare-and-set, but it does not make external store mutations a distributed
transaction or provide exactly-once execution.

If cleanup fails, the journal records the failed component and completed stores.
The catalog entry remains until all data stores have completed. Direct catalog
deletion remains available for low-level metadata administration and tests, but
bypasses index and asset cleanup.
