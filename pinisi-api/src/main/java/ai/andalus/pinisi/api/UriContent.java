package ai.andalus.pinisi.api;

import java.net.URI;
import java.util.Objects;

/** A reference to content. Resolution, credentials and network access remain provider/adapter policy. */
public record UriContent(MediaType mediaType, URI uri, String contentType)
        implements EmbeddingContent {
    public UriContent {
        Objects.requireNonNull(mediaType, "mediaType");
        Objects.requireNonNull(uri, "uri");
        Objects.requireNonNull(contentType, "contentType");
        if (mediaType == MediaType.TEXT) throw new IllegalArgumentException("Use TextContent for text");
        if (!uri.isAbsolute()) throw new IllegalArgumentException("uri must be absolute");
        if (contentType.isBlank()) throw new IllegalArgumentException("contentType must not be blank");
    }
}
