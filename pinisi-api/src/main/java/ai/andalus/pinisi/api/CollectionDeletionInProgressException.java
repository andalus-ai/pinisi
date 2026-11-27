package ai.andalus.pinisi.api;

/** Raised when a collection is hidden or protected by an unfinished deletion. */
public final class CollectionDeletionInProgressException extends RuntimeException {
    public CollectionDeletionInProgressException(String collectionId) {
        super("Collection deletion is in progress: " + collectionId);
    }
}
