# M1: Java Foundation

## Delivered in this milestone
- Java 21 Maven reactor with separate public API, provider SPI, core engine and example modules.
- Immutable asset, embedding-space, vector, request and result values.
- Explicit embedding-space equality as the compatibility gate; equal dimensions alone never permit comparisons.
- Embedding and vector-index extension interfaces.
- A small in-memory cosine index for tests and local prototypes.
- An embedded image-search application service that requires a declared text-to-image capability.

## Explicit limitations
This is a contract and engine foundation, not a production image-search release. There is no filesystem discovery/decoder, persistent index, server runtime, or real embedding model adapter yet. The test fixture uses synthetic vectors only. EmbeddingGemma 2 remains a candidate pending verification of its supported text/image input and output pairings in the chosen inference runtime.

## Build
Run `mvn test` with JDK 21 or newer. No Andalus dependency is present or required.
