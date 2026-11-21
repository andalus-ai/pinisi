# Repository guidance for Pinisi

## Mission and product boundary
Pinisi is an independent, Java-native multimodal search and retrieval platform. It owns source discovery, ingestion, media processing, representation generation, indexing, retrieval, ranking, search APIs, and search lifecycle.

Pinisi is not an agent framework, vector database, or RAG-only library. It must be useful as an embedded Java library and as an independently deployed service.

## System boundaries
- **Pinisi** owns search domain models, collections, assets, segments, ingestion jobs, connectors used for indexing, indexes, retrieval, ranking, and search APIs.
- **Andalus** is a neutral, general-purpose agentic platform. Pinisi may expose optional Andalus tools/extensions, but Pinisi core must not depend on Andalus runtime, agent, workflow, or MCP implementation modules.
- **Aljabr** provides reusable ML/compute and embedding abstractions/implementations.
- **AQLI** provides model inference execution and serving. Pinisi must use embedding-oriented inference contracts rather than pretending embedding generation is text completion.
- Original media remains under user control. Pinisi stores references and derived index data; copying originals into Pinisi-managed storage is not the default.

## Dependency rules
1. Public APIs and core domain must not expose third-party database, vector engine, media codec, cloud SDK, framework, or runtime types.
2. Keep API, SPI, core, provider implementations, and runtime adapters in separate modules.
3. Core may depend on API/SPI, but API/SPI must not depend on implementations.
4. Cloud connectors, vector stores, media processors, embedding providers, server frameworks, MCP, and Andalus integrations are optional modules.
5. No mandatory dependency on Andalus.
6. No mandatory dependency on a particular embedding model, vector database, metadata database, cloud provider, or deployment framework.
7. Avoid circular module dependencies. Runtime and integrations adapt to core; core never calls runtime/integration modules.
8. Keep provider-specific configuration out of generic domain types.

## Domain invariants
- A **collection** is a logical search boundary.
- An **asset** represents one original logical object in a source.
- A **segment** represents a searchable part of an asset (for example a video interval, audio interval, document page/chunk, or image region).
- An **embedding** is a model-produced representation of an asset or segment and belongs to a named embedding space.
- A vector's dimension alone does not establish compatibility. Embedding-space/model identity, version, preprocessing and metric compatibility must be explicit.
- Preserve source identity, object key, version/fingerprint, provenance, and access metadata.
- Preserve temporal positions for audio/video and structural positions for documents.
- Search results identify both the asset and, when applicable, the matching segment and position.
- Never silently compare vectors from incompatible embedding spaces.

## API design
- Prefer immutable records/value objects for public requests and responses.
- Validate identifiers and request invariants at API boundaries.
- Use typed domain values rather than unstructured maps where practical; reserve extension metadata maps for provider-specific or user-defined metadata.
- Support synchronous embedded usage and asynchronous/long-running operations without forcing a reactive framework on API consumers.
- Define cancellation, progress, pagination, timeouts, and error semantics explicitly.
- Do not expose raw vector arrays in ordinary search results.
- Keep query input modality distinct from target modalities and retrieval options.

## Ingestion and processing
- Ingestion must be idempotent and restartable.
- Long-running jobs need status, progress, cancellation, retry/recovery semantics, and durable state in production runtimes.
- Detect source changes using provider version identifiers/ETags where available and content fingerprints as appropriate.
- Process large audio/video using streaming or bounded-memory techniques; do not require whole-media buffering.
- Processing extracts normalized representations, metadata, and segments. Persistence/indexing is a separate concern.
- Deletion and source tombstones must propagate to all relevant indexes.
- Respect source permissions and avoid returning inaccessible objects.

## Retrieval
- Keep dense, lexical, hybrid, temporal, filtering, fusion, and reranking strategies composable.
- Start with simple, measurable baselines (for example cosine/dot-product dense retrieval and RRF fusion) before adding complex ranking.
- Return stable scores with documented semantics; do not imply scores are probabilities unless calibrated.
- Search should support modality constraints, metadata filters, result limits, and pagination.

## Implementation and quality
- Follow the repository's established build and Java conventions once introduced; do not invent parallel conventions.
- Add unit tests for domain invariants and contract tests for every SPI/provider.
- Add integration tests for each supported backend and source connector.
- Add cross-modal evaluation fixtures and retrieval-quality benchmarks before claiming modality support production-ready.
- Test failure, cancellation, retry, duplicate ingestion, changed source objects, deleted objects, and partial indexing.
- Document required native binaries, codecs, model files, licenses, and platform limitations.
- Keep examples runnable and minimal.
- Do not claim a feature is implemented merely because an interface or roadmap entry exists.

## Documentation expectations
Architecture decisions belong in `docs/architecture/`; API contracts in `docs/api/`; ingestion and retrieval details in their respective directories. Update docs and examples when public contracts change.
