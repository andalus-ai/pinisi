package ai.andalus.pinisi.core;

import ai.andalus.pinisi.api.CollectionDescriptor;
import ai.andalus.pinisi.spi.CollectionStore;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Thread-safe, process-local collection store for embedded use and tests. */
public final class InMemoryCollectionStore implements CollectionStore {
    private final ConcurrentMap<String, CollectionDescriptor> collections = new ConcurrentHashMap<>();

    @Override public boolean create(CollectionDescriptor descriptor) {
        return collections.putIfAbsent(descriptor.id(), descriptor) == null;
    }
    @Override public Optional<CollectionDescriptor> find(String collectionId) {
        return Optional.ofNullable(collections.get(collectionId));
    }
    @Override public List<CollectionDescriptor> list() {
        return collections.values().stream().sorted(Comparator.comparing(CollectionDescriptor::id)).toList();
    }
    @Override public boolean replace(CollectionDescriptor expected, CollectionDescriptor replacement) {
        return collections.replace(expected.id(), expected, replacement);
    }
    @Override public boolean delete(String collectionId) { return collections.remove(collectionId) != null; }
}
