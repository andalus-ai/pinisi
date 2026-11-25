package ai.andalus.pinisi.api;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Immutable configuration for one isolated search collection. */
public record CollectionDefinition(String id, String name, String description,
        EmbeddingSpace embeddingSpace, Set<MediaType> indexedModalities,
        Map<String, String> metadata) {
    public CollectionDefinition {
        id = requireText(id, "id");
        name = requireText(name, "name");
        description = description == null ? "" : description;
        Objects.requireNonNull(embeddingSpace, "embeddingSpace");
        indexedModalities = Set.copyOf(Objects.requireNonNull(indexedModalities, "indexedModalities"));
        if (indexedModalities.isEmpty()) throw new IllegalArgumentException("indexedModalities must not be empty");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }
}
