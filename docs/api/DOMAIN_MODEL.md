# Domain Model

## Concepts
- **Collection:** logical search boundary with configuration for modalities, providers, retention and indexing behavior.
- **Source:** configured origin from which objects are discovered.
- **Asset:** one logical original object, identified within a collection and source.
- **Segment:** a searchable subpart of an asset.
- **Representation:** normalized extracted content used by a processor or embedding provider.
- **Embedding:** vector plus explicit model/space identity, version, dimensions, metric and preprocessing provenance.
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
An embedding record must capture at least provider/model identifier, model revision, output dimensions, modality/input recipe, preprocessing version and similarity metric compatibility. Vectors from distinct spaces must not be compared unless a provider explicitly declares compatible spaces or a transformation is applied.

## Deletion and updates
Asset deletion must remove or tombstone all dependent segments, embeddings and lexical records. Source updates should create a new version/fingerprint and reconcile derived records idempotently. Preserve audit/provenance information according to collection retention policy.
