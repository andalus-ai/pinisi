package ai.andalus.pinisi.core;
import ai.andalus.pinisi.api.Asset;
import ai.andalus.pinisi.api.MediaType;
import ai.andalus.pinisi.spi.ImageEmbeddingProvider;
import ai.andalus.pinisi.spi.MediaSource;
import ai.andalus.pinisi.spi.VectorIndex;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
/** Synchronous, bounded-memory ingestion for a source of image assets. */
public final class LocalImageIndexer {
    private final ImageEmbeddingProvider embeddings;
    private final VectorIndex index;
    public LocalImageIndexer(ImageEmbeddingProvider embeddings, VectorIndex index) {
        this.embeddings = Objects.requireNonNull(embeddings);
        this.index = Objects.requireNonNull(index);
    }
    public int ingest(MediaSource source) throws IOException {
        Objects.requireNonNull(source);
        int indexed = 0;
        for (Asset asset : source.discover()) {
            if (asset.mediaType() != MediaType.IMAGE) continue;
            try (InputStream input = source.open(asset)) {
                index.upsert(asset, embeddings.embedImage(asset, input));
            }
            indexed++;
        }
        return indexed;
    }
}