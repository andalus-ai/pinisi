package ai.andalus.pinisi.api;

import java.util.Objects;

public record EmbeddingSpace(String id, String provider, String model, String revision,
                             String inputModality, int dimensions, String metric) {
    public EmbeddingSpace {
        if (id == null || id.isBlank() || provider == null || provider.isBlank() ||
            model == null || model.isBlank() || inputModality == null || inputModality.isBlank())
            throw new IllegalArgumentException("Embedding space identity fields are required");
        if (dimensions <= 0) throw new IllegalArgumentException("dimensions must be positive");
        Objects.requireNonNull(metric, "metric");
        revision = revision == null ? "unspecified" : revision;
    }
}
