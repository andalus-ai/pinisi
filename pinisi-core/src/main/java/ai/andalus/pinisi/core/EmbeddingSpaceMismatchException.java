package ai.andalus.pinisi.core;

/** Raised when a vector cannot be compared with its collection's configured space. */
public final class EmbeddingSpaceMismatchException extends IllegalArgumentException {
    public EmbeddingSpaceMismatchException(String message) {
        super(message);
    }
}
