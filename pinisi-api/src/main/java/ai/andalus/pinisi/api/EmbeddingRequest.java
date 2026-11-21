package ai.andalus.pinisi.api;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** One or more inputs to embed in a single provider invocation. */
public record EmbeddingRequest(
        List<EmbeddingInput> inputs,
        String targetEmbeddingSpaceId,
        Map<String, String> parameters) {
    public EmbeddingRequest {
        Objects.requireNonNull(inputs, "inputs");
        if (inputs.isEmpty()) throw new IllegalArgumentException("inputs must not be empty");
        inputs = List.copyOf(inputs);
        var ids = new HashSet<String>();
        for (var input : inputs) {
            Objects.requireNonNull(input, "input");
            if (!ids.add(input.id())) throw new IllegalArgumentException("input ids must be unique");
        }
        if (targetEmbeddingSpaceId != null && targetEmbeddingSpaceId.isBlank())
            throw new IllegalArgumentException("targetEmbeddingSpaceId must not be blank");
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }

    public EmbeddingRequest(List<EmbeddingInput> inputs) {
        this(inputs, null, Map.of());
    }
}
