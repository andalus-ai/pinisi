package ai.andalus.pinisi.api;

import java.util.Arrays;
import java.util.Objects;

/**
 * A vector produced from one input modality in a declared embedding space.
 */
public record EmbeddingVector(EmbeddingSpace space, MediaType modality, float[] values) {
    public EmbeddingVector {
        Objects.requireNonNull(space, "space");
        Objects.requireNonNull(modality, "modality");
        Objects.requireNonNull(values, "values");
        if (values.length != space.dimensions()) {
            throw new IllegalArgumentException("Vector length does not match embedding space");
        }
        values = Arrays.copyOf(values, values.length);
        for (float value : values) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("Vector values must be finite");
            }
        }
    }

    @Override
    public float[] values() {
        return Arrays.copyOf(values, values.length);
    }
}
