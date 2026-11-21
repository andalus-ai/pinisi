package ai.andalus.pinisi.api;

import java.util.Objects;

public record SearchRequest(String collectionId, String query, int limit, MediaType targetType) {
    public SearchRequest {
        if (collectionId == null || collectionId.isBlank()) throw new IllegalArgumentException("collectionId is required");
        if (query == null || query.isBlank()) throw new IllegalArgumentException("query is required");
        if (limit < 1 || limit > 1000) throw new IllegalArgumentException("limit must be between 1 and 1000");
        Objects.requireNonNull(targetType, "targetType");
    }
}
