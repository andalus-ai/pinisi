# Pinisi Roadmap

This roadmap describes intended milestones, not implemented features. Release readiness requires tests, documentation, operational limits and measured retrieval quality.

## M0 — Architecture foundation
- [x] Define product boundaries and runtime modes.
- [x] Define domain concepts and API/SPI direction.
- [x] Define ingestion and retrieval architecture.
- [x] Establish repository guidance and documentation.
- [ ] Review and ratify contracts with maintainers.

## M1 — Core and local image-search vertical slice
- [ ] Establish Maven reactor and Java/toolchain policy.
- [ ] Implement public domain/API and SPI contracts.
- [ ] Implement engine orchestration and collection lifecycle.
- [ ] Implement filesystem source connector.
- [ ] Implement metadata persistence and in-memory vector provider for tests.
- [ ] Integrate one real embedding provider through the provider SPI.
- [ ] Implement image processing and text-to-image retrieval.
- [ ] Add contract tests, integration tests and a reproducible example.

## M2 — Persistent indexing and production ingestion
- [ ] Add a persistent vector backend.
- [ ] Add durable job state, checkpoints, retries and cancellation.
- [ ] Add incremental update/delete reconciliation.
- [ ] Add REST server and CLI.
- [ ] Add operational metrics, health checks and limits.

## M3 — Audio, video and document retrieval
- [ ] Add audio processing, segmentation and temporal positions.
- [ ] Add video frame/audio extraction and timestamp-preserving segments.
- [ ] Add document parsing and structural positions.
- [ ] Add transcript/lexical retrieval and hybrid fusion.
- [ ] Add cross-modal evaluation benchmarks.

## M4 — Connectors and ecosystem integrations
- [ ] Add S3-compatible, GCS and Azure connectors.
- [ ] Add additional vector and metadata backends.
- [ ] Add MCP server.
- [ ] Add optional Andalus extension/tools adapter.
- [ ] Add distributed worker deployment profile.

## Release gates
- SPI contract tests pass for each provider.
- Repeated ingestion is idempotent; updates/deletes reconcile every index.
- Long-running jobs recover after restart.
- Media processing has documented resource limits and safe failure behavior.
- Permissions are enforced for search results.
- Each advertised modality/pairing has evaluation evidence.
- Public API and compatibility policy are documented.
