package ai.andalus.pinisi.spi;

import ai.andalus.pinisi.api.EmbeddingCapabilities;
import ai.andalus.pinisi.api.EmbeddingRequest;
import ai.andalus.pinisi.api.EmbeddingResponse;

/**
 * Provider SPI for typed, multimodal and batch embedding.
 * Implementations must return one result for each accepted input and preserve input IDs.
 */
public interface MultimodalEmbeddingProvider {
    String id();

    EmbeddingCapabilities capabilities();

    EmbeddingResponse embed(EmbeddingRequest request);
}
