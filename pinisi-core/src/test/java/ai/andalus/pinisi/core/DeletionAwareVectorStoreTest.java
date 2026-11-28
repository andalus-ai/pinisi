package ai.andalus.pinisi.core;

import ai.andalus.pinisi.api.*;
import ai.andalus.pinisi.spi.CollectionDeletionOperation;
import ai.andalus.pinisi.spi.CollectionDeletionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class DeletionAwareVectorStoreTest {
    @TempDir Path temp;

    private static final EmbeddingSpace SPACE =
            new EmbeddingSpace("shared-v1", "provider", "multimodal", "1", 3, "cosine");

    @Test void blocksReadsAndWritesWhileDeletionIsPendingButAllowsLifecycleCleanup() {
        var backingCatalog = new DefaultCollectionCatalog(new InMemoryCollectionStore());
        var descriptor = backingCatalog.create(definition("images"));
        var journal = new FileCollectionDeletionJournal(temp.resolve("journal"));
        var rawStore = new InMemoryVectorStore();
        rawStore.upsert(descriptor, record("v1"));
        var visibleCatalog = new DeletionAwareCollectionCatalog(backingCatalog, journal);
        var guarded = new DeletionAwareVectorStore(rawStore, visibleCatalog, journal);

        assertEquals(1, guarded.count("images"));
        journal.create(operation("images", CollectionDeletionStatus.PENDING));

        assertThrows(CollectionDeletionInProgressException.class,
                () -> guarded.search(descriptor, vector(), 5));
        assertThrows(CollectionDeletionInProgressException.class,
                () -> guarded.upsert(descriptor, record("v2")));
        assertThrows(CollectionDeletionInProgressException.class,
                () -> guarded.upsertAll(descriptor, List.of(record("v2"))));
        assertThrows(CollectionDeletionInProgressException.class,
                () -> guarded.delete("images", "v1"));
        assertThrows(CollectionDeletionInProgressException.class,
                () -> guarded.count("images"));

        // The coordinator must still be able to clean the underlying collection.
        assertEquals(1, guarded.deleteCollection("images"));
        assertEquals(0, rawStore.count("images"));
    }

    @Test void rejectsMissingCollectionsAndUsesCanonicalDescriptor() {
        var backingCatalog = new DefaultCollectionCatalog(new InMemoryCollectionStore());
        var journal = new FileCollectionDeletionJournal(temp.resolve("journal"));
        var guarded = new DeletionAwareVectorStore(
                new InMemoryVectorStore(), new DeletionAwareCollectionCatalog(backingCatalog, journal), journal);

        assertThrows(CollectionNotFoundException.class,
                () -> guarded.search(descriptor("missing"), vector(), 5));
        assertThrows(CollectionNotFoundException.class, () -> guarded.count("missing"));
    }

    @Test void allowsOperationsAgainAfterDeletionCompletesAndCollectionIsRecreated() {
        var backingCatalog = new DefaultCollectionCatalog(new InMemoryCollectionStore());
        backingCatalog.create(definition("images"));
        var journal = new FileCollectionDeletionJournal(temp.resolve("journal"));
        var rawStore = new InMemoryVectorStore();
        var guarded = new DeletionAwareVectorStore(
                rawStore, new DeletionAwareCollectionCatalog(backingCatalog, journal), journal);
        var pending = operation("images", CollectionDeletionStatus.PENDING);
        journal.create(pending);
        var completed = pending.withProgress(CollectionDeletionStatus.COMPLETED, List.of(), null, null,
                Instant.parse("2026-11-28T00:01:00Z"));
        assertTrue(journal.replace(pending, completed));
        backingCatalog.delete("images");
        var recreated = backingCatalog.create(definition("images"));

        guarded.upsert(recreated, record("new-vector"));
        assertEquals(1, guarded.count("images"));
    }

    private static CollectionDefinition definition(String id) {
        return new CollectionDefinition(id, id, "", SPACE, Set.of(MediaType.IMAGE), Map.of());
    }

    private static CollectionDescriptor descriptor(String id) {
        var now = Instant.parse("2026-11-25T00:00:00Z");
        return new CollectionDescriptor(definition(id), now, now);
    }

    private static CollectionDeletionOperation operation(String id, CollectionDeletionStatus status) {
        var now = Instant.parse("2026-11-28T00:00:00Z");
        return new CollectionDeletionOperation(id, status, List.of(), null, null, null, 0, now, now);
    }

    private static VectorRecord record(String id) {
        var asset = new Asset("asset-" + id, "images", "test-source", id,
                MediaType.IMAGE, URI.create("file:///tmp/" + id), Map.of());
        return new VectorRecord(id, asset, vector(), Map.of());
    }

    private static EmbeddingVector vector() {
        return new EmbeddingVector(SPACE, MediaType.IMAGE, new float[]{1, 0, 0});
    }
}
