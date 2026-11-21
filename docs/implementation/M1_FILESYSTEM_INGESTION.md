# M1: Local Filesystem Image Ingestion

## Implemented
- Read-only local filesystem image source for JPG/JPEG, PNG, GIF, WebP, BMP and TIFF extensions.
- Recursive discovery, deterministic asset IDs derived from source ID and relative key, basic file metadata and file URIs.
- Source-scoped reads that reject paths outside the configured root; symbolic links are not followed.
- An ingestion service that streams each image to an image-capable embedding provider and upserts the resulting vector.
- Unit tests for discovery, stable identity, reading and source isolation.

## Provider contract
The filesystem connector does not decode image pixels or create embeddings. An implementation of ImageEmbeddingProvider owns decoding/preprocessing and model inference. The stream is opened per asset and closed by the ingestion service after the provider returns.

## Current limitations
This is a synchronous first slice. It does not yet persist checkpoints, detect content changes beyond rediscovery, schedule retries, watch directories, or store vectors durably. A production source identity should be configured uniquely per root. The initial collection ID is default in this prototype and will become explicit configuration before the first release.

No model adapter is bundled. A provider must separately demonstrate compatible image-input and text-query embedding spaces before semantic search is considered functional.