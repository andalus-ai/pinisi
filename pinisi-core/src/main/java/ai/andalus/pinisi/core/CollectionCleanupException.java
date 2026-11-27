package ai.andalus.pinisi.core;

import java.util.List;

/** Indicates which collection store failed during a retryable multi-store cleanup. */
public final class CollectionCleanupException extends RuntimeException {
    private final String collectionId;
    private final String failedStoreId;
    private final List<String> completedStoreIds;

    public CollectionCleanupException(String collectionId, String failedStoreId,
                                      List<String> completedStoreIds, Throwable cause) {
        super("Failed to clean collection " + collectionId + " in store " + failedStoreId
                + "; completed stores: " + completedStoreIds, cause);
        this.collectionId = collectionId;
        this.failedStoreId = failedStoreId;
        this.completedStoreIds = List.copyOf(completedStoreIds);
    }

    public String collectionId() { return collectionId; }
    public String failedStoreId() { return failedStoreId; }
    public List<String> completedStoreIds() { return completedStoreIds; }
}
