package ai.andalus.pinisi.api;

/**
 * Identity and comparison semantics for a vector space.
 *
 * <p>A space is not tied to one input modality: multimodal models may map text,
 * images and other modalities into the same compatible space. The modality of
 * each individual vector is carried by {@link EmbeddingVector}.</p>
 */
public record EmbeddingSpace(
        String id,
        String provider,
        String model,
        String revision,
        int dimensions,
        String metric) {

    public EmbeddingSpace {
        id = requireText(id, "id");
        provider = requireText(provider, "provider");
        model = requireText(model, "model");
        revision = revision == null || revision.isBlank() ? "unspecified" : revision;
        if (dimensions <= 0) throw new IllegalArgumentException("dimensions must be positive");
        metric = requireText(metric, "metric");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
