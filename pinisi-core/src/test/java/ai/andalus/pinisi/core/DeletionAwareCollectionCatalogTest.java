package ai.andalus.pinisi.core;

import ai.andalus.pinisi.api.*;
import ai.andalus.pinisi.spi.CollectionDataStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class DeletionAwareCollectionCatalogTest {
    @TempDir Path temp;

    private static final EmbeddingSpace SPACE =
            new EmbeddingSpace("space", "provider", "model", "1", 3, "cosine");

    @Test void hidesCollectionImmediatelyAndKeepsItHiddenUntilCleanupCompletes() {
        var backing = new DefaultCollectionCatalog(new InMemoryCollectionStore());
        backing.create(definition("images"));
        var journal = new FileCollectionDeletionJournal(temp.resolve("journal"));
        var visible = new DeletionAwareCollectionCatalog(backing, journal);
        var failOnce = new AtomicBoolean(true);
        CollectionDataStore assets = new CollectionDataStore() {
            public String storeId() { return "assets"; }
            public long deleteCollection(String id) {
                if (failOnce.getAndSet(false)) throw new IllegalStateException("temporary failure");
                return 1;
            }
        };
        var coordinator = new DefaultCollectionLifecycleCoordinator(visible, List.of(assets), journal);

        assertThrows(CollectionCleanupException.class, () -> coordinator.deleteCollection("images"));
        assertTrue(visible.find("images").isEmpty());
        assertTrue(visible.list().isEmpty());
        assertThrows(CollectionDeletionInProgressException.class,
                () -> visible.create(definition("images")));
        assertThrows(CollectionDeletionInProgressException.class,
                () -> visible.update(definition("images")));

        assertEquals(1, coordinator.resumePendingDeletions());
        assertTrue(visible.find("images").isEmpty());
        assertTrue(visible.list().isEmpty());

        // A completed tombstone no longer blocks intentional ID reuse.
        assertEquals("images", visible.create(definition("images")).id());
        assertTrue(visible.find("images").isPresent());
    }

    @Test void doesNotHideUnrelatedCollections() {
        var backing = new DefaultCollectionCatalog(new InMemoryCollectionStore());
        backing.create(definition("images-a"));
        backing.create(definition("images-b"));
        var journal = new FileCollectionDeletionJournal(temp.resolve("journal"));
        var visible = new DeletionAwareCollectionCatalog(backing, journal);
        assertTrue(visible.find("images-a").isPresent());
        assertEquals(2, visible.list().size());
    }

    private static CollectionDefinition definition(String id) {
        return new CollectionDefinition(id, id, "", SPACE, Set.of(MediaType.IMAGE), Map.of());
    }
}
