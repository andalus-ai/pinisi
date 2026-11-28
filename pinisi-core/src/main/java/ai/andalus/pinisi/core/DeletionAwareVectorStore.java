package ai.andalus.pinisi.core;

import ai.andalus.pinisi.api.CollectionCatalog;
import ai.andalus.pinisi.api.CollectionDeletionInProgressException;
import ai.andalus.pinisi.api.CollectionDescriptor;
import ai.andalus.pinisi.api.CollectionNotFoundException;
import ai.andalus.pinisi.api.EmbeddingVector;
import ai.andalus.pinisi.api.VectorMatch;
import ai.andalus.pinisi.api.VectorRecord;
import ai.andalus.pinisi.spi.CollectionDeletionJournal;
import ai.andalus.pinisi.spi.CollectionDeletionStatus;
import ai.andalus.pinisi.spi.VectorStore;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Enforces collection visibility and lifecycle state at the vector access boundary.
 *
 * <p>Resolve collections through a deletion-aware catalog and share the deletion
 * journal used by the lifecycle coordinator. Collection-wide cleanup deliberately
 * bypasses the visibility check so the coordinator can finish deleting data.</p>
 */
public final class DeletionAwareVectorStore implements VectorStore {
    private final VectorStore delegate;
    private final CollectionCatalog catalog;
    private final CollectionDeletionJournal journal;

    public DeletionAwareVectorStore(VectorStore delegate, CollectionCatalog catalog,
                                    CollectionDeletionJournal journal) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.journal = Objects.requireNonNull(journal, "journal");
    }

    @Override public String storeId() {
        return delegate.storeId();
    }

    @Override public void upsert(CollectionDescriptor collection, VectorRecord record) {
        delegate.upsert(requireVisible(collection), record);
    }

    @Override public void upsertAll(CollectionDescriptor collection, Collection<VectorRecord> records) {
        delegate.upsertAll(requireVisible(collection), records);
    }

    @Override public List<VectorMatch> search(CollectionDescriptor collection, EmbeddingVector query, int limit) {
        return delegate.search(requireVisible(collection), query, limit);
    }

    @Override public boolean delete(String collectionId, String vectorId) {
        requireVisible(collectionId);
        return delegate.delete(collectionId, vectorId);
    }

    @Override public long count(String collectionId) {
        requireVisible(collectionId);
        return delegate.count(collectionId);
    }

    /**
     * Lifecycle cleanup must remain callable after a tombstone is written.
     * The coordinator is responsible for invoking this operation.
     */
    @Override public long deleteCollection(String collectionId) {
        return delegate.deleteCollection(collectionId);
    }

    private CollectionDescriptor requireVisible(CollectionDescriptor requested) {
        Objects.requireNonNull(requested, "collection");
        return requireVisible(requested.id());
    }

    private CollectionDescriptor requireVisible(String collectionId) {
        if (collectionId == null || collectionId.isBlank())
            throw new IllegalArgumentException("collectionId must not be blank");
        journal.find(collectionId)
                .filter(operation -> operation.status() != CollectionDeletionStatus.COMPLETED)
                .ifPresent(operation -> { throw new CollectionDeletionInProgressException(collectionId); });
        return catalog.find(collectionId).orElseThrow(() -> new CollectionNotFoundException(collectionId));
    }
}
