# Pinisi

**Pinisi is an independent, Java-native multimodal search and retrieval platform.**

It indexes and searches content where it lives—local filesystems and supported cloud/object stores—without requiring users to migrate their original media into a proprietary store.

Pinisi is designed as both:
- An embeddable Java library for applications that need search capabilities in-process.
- An independently deployable search runtime with APIs and background ingestion workers.

## What Pinisi is responsible for

- Source discovery and incremental ingestion.
- Image, audio, video, document and text processing.
- Asset, segment, metadata and embedding lifecycle.
- Dense, lexical, hybrid and temporal retrieval.
- Ranking, filtering and result aggregation.
- Search APIs, SDK, CLI and runtime.
- Source connectors and index-provider extension points.

## What Pinisi is not

Pinisi is not a general-purpose agent framework, a vector database, or a RAG-only library. It does not own agent planning or workflows, and it does not require ownership or migration of original media.

## Ecosystem

| Project | Responsibility |
|---|---|
| **Pinisi** | Multimodal search, ingestion, indexing and retrieval |
| **Andalus** | Neutral, general-purpose agentic platform and extension framework |
| **Aljabr** | ML/compute abstractions, tensor operations and embedding/model APIs |
| **AQLI** | Model inference execution, scheduling and serving |

Pinisi can integrate with Andalus through optional tools/extensions and MCP interfaces. Pinisi core must remain usable without Andalus. Aljabr/AQLI integrations are provider adapters, not mandatory dependencies of the public API.

## Architecture documentation

- [Architecture overview](docs/architecture/OVERVIEW.md)
- [System boundaries](docs/architecture/SYSTEM_BOUNDARIES.md)
- [Module structure](docs/architecture/MODULE_STRUCTURE.md)
- [Runtime model](docs/architecture/RUNTIME_MODEL.md)
- [Domain model](docs/api/DOMAIN_MODEL.md)
- [Search API direction](docs/api/SEARCH_API.md)
- [Extension SPI](docs/api/EXTENSION_SPI.md)
- [Ingestion architecture](docs/ingestion/INGESTION_ARCHITECTURE.md)
- [Retrieval architecture](docs/retrieval/RETRIEVAL_ARCHITECTURE.md)
- [Roadmap](docs/roadmap/ROADMAP.md)
- [Repository contribution guidance](AGENTS.md)

## Implementation status

The architecture foundation is documented in [PR #1](https://github.com/andalus-ai/pinisi/pull/1), and the Java/Maven foundation is in [PR #2](https://github.com/andalus-ai/pinisi/pull/2). The typed multimodal embedding API is in [PR #6](https://github.com/andalus-ai/pinisi/pull/6). This branch adds provider registry and request orchestration: capability validation, bounded batch splitting, result identity/order checks and embedding-space validation.

This remains an early implementation: no production Aljabr/AQLI embedding adapter or persistent vector store is bundled. EmbeddingGemma 2 remains under evaluation for the required text/image modality pairings and intended inference runtime.

## Design principles

1. **Model-independent:** no single embedding model is hardcoded into Pinisi.
2. **Storage-independent:** original media remains under user control.
3. **Runtime-independent:** embedded and standalone usage share the same engine.
4. **Provider-neutral:** vector stores, metadata stores, processors and connectors are replaceable.
5. **Provenance-preserving:** search results retain source identity and segment positions.
6. **Capability-driven:** cross-modal search is enabled only when embedding providers declare compatible modality pairings.
7. **Permission-aware:** search must not expose objects the caller is not authorized to access.
