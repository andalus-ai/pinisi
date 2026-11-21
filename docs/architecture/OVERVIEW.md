# Pinisi Architecture Overview

## Purpose
Pinisi is a Java-native multimodal search and retrieval platform for indexing and searching content in user-controlled local and cloud storage. It provides an embeddable library and an independently deployable runtime over the same search core.

The first target modalities are text, images, audio, video, and documents. Search can cross modalities where the selected embedding provider supports the requested query/target pairing.

## Goals
- Provide one coherent API for collection management, ingestion, indexing, and search.
- Search media by semantic meaning, lexical content, metadata, and temporal location.
- Preserve links to original source objects instead of requiring media migration.
- Support pluggable media processors, embedding providers, vector stores, metadata stores, and source connectors.
- Run embedded in Java applications or as a standalone/distributed service.
- Integrate with Andalus as an optional consumer-facing extension, tool provider, or MCP server.
- Use Aljabr and AQLI integrations without coupling core to a specific model or inference runtime.

## Non-goals
- Replacing Andalus as a general-purpose agent framework.
- Owning agent conversations, planning, workflows, or general tool orchestration.
- Becoming a proprietary object store for original media.
- Requiring a specific vector database or embedding model.
- Treating all media as plain text chunks.
- Claiming universal cross-modal search when a selected model does not support that modality pairing.

## Logical architecture
1. **Public API and domain:** collections, assets, segments, queries, results, jobs, identifiers and capabilities.
2. **Core orchestration:** collection lifecycle, ingestion coordination, query planning, retrieval orchestration and result assembly.
3. **SPIs:** source providers, media processors, embedding providers, vector stores, metadata stores, lexical indexes, rankers and job-state stores.
4. **Provider modules:** local/cloud connectors, media decoders, model adapters and database adapters.
5. **Runtime adapters:** embedded facade, server/API, background workers and CLI.
6. **Integrations:** MCP and optional Andalus tools/extensions.

## Core data flow
### Ingestion
Source discovery -> change detection -> bounded-memory media processing -> segment/representation extraction -> embedding generation -> metadata/vector/lexical persistence -> indexed state.

### Search
Query validation -> query representation generation -> candidate retrieval from applicable strategies -> metadata/permission filtering -> candidate fusion -> optional reranking -> result aggregation.

## Key domain distinctions
- Collection: logical namespace and configuration boundary.
- Source: external system or location from which objects are discovered.
- Asset: one original logical object.
- Segment: a searchable part of an asset.
- Representation: extracted text, frame, waveform/audio input, transcript, page, or other normalized content supplied to a processor/provider.
- Embedding: vector plus explicit embedding-space identity and provenance.
- Search result: ranked asset/segment match with score, position, highlights and safe source reference.

## Runtime modes
- **Embedded:** caller constructs and closes an engine in-process.
- **Standalone:** API server and worker processes expose the same core.
- **Distributed:** API, ingestion workers, schedulers and persistence scale independently. Distributed execution is a later deployment profile, not a prerequisite for the first release.

## Initial vertical slice
Prove local image ingestion and text-to-image retrieval end-to-end using one supported embedding provider and one vector backend. Keep contracts modality-neutral so audio/video can follow without redesigning the public API.
