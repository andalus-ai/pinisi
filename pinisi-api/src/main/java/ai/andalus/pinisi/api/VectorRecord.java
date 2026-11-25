package ai.andalus.pinisi.api;

import java.util.Map;
import java.util.Objects;

/** One vector associated with an asset or a future media segment. */
public record VectorRecord(String id, Asset asset, EmbeddingVector embedding,
                           Map<String, String> metadata) {
    public VectorRecord {
        id = requireText(id, "id");
        Objects.requireNonNull(asset, "asset");
        Objects.requireNonNull(embedding, "embedding");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        if (asset.mediaType() != embedding.modality()) {
            throw new IllegalArgumentException("Asset media type must match embedding modality");
        }
    }

    public String collectionId() {
        return asset.collectionId();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
