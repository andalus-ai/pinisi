# Retrieval Architecture

## Query execution
1. Validate query and collection.
2. Resolve target modalities and compatible embedding spaces.
3. Produce query representations through declared providers.
4. Execute dense and/or lexical candidate retrieval.
5. Apply collection, metadata and authorization filters.
6. Fuse candidate lists.
7. Optionally rerank candidates.
8. Aggregate segment hits into asset-level results when requested.
9. Return stable result metadata, positions, scores and pagination information.

## Strategies
- Dense semantic retrieval for supported embedding spaces.
- Lexical retrieval for exact terms, identifiers, transcripts and extracted text.
- Hybrid retrieval combining dense and lexical candidates.
- Temporal retrieval for matching audio/video intervals.
- Metadata filtering for structured constraints.
- Optional reranking for improved precision.

## Fusion and scoring
Start with a documented baseline such as Reciprocal Rank Fusion for combining independently ranked lists. Preserve per-strategy scores for diagnostics where useful. Do not present uncalibrated similarity values as probabilities. Rerankers must be optional and bounded by configured latency/cost.

## Cross-modal rules
Cross-modal retrieval is provider-capability-driven. A shared embedding space may enable text-to-image or text-to-audio retrieval, but Pinisi must not assume all modalities share a compatible space. If no compatible path exists, return a clear unsupported-pairing error rather than silently falling back to unrelated vectors.

## Video and audio
Return matching segments with start/end timestamps and parent asset identity. Support segment-level results and optional aggregation to a video/audio asset. A video-wide embedding may be used as a coarse candidate signal, but should not replace temporal segment indexing for precise moment retrieval.

## Evaluation
Maintain representative datasets and measure recall@k, precision@k, MRR/nDCG, latency, indexing throughput, memory/disk use and update/delete correctness. Evaluate each modality and cross-modal pairing independently.
