package ai.andalus.pinisi.spi;

/** Durable lifecycle state for a collection deletion operation. */
public enum CollectionDeletionStatus {
    PENDING, IN_PROGRESS, FAILED, COMPLETED
}
