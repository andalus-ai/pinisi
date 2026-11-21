package ai.andalus.pinisi.api;

import java.util.Map;
import java.util.Objects;

public record EmbeddingInput(String id, EmbeddingContent content, Map<String, String> metadata) {
    public EmbeddingInput {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
        Objects.requireNonNull(content, "content");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public EmbeddingInput(String id, EmbeddingContent content) {
        this(id, content, Map.of());
    }
}
