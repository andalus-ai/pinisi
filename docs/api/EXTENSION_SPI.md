# Extension SPI

Pinisi is provider-neutral. Implementations plug into stable contracts rather than adding provider dependencies to core.

## Required SPI families
- `MediaSourceProvider` / `MediaSource`: connect, list/discover, inspect metadata, open bounded/streaming content, detect changes and expose source capabilities.
- `MediaProcessor`: process a media input into normalized representations, metadata and typed segments.
- `EmbeddingProvider`: embed typed inputs and batches; declare model identity, embedding-space identity, dimensions, supported modalities and cross-modal pairings.
- `VectorStore`: create spaces/indexes, upsert, search, delete and report capabilities.
- `MetadataStore`: persist/query collections, sources, assets, segments and ingestion state.
- `LexicalIndex`: index and query extracted text when enabled.
- `ResultRanker`: rank/fuse candidates without owning source access.
- `JobStateStore`: persist job state and checkpoints for restartable runtimes.

## Contract requirements
- Capability declarations must be inspectable before execution.
- Requests carry deadlines/cancellation where applicable.
- Batch and streaming behavior is explicit.
- Provider failures map to stable Pinisi errors.
- Providers must document thread safety and resource ownership.
- Contract tests are shared and run against each implementation.
- Provider identifiers and versions are observable for diagnostics.

## Embedding compatibility
An embedding space is more than a dimension. It includes model/provider identity, model revision, input modality/recipe, preprocessing version, output dimensions and metric semantics. A vector search request must select a compatible space. Never merge unrelated spaces based only on equal dimensions.

## Source connector contract
A source connector should support pagination and bounded reads, expose stable object identity and version/fingerprint information, and report capabilities such as watch/events, ranged reads and permission metadata. Cloud connectors must not require downloading the entire source into a local permanent mirror.

## Processor contract
Processors extract content and positions; they do not persist vectors. Video processors should preserve frame timestamps and audio/video alignment where available. Audio/video processing must use bounded memory and configurable segmentation. Document processors preserve page/section provenance.

## Optional adapters
Aljabr/AQLI, filesystem, S3/GCS/Azure, FAISS, pgvector, Qdrant, REST, gRPC, MCP and Andalus adapters are optional modules. Their absence must not prevent using the core API with another provider.
