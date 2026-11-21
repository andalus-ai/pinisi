package ai.andalus.pinisi.api;

import java.util.Objects;
import java.util.Set;

/** Provider-declared capabilities; compatibility is explicit and never inferred from vector dimensions. */
public record EmbeddingCapabilities(
        String providerId,
        String modelId,
        String modelRevision,
        Set<MediaType> inputModalities,
        Set<EmbeddingPair> supportedPairs,
        int maxBatchSize,
        boolean supportsBinaryContent,
        boolean supportsUriReferences) {
    public EmbeddingCapabilities {
        if (providerId == null || providerId.isBlank()) throw new IllegalArgumentException("providerId is required");
        if (modelId == null || modelId.isBlank()) throw new IllegalArgumentException("modelId is required");
        modelRevision = modelRevision == null ? "unspecified" : modelRevision;
        inputModalities = Set.copyOf(Objects.requireNonNull(inputModalities, "inputModalities"));
        supportedPairs = Set.copyOf(Objects.requireNonNull(supportedPairs, "supportedPairs"));
        if (inputModalities.isEmpty()) throw new IllegalArgumentException("inputModalities must not be empty");
        if (maxBatchSize <= 0) throw new IllegalArgumentException("maxBatchSize must be positive");
    }
}
