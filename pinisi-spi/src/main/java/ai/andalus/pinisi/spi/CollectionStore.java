package ai.andalus.pinisi.spi;

import ai.andalus.pinisi.api.CollectionDescriptor;
import java.util.List;
import java.util.Optional;

/** Persistence SPI for collection definitions. Implementations must make create and replace atomic. */
public interface CollectionStore {
    boolean create(CollectionDescriptor descriptor);
    Optional<CollectionDescriptor> find(String collectionId);
    List<CollectionDescriptor> list();
    boolean replace(CollectionDescriptor expected, CollectionDescriptor replacement);
    boolean delete(String collectionId);
}
