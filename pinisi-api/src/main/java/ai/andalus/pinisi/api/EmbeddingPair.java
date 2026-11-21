package ai.andalus.pinisi.api;

import java.util.Objects;

/** A query modality that can retrieve indexed content of the declared modality. */
public record EmbeddingPair(MediaType queryModality, MediaType documentModality) {
    public EmbeddingPair {
        Objects.requireNonNull(queryModality, "queryModality");
        Objects.requireNonNull(documentModality, "documentModality");
    }
}
