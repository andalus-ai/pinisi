# M1: Java Foundation

## Delivered
- Java 21 Maven reactor with public API, provider SPI, core engine, filesystem connector and example modules.
- Immutable asset, embedding-space, vector, request and result values.
- Explicit embedding-space equality as the compatibility gate; equal dimensions alone never permit comparisons.
- Embedding, image-embedding, media-source and vector-index extension interfaces.
- In-memory cosine index for tests and local prototypes.
- Embedded image-search application service with a declared text-to-image capability check.
- Recursive local image discovery and streaming ingestion through an image embedding provider.

## Explicit limitations
This is a contract and ingestion foundation, not a production image-search release. Filesystem discovery and streaming are implemented, but there is no image decoder/model adapter, persistent index, durable job/checkpoint state, server runtime, or watcher/retry loop. Test vectors are synthetic. EmbeddingGemma 2 remains a candidate pending verification of its supported text/image input and output pairings in the chosen inference runtime.

See [local filesystem ingestion](M1_FILESYSTEM_INGESTION.md) for connector behavior and security boundaries.

## Build
Run `mvn test` with JDK 21 or newer. No Andalus dependency is present or required.
