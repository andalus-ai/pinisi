package ai.andalus.pinisi.core;

import ai.andalus.pinisi.api.*;
import ai.andalus.pinisi.spi.CollectionDeletionJournal;
import ai.andalus.pinisi.spi.CollectionDeletionOperation;
import ai.andalus.pinisi.spi.CollectionDeletionStatus;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Collection catalog view that hides collections while their durable deletion
 * operation is incomplete. Use this view at application/search read boundaries.
 *
 * <p>The lifecycle coordinator may use this decorator too: mutations are
 * delegated to the underlying catalog after the coordinator has established
 * the journal tombstone.</p>
 */
public final class DeletionAwareCollectionCatalog implements CollectionCatalog {
    private final CollectionCatalog delegate;
    private final CollectionDeletionJournal journal;

    public DeletionAwareCollectionCatalog(CollectionCatalog delegate, CollectionDeletionJournal journal) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.journal = Objects.requireNonNull(journal, "journal");
    }

    @Override public CollectionDescriptor create(CollectionDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        rejectIncompleteDeletion(definition.id());
        return delegate.create(definition);
    }

    @Override public Optional<CollectionDescriptor> find(String collectionId) {
        requireId(collectionId);
        if (hasIncompleteDeletion(collectionId)) return Optional.empty();
        return delegate.find(collectionId);
    }

    @Override public List<CollectionDescriptor> list() {
        return delegate.list().stream()
                .filter(descriptor -> !hasIncompleteDeletion(descriptor.id()))
                .toList();
    }

    @Override public CollectionDescriptor update(CollectionDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        rejectIncompleteDeletion(definition.id());
        return delegate.update(definition);
    }

    /** Low-level metadata deletion; application workflows should use the lifecycle coordinator. */
    @Override public boolean delete(String collectionId) {
        requireId(collectionId);
        return delegate.delete(collectionId);
    }

    private boolean hasIncompleteDeletion(String collectionId) {
        return journal.find(collectionId)
                .map(operation -> operation.status() != CollectionDeletionStatus.COMPLETED)
                .orElse(false);
    }

    private void rejectIncompleteDeletion(String collectionId) {
        if (hasIncompleteDeletion(collectionId))
            throw new CollectionDeletionInProgressException(collectionId);
    }

    private static void requireId(String collectionId) {
        if (collectionId == null || collectionId.isBlank())
            throw new IllegalArgumentException("collectionId must not be blank");
    }
}
