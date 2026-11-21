package ai.andalus.pinisi.api;

import java.util.Objects;

public record TextContent(String text) implements EmbeddingContent {
    public TextContent {
        Objects.requireNonNull(text, "text");
        if (text.isBlank()) throw new IllegalArgumentException("text must not be blank");
    }

    @Override
    public MediaType mediaType() {
        return MediaType.TEXT;
    }
}
