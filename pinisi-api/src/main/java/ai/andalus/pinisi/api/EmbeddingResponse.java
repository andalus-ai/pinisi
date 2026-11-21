package ai.andalus.pinisi.api;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record EmbeddingResponse(List<EmbeddingResult> results) {
    public EmbeddingResponse {
        Objects.requireNonNull(results, "results");
        if (results.isEmpty()) throw new IllegalArgumentException("results must not be empty");
        results = List.copyOf(results);
        var ids = new HashSet<String>();
        for (var result : results) {
            Objects.requireNonNull(result, "result");
            if (!ids.add(result.inputId())) throw new IllegalArgumentException("result input ids must be unique");
        }
    }
}
