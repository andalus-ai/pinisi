package ai.andalus.pinisi.core;

import ai.andalus.pinisi.api.CollectionCatalog;
import ai.andalus.pinisi.spi.CollectionDataStore;
import ai.andalus.pinisi.spi.CollectionDeletionJournal;
import ai.andalus.pinisi.spi.CollectionDeletionOperation;
import ai.andalus.pinisi.spi.CollectionDeletionStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Coordinates restartable collection deletion across collection-owned stores.
 *
 * <p>Progress is journaled after each idempotent store deletion. Recovery is
 * at-least-once: a crash between deleting data and checkpointing may repeat that
 * store operation, so every participant must make deletion idempotent.</p>
 */
public final class DefaultCollectionLifecycleCoordinator {
    private final CollectionCatalog catalog;
    private final List<CollectionDataStore> stores;
    private final CollectionDeletionJournal journal;

    public DefaultCollectionLifecycleCoordinator(CollectionCatalog catalog,
            Collection<? extends CollectionDataStore> stores, CollectionDeletionJournal journal) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.journal = Objects.requireNonNull(journal, "journal");
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

    /** Starts or resumes deletion. Returns false only when neither catalog nor journal has the collection. */
    public synchronized boolean deleteCollection(String collectionId) {
        requireId(collectionId);
        var existing = journal.find(collectionId);
        boolean catalogPresent = catalog.find(collectionId).isPresent();
        if (existing.isEmpty() && !catalogPresent) return false;

        CollectionDeletionOperation operation;
        if (existing.isEmpty()) {
            operation = newOperation(collectionId, 0);
            if (!journal.create(operation))
                operation = journal.find(collectionId).orElseThrow(
                        () -> new IllegalStateException("Deletion journal create race for " + collectionId));
        } else {
            operation = existing.orElseThrow();
            if (operation.status() == CollectionDeletionStatus.COMPLETED) {
                if (!catalogPresent) return true;
                CollectionDeletionOperation next = newOperation(collectionId, operation.revision() + 1);
                if (!journal.replace(operation, next))
                    throw new IllegalStateException("Concurrent collection deletion update for " + collectionId);
                operation = next;
            }
        }
        execute(operation);
        return true;
    }

    /** Resumes every non-completed deletion found in the durable journal. */
    public synchronized int resumePendingDeletions() {
        int resumed = 0;
        for (CollectionDeletionOperation operation : journal.listIncomplete()) {
            execute(operation);
            resumed++;
        }
        return resumed;
    }

    private void execute(CollectionDeletionOperation operation) {
        String collectionId = operation.collectionId();
        var completed = new ArrayList<>(operation.completedStoreIds());
        operation = update(operation, CollectionDeletionStatus.IN_PROGRESS, completed, null, null);

        for (CollectionDataStore store : stores) {
            if (completed.contains(store.storeId())) continue;
            try {
                store.deleteCollection(collectionId);
                completed.add(store.storeId());
                operation = update(operation, CollectionDeletionStatus.IN_PROGRESS, completed, null, null);
            } catch (RuntimeException failure) {
                update(operation, CollectionDeletionStatus.FAILED, completed, store.storeId(), failure);
                throw new CollectionCleanupException(collectionId, store.storeId(), completed, failure);
            }
        }

        try {
            catalog.delete(collectionId);
        } catch (RuntimeException failure) {
            update(operation, CollectionDeletionStatus.FAILED, completed, "catalog", failure);
            throw new CollectionCleanupException(collectionId, "catalog", completed, failure);
        }
        update(operation, CollectionDeletionStatus.COMPLETED, completed, null, null);
    }

    private CollectionDeletionOperation update(CollectionDeletionOperation current,
            CollectionDeletionStatus status, List<String> completed, String failedId, Throwable failure) {
        CollectionDeletionOperation replacement = current.withProgress(status, completed, failedId, failure, Instant.now());
        if (!journal.replace(current, replacement))
            throw new IllegalStateException("Concurrent collection deletion update for " + current.collectionId());
        return replacement;
    }

    private static CollectionDeletionOperation newOperation(String id, long revision) {
        Instant now = Instant.now();
        return new CollectionDeletionOperation(id, CollectionDeletionStatus.PENDING, List.of(),
                null, null, null, revision, now, now);
    }

    private static void requireId(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("collectionId must not be blank");
    }
}
