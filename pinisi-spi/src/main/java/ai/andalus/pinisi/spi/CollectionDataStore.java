package ai.andalus.pinisi.spi;

/**
 * A collection-owned persistence resource that participates in collection deletion.
 *
 * <p>Deletion must be idempotent: retries after a partial multi-store cleanup
 * must be safe. Implementations should make deletion of one collection atomic
 * within that store and must not affect other collection namespaces.</p>
 */
public interface CollectionDataStore {
    /** Stable, non-blank identifier used in lifecycle diagnostics. */
    String storeId();

    /** Deletes all data owned by the collection and returns the number of removed records. */
    long deleteCollection(String collectionId);
}
