package ai.andalus.pinisi.api;

import java.util.Objects;

public record EmbeddingResult(String inputId, EmbeddingVector embedding) {
    public EmbeddingResult {
        if (inputId == null || inputId.isBlank()) throw new IllegalArgumentException("inputId must not be blank");
        Objects.requireNonNull(embedding, "embedding");
    }
}
