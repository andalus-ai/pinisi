package ai.andalus.pinisi.spi;

import ai.andalus.pinisi.api.Asset;
import ai.andalus.pinisi.api.EmbeddingVector;
import java.util.List;

public interface VectorIndex {
    void upsert(Asset asset, EmbeddingVector vector);
    List<Match> search(String collectionId, EmbeddingVector query, int limit);
    void delete(String assetId);
    record Match(Asset asset, double score) {}
}
