package ai.andalus.pinisi.spi;
import ai.andalus.pinisi.api.Asset;
import ai.andalus.pinisi.api.EmbeddingVector;
import java.io.IOException;
import java.io.InputStream;
/** Image-input embedding capability. Implementations consume the stream during this call. */
public interface ImageEmbeddingProvider extends EmbeddingProvider {
    EmbeddingVector embedImage(Asset asset, InputStream image) throws IOException;
}