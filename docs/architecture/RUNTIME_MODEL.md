# Runtime Model

Pinisi exposes one engine contract through multiple runtime profiles.

## Embedded
A Java application constructs a Pinisi engine with explicit provider instances or a provider registry. The application owns lifecycle and resource closure. No HTTP server, agent framework or background daemon is required.

## Standalone
A server adapter exposes versioned REST and optionally gRPC endpoints. A worker adapter runs ingestion and maintenance jobs. The server and worker may initially run in one process, but their responsibilities should remain separable.

## Distributed
A later deployment profile may separate API nodes, schedulers, ingestion workers and query workers. Durable job state, shared metadata/vector stores, leases and idempotent workers are prerequisites. Do not claim distributed safety for an in-memory implementation.

## Lifecycle
- Engine construction validates required providers and capabilities.
- Startup performs provider health checks where supported.
- Jobs have explicit states, progress, cancellation and terminal outcomes.
- Shutdown stops accepting new work, applies a configurable drain policy, closes owned resources and leaves durable jobs recoverable.
- Resource ownership must be explicit: Pinisi closes resources it creates, not arbitrary caller-owned providers unless configured.

## API transport
REST is the first external transport. gRPC can be added when streaming or high-throughput use cases justify it. The Java SDK should call the same application service layer, not a separate implementation.

## CLI
The CLI is a thin client of the runtime API where possible. It should not implement a second ingestion or retrieval engine.
