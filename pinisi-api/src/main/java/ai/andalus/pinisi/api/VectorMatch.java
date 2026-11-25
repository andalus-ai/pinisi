package ai.andalus.pinisi.api;

import java.util.Objects;

/** A vector search result with its originating record and similarity score. */
public record VectorMatch(VectorRecord record, double score) {
    public VectorMatch {
        Objects.requireNonNull(record, "record");
        if (!Double.isFinite(score)) throw new IllegalArgumentException("score must be finite");
    }
}
