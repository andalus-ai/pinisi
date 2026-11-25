# Collections

A collection is Pinisi's logical isolation boundary for indexed content and its search configuration.

## Definition

A collection definition includes a stable collection ID and display name, description and application metadata, the embedding space used by its vector index, and the modalities accepted for indexing.

Collection IDs are opaque application identifiers; they are not derived from filenames or source paths.

## Embedding-space policy

A collection's vector index is bound to one explicit embedding-space identity. Vectors from another provider/model/revision, dimension or metric must not be silently mixed into that index. Multimodal vectors may have different input modalities while sharing the same embedding space, provided the embedding provider declares the relevant query/document modality pair.

Changing a collection's embedding space is not an ordinary metadata update. It requires an explicit reconfiguration/reindex operation so existing vectors are not compared with incompatible vectors. The initial catalog rejects such changes through CollectionReconfigurationRequiredException.

Applications that need independent model spaces should create separate collections in this milestone. A future named-vector/index-profile abstraction can allow multiple spaces inside one collection without weakening compatibility checks.

## Lifecycle

CollectionCatalog provides create, find, list, update and delete operations. The SPI CollectionStore persists immutable descriptors and must implement atomic create semantics. The core DefaultCollectionCatalog owns validation and lifecycle policy; storage implementations own persistence.

InMemoryCollectionStore is suitable for tests and embedded ephemeral use. It is not durable and is not intended for multi-process deployments.

Deleting a collection currently removes its catalog definition only. A persistent implementation must coordinate deletion of dependent vector, lexical and asset records; the full cascade/deletion-job contract will be introduced with the durable index lifecycle rather than hidden inside this in-memory store.
