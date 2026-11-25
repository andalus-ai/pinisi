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
Collection catalog deletion is deliberately not coupled to vector cleanup yet:
a later lifecycle coordinator will make catalog and index cleanup an explicit,
failure-aware operation across vector, lexical, and asset stores.

`InMemoryVectorStore` is a thread-safe process-local implementation for
embedded applications and tests, not a durable production database.
