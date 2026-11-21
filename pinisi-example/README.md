# Embedded image-search example

This module demonstrates the embedded engine contract. Supply an `EmbeddingProvider` that supports text-to-image embeddings and a `VectorIndex`; then call `indexImage` and `searchImages`.

No model is bundled in this first foundation commit. The fixture provider in core tests is deliberately synthetic and is not a semantic embedding model. The first production adapter will be selected only after verifying EmbeddingGemma 2 modality support and the Aljabr/AQLI execution path.
