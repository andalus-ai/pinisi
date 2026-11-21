package ai.andalus.pinisi.api;

import java.util.Objects;

public record SearchResult(Asset asset, double score) {
    public SearchResult {
        Objects.requireNonNull(asset, "asset");
        if (!Double.isFinite(score)) throw new IllegalArgumentException("score must be finite");
    }
}
