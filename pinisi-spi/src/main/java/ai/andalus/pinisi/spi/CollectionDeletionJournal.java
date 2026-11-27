package ai.andalus.pinisi.spi;

import java.util.List;
import java.util.Optional;

/**
 * Durable journal for resumable collection deletion.
 * replace is an optimistic compare-and-set on the operation revision.
 */
public interface CollectionDeletionJournal {
    boolean create(CollectionDeletionOperation operation);
    Optional<CollectionDeletionOperation> find(String collectionId);
    List<CollectionDeletionOperation> listIncomplete();
    boolean replace(CollectionDeletionOperation expected, CollectionDeletionOperation replacement);
}
