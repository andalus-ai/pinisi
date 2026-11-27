package ai.andalus.pinisi.spi;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Immutable, revisioned checkpoint for one collection deletion. */
public record CollectionDeletionOperation(
        String collectionId,
        CollectionDeletionStatus status,
        List<String> completedStoreIds,
        String failedComponentId,
        String failureType,
        String failureMessage,
        long revision,
        Instant createdAt,
        Instant updatedAt) {
    public CollectionDeletionOperation {
        if (collectionId == null || collectionId.isBlank()) throw new IllegalArgumentException("collectionId must not be blank");
        Objects.requireNonNull(status, "status");
        completedStoreIds = List.copyOf(Objects.requireNonNull(completedStoreIds, "completedStoreIds"));
        if (revision < 0) throw new IllegalArgumentException("revision must not be negative");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public CollectionDeletionOperation withProgress(CollectionDeletionStatus nextStatus,
            List<String> completed, String failedId, Throwable failure, Instant now) {
        String type = failure == null ? null : failure.getClass().getName();
        String message = failure == null ? null : failure.getMessage();
        if (message != null && message.length() > 512) message = message.substring(0, 512);
        return new CollectionDeletionOperation(collectionId, nextStatus, completed, failedId,
                type, message, revision + 1, createdAt, now);
    }
}
