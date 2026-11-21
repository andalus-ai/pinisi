# Ingestion Architecture

## Pipeline
1. Source registration and capability discovery.
2. Discovery with pagination and stable object identity.
3. Change detection using provider version/ETag and/or content fingerprint.
4. Asset upsert and ingestion job scheduling.
5. Bounded-memory media processing and representation extraction.
6. Segment and metadata normalization.
7. Embedding generation for configured embedding spaces.
8. Vector, metadata and optional lexical index persistence.
9. Completion/checkpoint update and metrics emission.

## Idempotency
A repeated event or retry for the same source object version must not create duplicate assets, segments or vectors. Use deterministic identity keys or persisted source-version mappings. Writes should be upsert-oriented. Checkpoint only after durable writes have completed.

## Updates and deletion
When a source object changes, reprocess affected representations and reconcile stale segments/embeddings. When a source object is deleted, propagate tombstones to all indexes. Define behavior for temporarily unavailable sources separately from confirmed deletion.

## Job lifecycle
Jobs need identifiers, states (queued, running, succeeded, partially succeeded, failed, cancelled), progress counters, timestamps, error summaries, cancellation and retry policy. Production runtimes persist job state and checkpoints. A process crash must not silently mark an incomplete job as complete.

## Media processing
- Images: decode safely, normalize according to provider requirements, preserve dimensions/orientation and optional regions.
- Audio: inspect codec/sample rate/channels/duration; resample/segment only according to provider requirements; preserve source timestamps.
- Video: inspect container/streams, sample frames by configurable policy (time interval, scene change or adaptive sampling), extract audio independently, preserve timestamp alignment and segment-to-asset links.
- Documents: extract text/layout/tables/images where supported; preserve page and section coordinates.
- Text: normalize and segment with source offsets; avoid assuming fixed character chunks fit every language/content type.

## Resource limits and security
Set maximum file sizes, durations, dimensions, decompressed sizes, processing time, concurrency and temporary-disk usage. Treat media as untrusted input. Avoid unsafe parser defaults and archive/path traversal. Keep credentials out of logs and metadata. Support cancellation and cleanup of temporary artifacts.

## First vertical slice
Local filesystem discovery -> image processor -> embedding provider -> vector store + metadata store -> text-to-image query. Keep the pipeline generic enough that audio/video processors can reuse it.
