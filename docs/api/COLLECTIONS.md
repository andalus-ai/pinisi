# Collection lifecycle

Collections are isolated namespaces with one configured embedding space in the
current milestone. Their definitions are managed through `CollectionCatalog`.

## Safe deletion

Applications should use `DefaultCollectionLifecycleCoordinator` for deletion
rather than calling `CollectionCatalog.delete` directly. The coordinator
cleans each registered `CollectionDataStore` before removing the catalog entry.
Vector, lexical, metadata, and asset stores can participate without the catalog
depending on their implementations.

A store's `deleteCollection` operation must be idempotent and atomic within
that store. If cleanup fails, the coordinator leaves the catalog entry in place
and raises `CollectionCleanupException`, which identifies the failed store and
the stores that completed. The caller can retry safely; stores already cleaned
must treat the repeated deletion as a no-op.

This is a retryable multi-store workflow, not a distributed transaction. During
partial cleanup, some stores may already have removed their data while the
collection remains in the catalog. Production durable stores should implement
deletion with durable tombstones or equivalent retry-safe semantics.

Direct catalog deletion remains available for low-level metadata administration
and tests, but bypasses index and asset cleanup.
