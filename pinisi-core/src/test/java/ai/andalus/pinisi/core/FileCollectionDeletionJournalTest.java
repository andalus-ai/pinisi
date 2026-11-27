package ai.andalus.pinisi.core;

import ai.andalus.pinisi.spi.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FileCollectionDeletionJournalTest {
    @TempDir Path temp;

    @Test void persistsOperationAndProgressAcrossInstances() {
        Instant now = Instant.parse("2026-11-27T00:00:00Z");
        var first = new FileCollectionDeletionJournal(temp);
        var pending = new CollectionDeletionOperation("tenant/images", CollectionDeletionStatus.PENDING,
                List.of(), null, null, null, 0, now, now);
        assertTrue(first.create(pending));

        var inProgress = pending.withProgress(CollectionDeletionStatus.IN_PROGRESS,
                List.of("vectors"), null, null, now.plusSeconds(1));
        assertTrue(first.replace(pending, inProgress));

        var reopened = new FileCollectionDeletionJournal(temp);
        var loaded = reopened.find("tenant/images").orElseThrow();
        assertEquals(1, loaded.revision());
        assertEquals(List.of("vectors"), loaded.completedStoreIds());
        assertEquals(CollectionDeletionStatus.IN_PROGRESS, loaded.status());
        assertEquals(1, reopened.listIncomplete().size());
        assertFalse(reopened.create(pending));
    }

    @Test void compareAndSetRejectsStaleRevision() {
        Instant now = Instant.now();
        var journal = new FileCollectionDeletionJournal(temp);
        var pending = new CollectionDeletionOperation("../unsafe/id", CollectionDeletionStatus.PENDING,
                List.of(), null, null, null, 0, now, now);
        assertTrue(journal.create(pending));
        var next = pending.withProgress(CollectionDeletionStatus.FAILED, List.of(),
                "assets", new IllegalStateException("unavailable"), now.plusSeconds(1));
        assertTrue(journal.replace(pending, next));
        assertFalse(journal.replace(pending, next));
        assertEquals(CollectionDeletionStatus.FAILED,
                new FileCollectionDeletionJournal(temp).find("../unsafe/id").orElseThrow().status());
    }
}
