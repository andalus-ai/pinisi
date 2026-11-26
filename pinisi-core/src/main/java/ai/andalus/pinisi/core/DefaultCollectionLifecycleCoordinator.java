package ai.andalus.pinisi.core;

import ai.andalus.pinisi.api.CollectionCatalog;
import ai.andalus.pinisi.spi.CollectionDataStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Coordinates collection deletion across all collection-owned stores.
 *
 * <p>Stores are cleaned before the catalog entry is removed. If a store fails,
 * the catalog remains available and the exception identifies completed work.
 * Every store must provide idempotent collection deletion so callers can safely
 * retry the operation after partial progress.</p>
 */
public final class DefaultCollectionLifecycleCoordinator {
    private final CollectionCatalog catalog;
    private final List<CollectionDataStore> stores;

    public DefaultCollectionLifecycleCoordinator(CollectionCatalog catalog,
                                                  Collection<? extends CollectionDataStore> stores) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(stores, "stores");
        var copy = new ArrayList<CollectionDataStore>(stores.size());
        var ids = new HashSet<String>();
        for (CollectionDataStore store : stores) {
            Objects.requireNonNull(store, "store");
            String id = store.storeId();
            if (id == null || id.isBlank()) throw new IllegalArgumentException("storeId must not be blank");
            if (!ids.add(id)) throw new IllegalArgumentException("Duplicate collection store id: " + id);
            copy.add(store);
        }
        this.stores = List.copyOf(copy);
    }

    /**
     * Deletes collection data from each store, then removes the catalog entry.
     * Returns false when the collection does not exist.
     */
    public synchronized boolean deleteCollection(String collectionId) {
        if (collectionId == null || collectionId.isBlank())
            throw new IllegalArgumentException("collectionId must not be blank");
        if (catalog.find(collectionId).isEmpty()) return false;

        var completed = new ArrayList<String>();
        for (CollectionDataStore store : stores) {
            try {
                store.deleteCollection(collectionId);
                completed.add(store.storeId());
            } catch (RuntimeException failure) {
                throw new CollectionCleanupException(collectionId, store.storeId(), completed, failure);
            }
        }
        return catalog.delete(collectionId);
    }
}
