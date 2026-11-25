package ai.andalus.pinisi.api;

import java.time.Instant;
import java.util.Objects;

/** Persisted collection definition and lifecycle timestamps. */
public record CollectionDescriptor(CollectionDefinition definition, Instant createdAt, Instant updatedAt) {
    public CollectionDescriptor {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (updatedAt.isBefore(createdAt)) throw new IllegalArgumentException("updatedAt must not be before createdAt");
    }
    public String id() { return definition.id(); }
}
