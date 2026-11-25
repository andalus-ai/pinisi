package ai.andalus.pinisi.api;

public final class CollectionAlreadyExistsException extends RuntimeException {
    public CollectionAlreadyExistsException(String collectionId) {
        super("Collection already exists: " + collectionId);
    }
}
