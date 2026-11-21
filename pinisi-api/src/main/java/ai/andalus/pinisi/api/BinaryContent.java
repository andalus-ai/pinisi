package ai.andalus.pinisi.api;

import java.util.Arrays;
import java.util.Objects;

/** In-memory binary payload. Callers retain ownership of their original array. */
public record BinaryContent(MediaType mediaType, String contentType, byte[] data)
        implements EmbeddingContent {
    public BinaryContent {
        Objects.requireNonNull(mediaType, "mediaType");
        Objects.requireNonNull(contentType, "contentType");
        Objects.requireNonNull(data, "data");
        if (mediaType == MediaType.TEXT) throw new IllegalArgumentException("Use TextContent for text");
        if (contentType.isBlank()) throw new IllegalArgumentException("contentType must not be blank");
        if (data.length == 0) throw new IllegalArgumentException("data must not be empty");
        data = Arrays.copyOf(data, data.length);
    }

    @Override
    public byte[] data() {
        return Arrays.copyOf(data, data.length);
    }
}
