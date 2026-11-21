# Module Structure

The structure below is a target decomposition. Create modules incrementally when there is a concrete implementation and test; do not add empty modules merely to match the diagram.

```text
pinisi/
  pinisi-api/
  pinisi-spi/
  pinisi-core/
  embedding/
    embedding-api/
    embedding-spi/
    embedding-aljabr/
  ingestion/
    ingestion-core/
    ingestion-image/
    ingestion-audio/
    ingestion-video/
    ingestion-document/
  indexing/
    index-core/
    index-metadata/
    index-temporal/
  retrieval/
    retrieval-dense/
    retrieval-lexical/
    retrieval-hybrid/
    retrieval-ranking/
  connectors/
    connector-filesystem/
    connector-s3/
    connector-gcs/
    connector-azure/
  vector/
    vector-memory/
    vector-faiss/
    vector-pgvector/
    vector-qdrant/
  runtime/
    runtime-core/
    runtime-server/
    runtime-worker/
    runtime-cli/
  integrations/
    integrations-mcp/
    integrations-andalus/
  sdk/
  examples/
  docs/
```

## Module responsibilities
- `pinisi-api`: stable public domain and application-facing contracts.
- `pinisi-spi`: provider extension points; depends only on API and minimal shared abstractions.
- `pinisi-core`: orchestration and default engine implementation; no direct dependency on optional provider modules.
- `embedding-*`: modality-aware embedding contracts and provider adapters.
- `ingestion-*`: discovery/processing orchestration and modality-specific processors.
- `index-*`: metadata, temporal and lifecycle indexing abstractions/implementations.
- `retrieval-*`: candidate generation, fusion, filtering and ranking strategies.
- `connector-*`: source-specific access and change detection.
- `vector-*`: vector-store adapters.
- `runtime-*`: lifecycle and deployment adapters.
- `integrations-* `: external protocol/framework adapters.

## Dependency rules
1. API has no implementation dependencies.
2. SPI depends on API, never on concrete providers.
3. Core depends on API/SPI and internal abstractions only.
4. Provider modules depend inward on API/SPI; core must not import provider implementations.
5. Runtime and integration modules depend on core/API and their optional host frameworks.
6. No dependency from Pinisi core to Andalus.
7. Keep cloud SDKs, codecs, vector libraries and server frameworks scoped to their adapters.

## Build layout
The initial build should be a single multi-module Maven reactor unless repository maintainers choose otherwise. Centralize Java/toolchain and dependency versions in the parent build. Avoid splitting into many independently versioned artifacts until there is a demonstrated release need.
