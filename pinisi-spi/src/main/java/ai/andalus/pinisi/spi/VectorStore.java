package ai.andalus.pinisi.spi;

import ai.andalus.pinisi.api.CollectionDescriptor;
import ai.andalus.pinisi.api.EmbeddingVector;
import ai.andalus.pinisi.api.VectorMatch;
import ai.andalus.pinisi.api.VectorRecord;
import java.util.Collection;
import java.util.List;

/**
 * Collection-scoped vector persistence and similarity search.
 *
 * <p>The collection descriptor is supplied on each operation so implementations
 * can validate the collection's canonical embedding space without depending on
 * a catalog implementation.</p>
 */
public interface VectorStore {
    void upsert(CollectionDescriptor collection, VectorRecord record);

    /** Implementations must validate the complete batch before making it visible. */
    void upsertAll(CollectionDescriptor collection, Collection<VectorRecord> records);

    List<VectorMatch> search(CollectionDescriptor collection, EmbeddingVector query, int limit);

    boolean delete(String collectionId, String vectorId);

    long count(String collectionId);

    /** Removes all vectors in the named collection; unrelated collections are untouched. */
    long deleteCollection(String collectionId);
}
