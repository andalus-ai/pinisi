package ai.andalus.pinisi.api;

import java.util.List;
import java.util.Optional;

/** Public lifecycle API for managing isolated search collections. */
public interface CollectionCatalog {
    CollectionDescriptor create(CollectionDefinition definition);
    Optional<CollectionDescriptor> find(String collectionId);
    List<CollectionDescriptor> list();
    CollectionDescriptor update(CollectionDefinition definition);
    boolean delete(String collectionId);
}
