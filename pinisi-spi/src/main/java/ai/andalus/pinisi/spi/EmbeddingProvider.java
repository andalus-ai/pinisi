package ai.andalus.pinisi.spi;

import ai.andalus.pinisi.api.EmbeddingVector;
import ai.andalus.pinisi.api.MediaType;

public interface EmbeddingProvider {
    String id();
    boolean supports(MediaType inputType, MediaType targetType);
    EmbeddingVector embedText(String text, MediaType targetType);
}
