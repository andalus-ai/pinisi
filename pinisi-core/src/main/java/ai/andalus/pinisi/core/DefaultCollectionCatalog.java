package ai.andalus.pinisi.core;

import ai.andalus.pinisi.api.*;
import ai.andalus.pinisi.spi.CollectionStore;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Default collection lifecycle service; persistence is delegated to CollectionStore. */
public final class DefaultCollectionCatalog implements CollectionCatalog {
    private final CollectionStore store;
    private final Clock clock;

    public DefaultCollectionCatalog(CollectionStore store) { this(store, Clock.systemUTC()); }

    public DefaultCollectionCatalog(CollectionStore store, Clock clock) {
        this.store = Objects.requireNonNull(store, "store");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override public CollectionDescriptor create(CollectionDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        Instant now = clock.instant();
        CollectionDescriptor descriptor = new CollectionDescriptor(definition, now, now);
        if (!store.create(descriptor)) throw new CollectionAlreadyExistsException(definition.id());
        return descriptor;
    }

    @Override public Optional<CollectionDescriptor> find(String collectionId) {
        requireId(collectionId);
        return store.find(collectionId);
    }

    @Override public List<CollectionDescriptor> list() { return List.copyOf(store.list()); }

    @Override public CollectionDescriptor update(CollectionDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        CollectionDescriptor current = store.find(definition.id())
                .orElseThrow(() -> new CollectionNotFoundException(definition.id()));
        if (!current.definition().embeddingSpace().equals(definition.embeddingSpace())) {
            throw new CollectionReconfigurationRequiredException(definition.id());
        }
        CollectionDescriptor updated = new CollectionDescriptor(definition, current.createdAt(), clock.instant());
        if (!store.replace(current, updated)) {
            throw new CollectionNotFoundException(definition.id());
        }
        return updated;
    }

    @Override public boolean delete(String collectionId) {
        requireId(collectionId);
        return store.delete(collectionId);
    }

    private static void requireId(String collectionId) {
        if (collectionId == null || collectionId.isBlank()) {
            throw new IllegalArgumentException("collectionId must not be blank");
        }
    }
}
