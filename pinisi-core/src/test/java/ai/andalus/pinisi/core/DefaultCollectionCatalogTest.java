package ai.andalus.pinisi.core;

import ai.andalus.pinisi.api.*;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class DefaultCollectionCatalogTest {
    private static final EmbeddingSpace SPACE_A = new EmbeddingSpace("space-a", "provider", "model-a", "1", 3, "cosine");
    private static final EmbeddingSpace SPACE_B = new EmbeddingSpace("space-b", "provider", "model-b", "1", 3, "cosine");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneOffset.UTC);

    @Test void createsFindsListsAndDeletesCollection() {
        var catalog = new DefaultCollectionCatalog(new InMemoryCollectionStore(), CLOCK);
        var definition = definition("images", "Image collection", SPACE_A);
        var created = catalog.create(definition);
        assertEquals(definition, created.definition());
        assertEquals(CLOCK.instant(), created.createdAt());
        assertEquals(created, catalog.find("images").orElseThrow());
        assertEquals(1, catalog.list().size());
        assertTrue(catalog.delete("images"));
        assertTrue(catalog.find("images").isEmpty());
        assertFalse(catalog.delete("images"));
    }

    @Test void rejectsDuplicateCollectionIds() {
        var catalog = new DefaultCollectionCatalog(new InMemoryCollectionStore(), CLOCK);
        catalog.create(definition("images", "First", SPACE_A));
        assertThrows(CollectionAlreadyExistsException.class,
                () -> catalog.create(definition("images", "Duplicate", SPACE_A)));
    }

    @Test void updatesMetadataButPreservesEmbeddingSpaceAndCreationTime() {
        var catalog = new DefaultCollectionCatalog(new InMemoryCollectionStore(), CLOCK);
        var created = catalog.create(definition("images", "Original", SPACE_A));
        var updated = catalog.update(new CollectionDefinition("images", "Renamed", "Updated description",
                SPACE_A, Set.of(MediaType.IMAGE, MediaType.TEXT), Map.of("owner", "test")));
        assertEquals(created.createdAt(), updated.createdAt());
        assertEquals("Renamed", updated.definition().name());
        assertEquals(created.createdAt(), updated.updatedAt());
    }

    @Test void requiresExplicitReconfigurationToChangeEmbeddingSpace() {
        var catalog = new DefaultCollectionCatalog(new InMemoryCollectionStore(), CLOCK);
        catalog.create(definition("images", "Original", SPACE_A));
        assertThrows(CollectionReconfigurationRequiredException.class,
                () -> catalog.update(definition("images", "Changed model", SPACE_B)));
    }

    @Test void rejectsUpdateOfUnknownCollection() {
        var catalog = new DefaultCollectionCatalog(new InMemoryCollectionStore(), CLOCK);
        assertThrows(CollectionNotFoundException.class,
                () -> catalog.update(definition("missing", "Missing", SPACE_A)));
    }

    @Test void collectionsRemainIsolatedByIdAndEmbeddingSpace() {
        var catalog = new DefaultCollectionCatalog(new InMemoryCollectionStore(), CLOCK);
        catalog.create(definition("images-a", "A", SPACE_A));
        catalog.create(definition("images-b", "B", SPACE_B));
        assertEquals(SPACE_A, catalog.find("images-a").orElseThrow().definition().embeddingSpace());
        assertEquals(SPACE_B, catalog.find("images-b").orElseThrow().definition().embeddingSpace());
        assertEquals(2, catalog.list().size());
    }

    private static CollectionDefinition definition(String id, String name, EmbeddingSpace space) {
        return new CollectionDefinition(id, name, "test collection", space, Set.of(MediaType.IMAGE), Map.of());
    }
}
