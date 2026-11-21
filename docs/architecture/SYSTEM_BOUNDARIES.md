# System Boundaries

## Product responsibilities

| Project | Owns | Does not own |
|---|---|---|
| Pinisi | Search domain, ingestion, media processing, indexing, retrieval, ranking, search lifecycle and search runtime | Agent planning, general workflows, model training, original-media ownership |
| Andalus | General-purpose agentic framework, orchestration, workflows, tools/MCP infrastructure and extension lifecycle | Pinisi's indexing and retrieval internals |
| Aljabr | ML/compute abstractions, tensor operations, embedding/model APIs and backend integrations | Search collections, source crawling, search ranking |
| AQLI | Model loading/execution, inference scheduling, batching and serving | Media catalog, ingestion lifecycle, search query planning |

## Dependency direction
```text
Applications / Andalus agents / external MCP clients
                     |
            Pinisi SDK / APIs
                     |
               Pinisi Core
                     |
               Pinisi SPIs
          /          |          \
   Source SPI   Embedding SPI   Index SPIs
       |             |              |
  Connectors   Aljabr adapter   Vector/metadata/
                    |           lexical providers
                   AQLI
```

The diagram describes logical relationships, not mandatory runtime deployment. Pinisi core must remain usable when Andalus is absent. AQLI can be local or remote. Aljabr adapters are optional.

## Andalus integration
Pinisi may publish:
- An Andalus extension/plugin that registers Pinisi-backed capabilities.
- Tool definitions such as search, asset lookup, and segment lookup.
- An MCP server exposing a carefully scoped search interface.

Andalus owns its generic tool registration, agent execution and MCP framework. Pinisi owns the tool implementation and search semantics. Do not duplicate an agent runtime or general MCP framework inside Pinisi.

## Aljabr and AQLI integration
Pinisi defines an embedding-provider SPI around modality-aware requests and results. An optional adapter maps that SPI to Aljabr APIs and AQLI execution. Embedding inference must not be modeled as token generation. Model-specific preprocessing and supported modality pairings must be declared by capabilities.

## Storage and ownership
Original objects remain in local filesystems or user-selected cloud/object stores. Pinisi persists identifiers, source references, fingerprints/version markers, extracted metadata, segments and indexes. Temporary caches may be used under explicit policies. Credentials must be handled by connector configuration/secret providers and must not be copied into index metadata or logs.

## Security boundary
Search results must not bypass source authorization. The runtime must define whether permissions are checked at ingestion, query time, or both; production connectors should preserve access metadata and support revalidation where the source supports it. Signed URLs and credentials must not be persisted as durable public references.
