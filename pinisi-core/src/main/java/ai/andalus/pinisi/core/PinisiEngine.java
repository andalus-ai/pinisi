package ai.andalus.pinisi.core;

import ai.andalus.pinisi.api.*;
import ai.andalus.pinisi.spi.EmbeddingProvider;
import ai.andalus.pinisi.spi.VectorIndex;
import java.util.List;
import java.util.Objects;

public final class PinisiEngine {
    private final EmbeddingProvider embeddings;
    private final VectorIndex index;
    public PinisiEngine(EmbeddingProvider embeddings, VectorIndex index) {
        this.embeddings = Objects.requireNonNull(embeddings); this.index = Objects.requireNonNull(index);
    }
    public void indexImage(Asset asset, EmbeddingVector vector) {
        if (asset.mediaType() != MediaType.IMAGE) throw new IllegalArgumentException("Asset must be an image");
        index.upsert(asset, vector);
    }
    public List<SearchResult> searchImages(SearchRequest request) {
        if (request.targetType() != MediaType.IMAGE) throw new IllegalArgumentException("Only IMAGE search is implemented in this milestone");
        if (!embeddings.supports(MediaType.TEXT, MediaType.IMAGE)) throw new IllegalStateException("Embedding provider does not support text-to-image retrieval");
        EmbeddingVector query = embeddings.embedText(request.query(), MediaType.IMAGE);
        return index.search(request.collectionId(), query, request.limit()).stream()
            .map(m -> new SearchResult(m.asset(), m.score())).toList();
    }
}
