package ai.andalus.pinisi.core;

import ai.andalus.pinisi.api.*;
import ai.andalus.pinisi.spi.CollectionDataStore;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class DefaultCollectionLifecycleCoordinatorTest {
    private static final EmbeddingSpace SPACE =
            new EmbeddingSpace("space", "provider", "model", "1", 3, "cosine");

    @Test void removesStoreDataBeforeCatalogEntry() {
        var catalog = catalogWith("images");
        var vectors = new InMemoryVectorStore();
        vectors.upsert(catalog.find("images").orElseThrow(), record("images", "v1"));
        var deleted = new ArrayList<String>();
        CollectionDataStore assets = store("assets", deleted, false);
        var coordinator = new DefaultCollectionLifecycleCoordinator(catalog, List.of(vectors, assets));

        assertTrue(coordinator.deleteCollection("images"));
        assertEquals(List.of("assets"), deleted);
        assertTrue(catalog.find("images").isEmpty());
        assertEquals(0, vectors.count("images"));
    }

    @Test void retainsCatalogEntryWhenAStoreFailsAndAllowsRetry() {
        var catalog = catalogWith("images");
        var vectors = new InMemoryVectorStore();
        vectors.upsert(catalog.find("images").orElseThrow(), record("images", "v1"));
        var fail = new AtomicBoolean(true);
        var completed = new ArrayList<String>();
        CollectionDataStore assets = new CollectionDataStore() {
            public String storeId() { return "assets"; }
            public long deleteCollection(String id) {
                if (fail.getAndSet(false)) throw new IllegalStateException("temporary failure");
                completed.add(id);
                return 1;
            }
        };
        var coordinator = new DefaultCollectionLifecycleCoordinator(catalog, List.of(vectors, assets));

        var error = assertThrows(CollectionCleanupException.class,
                () -> coordinator.deleteCollection("images"));
        assertEquals("assets", error.failedStoreId());
        assertEquals(List.of("in-memory-vector-store"), error.completedStoreIds());
        assertTrue(catalog.find("images").isPresent());
        assertEquals(0, vectors.count("images"));

        assertTrue(coordinator.deleteCollection("images"));
        assertEquals(List.of("images"), completed);
        assertTrue(catalog.find("images").isEmpty());
    }

    @Test void returnsFalseForUnknownCollectionAndValidatesId() {
        var coordinator = new DefaultCollectionLifecycleCoordinator(catalogWith("images"), List.of());
        assertFalse(coordinator.deleteCollection("missing"));
        assertThrows(IllegalArgumentException.class, () -> coordinator.deleteCollection(" "));
    }

    @Test void rejectsDuplicateStoreIds() {
        CollectionDataStore first = store("same", new ArrayList<>(), false);
        CollectionDataStore second = store("same", new ArrayList<>(), false);
        assertThrows(IllegalArgumentException.class, () ->
                new DefaultCollectionLifecycleCoordinator(catalogWith("images"), List.of(first, second)));
    }

    private static DefaultCollectionCatalog catalogWith(String id) {
        var catalog = new DefaultCollectionCatalog(new InMemoryCollectionStore());
        catalog.create(new CollectionDefinition(id, id, "", SPACE, Set.of(MediaType.IMAGE), Map.of()));
        return catalog;
    }

    private static VectorRecord record(String collectionId, String id) {
        var asset = new Asset("asset-" + id, collectionId, "test", id, MediaType.IMAGE,
                URI.create("file:///tmp/" + id), Map.of());
        return new VectorRecord(id, asset,
                new EmbeddingVector(SPACE, MediaType.IMAGE, new float[]{1, 0, 0}), Map.of());
    }

    private static CollectionDataStore store(String id, List<String> deleted, boolean fail) {
        return new CollectionDataStore() {
            public String storeId() { return id; }
            public long deleteCollection(String collectionId) {
                deleted.add(collectionId);
                return 1;
            }
        };
    }
}
