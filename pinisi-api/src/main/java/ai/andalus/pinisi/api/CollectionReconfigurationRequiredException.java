package ai.andalus.pinisi.api;

public final class CollectionReconfigurationRequiredException extends RuntimeException {
    public CollectionReconfigurationRequiredException(String collectionId) {
        super("Changing the embedding space of collection '" + collectionId
                + "' requires an explicit reindex/reconfiguration operation");
    }
}
