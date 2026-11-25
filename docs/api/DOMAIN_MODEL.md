# Domain Model

## Concepts
- **Collection:** logical search boundary with configuration for modalities, providers, retention and indexing behavior.
- **Source:** configured origin from which objects are discovered.
- **Asset:** one logical original object, identified within a collection and source.
- **Segment:** a searchable subpart of an asset.
- **Representation:** normalized extracted content used by a processor or embedding provider.
- **Embedding space:** shared vector-space identity, including provider/model/revision, dimensions and metric semantics. A multimodal model may map several input modalities into one space.
- **Embedding vector:** one vector in an embedding space, retaining the modality of the input that produced it.
- **Index record:** persisted searchable representation and its links to asset/segment.
- **Query:** typed user input, target modalities, filters and retrieval options.
- **Result:** ranked asset/segment match with score, position and safe metadata.
- **Ingestion job:** durable unit of discovery, processing and indexing work.

## Identifier rules
Use opaque, stable identifiers for collections, sources, assets, segments, jobs and embedding spaces. Do not assume filenames or object keys are globally unique. A source object identity should include the source identifier and provider object key/version as applicable.

## Media types
Initial logical types: TEXT, IMAGE, AUDIO, VIDEO, DOCUMENT. A document may contain text, images, tables and pages; those extracted representations may be indexed independently while preserving document provenance.

## Position model
Use typed position values rather than a single numeric offset:
- Time interval for audio/video (start inclusive, end exclusive, in a documented time unit).
- Page/section/range for documents.
- Optional normalized region/bounding box for images.
- Character/token range for text.

Positions must refer to the original asset timeline/structure, not transient processing buffers.

## Embedding-space identity
An embedding space is not identified by dimension alone. It captures provider/model identity, model revision, dimensions and metric compatibility. Individual vectors additionally record their input modality. Providers declare compatible query/document modality pairs; Pinisi must not infer cross-modal compatibility merely because vectors share dimensions or a model name. A requested target space is validated independently from the input modality.

## Deletion and updates
Asset deletion must remove or tombstone all dependent segments, embeddings and lexical records. Source updates should create a new version/fingerprint and reconcile derived records idempotently. Preserve audit/provenance information according to collection retention policy.
