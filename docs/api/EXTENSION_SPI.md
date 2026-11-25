# Extension SPI

Pinisi is provider-neutral. Implementations plug into stable contracts rather than adding provider dependencies to core.

## Required SPI families
- `MediaSourceProvider` / `MediaSource`: connect, list/discover, inspect metadata, open bounded/streaming content, detect changes and expose source capabilities.
- `MediaProcessor`: process a media input into normalized representations, metadata and typed segments.
- `EmbeddingProvider`: legacy text-oriented embedding contract retained for existing integrations.
- `MultimodalEmbeddingProvider`: typed multimodal and batch embedding contract; declares model identity, accepted modalities, compatible query/document modality pairs, batch limits and binary/URI support.
- `VectorStore`: create spaces/indexes, upsert, search, delete and report capabilities.
- `MetadataStore`: persist/query collections, sources, assets, segments and ingestion state.
- `LexicalIndex`: index and query extracted text when enabled.
- `ResultRanker`: rank/fuse candidates without owning source access.
- `JobStateStore`: persist job state and checkpoints for restartable runtimes.

## Multimodal embedding request contract

The public API represents input as a typed `EmbeddingContent`:
- `TextContent` for text.
- `BinaryContent` for caller-supplied bytes with an explicit media type and content type.
- `UriContent` for an absolute content reference. Pinisi core does not fetch the URI; resolution, credentials, network policy and ownership belong to the provider/adapter boundary.

Each `EmbeddingInput` has a stable request-local ID and optional metadata. `EmbeddingRequest` accepts one or more inputs, an optional target embedding-space ID and provider parameters. `EmbeddingResponse` maps result IDs to `EmbeddingVector` values; each vector carries its complete `EmbeddingSpace` identity and its own input modality.

The provider declares:
- Input modalities it accepts.
- Supported query-modality/document-modality pairs.
- Maximum batch size.
- Whether it accepts binary payloads and URI references.
- Provider, model and model-revision identity.

An `EmbeddingSpace` describes the shared vector space: stable space ID, provider/model/revision identity, dimensions and metric semantics. It is intentionally independent of input modality. A multimodal model may map text, image and audio inputs into the same space. `EmbeddingVector.modality` records which modality produced an individual vector.

### Core orchestration

`MultimodalEmbeddingProviderRegistry` registers providers by stable ID, rejects duplicate IDs and exposes explicit query/document pair capability checks. `MultimodalEmbeddingService` resolves the requested provider and:
1. Validates all input modalities and binary/URI transport capabilities before invoking the provider.
2. Splits requests into batches no larger than the provider's declared maximum while preserving request IDs and request parameters.
3. Validates each batch response for missing, foreign or duplicate IDs and restores the original request order.
4. Verifies each returned vector's provider/model/revision and input-modality identity, plus the requested embedding-space ID when one is supplied.

Provider exceptions propagate to the caller; the orchestration layer does not silently retry or change providers. Retry, timeout and cancellation policy belong to the runtime/job execution layer. Pair declarations are queried explicitly and are never inferred from equal vector dimensions.

A batch provider must preserve input IDs and return one embedding per accepted input. URI support is not permission to bypass source authorization: adapters must apply explicit allowlists, credentials and access policy. Binary payload sizes and decoding limits must be bounded by the implementation.

The existing `EmbeddingProvider` and `ImageEmbeddingProvider` remain available during migration. New multimodal integrations should implement `MultimodalEmbeddingProvider`; no existing contract is silently reinterpreted.

## Contract requirements
- Capability declarations must be inspectable before execution.
- Requests carry deadlines/cancellation where applicable.
- Batch and streaming behavior is explicit.
- Provider failures map to stable Pinisi errors.
- Providers must document thread safety and resource ownership.
- Contract tests are shared and run against each implementation.
- Provider identifiers and versions are observable for diagnostics.

## Embedding compatibility

An embedding space is more than a dimension. It includes provider/model identity, model revision, output dimensions and metric semantics. Individual vectors retain their input modality, while the provider declares which query/document modality pairs are compatible. Never merge unrelated spaces based only on equal dimensions. Cross-modal query/document compatibility must be declared explicitly by the provider and validated by Pinisi.

## Source connector contract

A source connector should support pagination and bounded reads, expose stable object identity and version/fingerprint information, and report capabilities such as watch/events, ranged reads and permission metadata. Cloud connectors must not require downloading the entire source into a local permanent mirror.

## Processor contract

Processors extract content and positions; they do not persist vectors. Video processors should preserve frame timestamps and audio/video alignment where available. Audio/video processing must use bounded memory and configurable segmentation. Document processors preserve page/section provenance.

## Optional adapters

Aljabr/AQLI, filesystem, S3/GCS/Azure, FAISS, pgvector, Qdrant, REST, gRPC, MCP and Andalus adapters are optional modules. Their absence must not prevent using the core API with another provider.
