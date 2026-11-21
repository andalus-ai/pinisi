package ai.andalus.pinisi.api;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

public record Asset(String id, String collectionId, String sourceId, String sourceKey,
                    MediaType mediaType, URI uri, Map<String, String> metadata) {
    public Asset {
        id = require(id, "id"); collectionId = require(collectionId, "collectionId");
        sourceId = require(sourceId, "sourceId"); sourceKey = require(sourceKey, "sourceKey");
        Objects.requireNonNull(mediaType, "mediaType"); Objects.requireNonNull(uri, "uri");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
    private static String require(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
