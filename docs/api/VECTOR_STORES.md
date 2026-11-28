# Vector stores

Pinisi separates vector persistence from retrieval orchestration through the
`VectorStore` SPI. A store receives a `CollectionDescriptor` for every
operation; the descriptor is the authoritative collection namespace and
embedding-space contract.

## Records and identity

A `VectorRecord` has a stable vector ID, its source `Asset`, an
`EmbeddingVector`, and optional record metadata. The vector ID is scoped to
the collection, so two collections may independently contain the same ID.
This record shape can later represent asset-level or segment-level vectors
without tying the storage SPI to a particular vector database.

## Compatibility rules

- Writes must target the collection named by the record.
- The vector's embedding space must equal the collection's configured space.
- The vector modality must be enabled in the collection's indexed modalities.
- Asset media type and vector modality must agree.
- Queries must use the collection's configured embedding space.
- Batch writes validate every record before publishing any record.

An incompatible query or write is rejected explicitly; it is never treated as
an empty result set. The in-memory implementation currently supports cosine
similarity and orders equal scores by vector ID for deterministic results.

## Lifecycle operations

The SPI supports single and batch upsert, collection-scoped search, deletion
of one vector, counting, and removal of all vectors in one collection.
Collection cleanup is coordinated with the collection catalog and every
registered data store by the lifecycle coordinator.

## Enforcing deletion visibility

Wrap each application-facing vector store with `DeletionAwareVectorStore`,
using the same `CollectionDeletionJournal` as the lifecycle coordinator and a
`DeletionAwareCollectionCatalog` for collection resolution. The decorator
checks the journal and resolves the current canonical descriptor before every
read or mutation, so stale descriptors cannot be used to bypass a pending
tombstone or an updated collection definition.

The decorator guards search, upsert, batch upsert, single-vector deletion, and
count. Its `deleteCollection` method intentionally delegates without a
visibility check: the lifecycle coordinator must be able to finish cleanup
after the tombstone is written. Register the underlying store with the
coordinator, not the decorator, to keep cleanup ownership unambiguous.

The check is performed at operation entry. An operation that passed its
visibility check before a concurrent deletion tombstone is committed may
finish; the decorator does not claim a distributed lock across catalog,
journal, and vector storage. Applications must route all user-facing vector
access through the decorator and must not expose the raw store.

`InMemoryVectorStore` is a thread-safe process-local implementation for
embedded applications and tests, not a durable production database.
