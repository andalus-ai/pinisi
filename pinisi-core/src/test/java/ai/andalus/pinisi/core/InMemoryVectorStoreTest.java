package ai.andalus.pinisi.core;

import ai.andalus.pinisi.api.*;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryVectorStoreTest {
    private static final EmbeddingSpace SPACE =
            new EmbeddingSpace("shared-v1", "provider", "multimodal", "1", 3, "cosine");

    @Test void isolatesRecordsByCollectionAndRanksCosineSimilarity() {
        var store = new InMemoryVectorStore();
        var first = collection("images-a", SPACE);
        var second = collection("images-b", SPACE);
        store.upsert(first, record("v1", "images-a", MediaType.IMAGE, 1, 0, 0));
        store.upsert(second, record("v1", "images-b", MediaType.IMAGE, 0, 1, 0));

        var matches = store.search(first, vector(MediaType.TEXT, 1, 0, 0), 10);
        assertEquals(1, matches.size());
        assertEquals("v1", matches.getFirst().record().id());
        assertEquals(1.0, matches.getFirst().score(), 1e-9);
        assertEquals(1, store.count("images-a"));
        assertEquals(1, store.count("images-b"));
    }

    @Test void rejectsMismatchedSpaceOnWriteAndQuery() {
        var store = new InMemoryVectorStore();
        var collection = collection("images", SPACE);
        var other = new EmbeddingSpace("other", "provider", "other-model", "1", 3, "cosine");
        assertThrows(EmbeddingSpaceMismatchException.class, () ->
                store.upsert(collection, new VectorRecord("v1",
                        asset("images", "a1", MediaType.IMAGE),
                        new EmbeddingVector(other, MediaType.IMAGE, new float[]{1, 0, 0}), Map.of())));
        assertThrows(EmbeddingSpaceMismatchException.class, () ->
                store.search(collection, new EmbeddingVector(other, MediaType.TEXT, new float[]{1, 0, 0}), 5));
        assertEquals(0, store.count("images"));
    }

    @Test void validatesEntireBatchBeforeWriting() {
        var store = new InMemoryVectorStore();
        var collection = collection("images", SPACE);
        var valid = record("valid", "images", MediaType.IMAGE, 1, 0, 0);
        var other = new EmbeddingSpace("other", "provider", "other-model", "1", 3, "cosine");
        var invalid = new VectorRecord("invalid", asset("images", "a2", MediaType.IMAGE),
                new EmbeddingVector(other, MediaType.IMAGE, new float[]{0, 1, 0}), Map.of());

        assertThrows(EmbeddingSpaceMismatchException.class, () -> store.upsertAll(collection, List.of(valid, invalid)));
        assertEquals(0, store.count("images"));
        assertThrows(IllegalArgumentException.class, () ->
                store.upsertAll(collection, List.of(valid, valid)));
        assertEquals(0, store.count("images"));
    }

    @Test void rejectsWrongCollectionAndUnconfiguredModality() {
        var store = new InMemoryVectorStore();
        var collection = collection("images", SPACE);
        assertThrows(IllegalArgumentException.class, () ->
                store.upsert(collection, record("v1", "other", MediaType.IMAGE, 1, 0, 0)));
        assertThrows(IllegalArgumentException.class, () ->
                store.upsert(collection, record("v2", "images", MediaType.AUDIO, 1, 0, 0)));
        assertEquals(0, store.count("images"));
    }

    @Test void supportsScopedDeletionAndCollectionCleanup() {
        var store = new InMemoryVectorStore();
        var first = collection("a", SPACE);
        var second = collection("b", SPACE);
        store.upsert(first, record("same-id", "a", MediaType.IMAGE, 1, 0, 0));
        store.upsert(second, record("same-id", "b", MediaType.IMAGE, 0, 1, 0));

        assertTrue(store.delete("a", "same-id"));
        assertFalse(store.delete("a", "same-id"));
        assertEquals(1, store.count("b"));
        assertEquals(1, store.deleteCollection("b"));
        assertEquals(0, store.count("b"));
    }

    @Test void sortsEqualScoresDeterministicallyAndValidatesLimit() {
        var store = new InMemoryVectorStore();
        var collection = collection("images", SPACE);
        store.upsertAll(collection, List.of(
                record("z", "images", MediaType.IMAGE, 1, 0, 0),
                record("a", "images", MediaType.IMAGE, 1, 0, 0)));
        assertEquals("a", store.search(collection, vector(MediaType.TEXT, 1, 0, 0), 2)
                .getFirst().record().id());
        assertThrows(IllegalArgumentException.class,
                () -> store.search(collection, vector(MediaType.TEXT, 1, 0, 0), -1));
    }

    private static CollectionDescriptor collection(String id, EmbeddingSpace space) {
        var definition = new CollectionDefinition(id, id, "", space, Set.of(MediaType.IMAGE), Map.of());
        var now = Instant.parse("2026-11-25T00:00:00Z");
        return new CollectionDescriptor(definition, now, now);
    }

    private static VectorRecord record(String id, String collection, MediaType modality,
                                       float x, float y, float z) {
        return new VectorRecord(id, asset(collection, "asset-" + id, modality),
                new EmbeddingVector(SPACE, modality, new float[]{x, y, z}), Map.of());
    }

    private static EmbeddingVector vector(MediaType modality, float x, float y, float z) {
        return new EmbeddingVector(SPACE, modality, new float[]{x, y, z});
    }

    private static Asset asset(String collection, String id, MediaType modality) {
        return new Asset(id, collection, "test-source", id,
                modality, URI.create("file:///tmp/" + id), Map.of());
    }
}
