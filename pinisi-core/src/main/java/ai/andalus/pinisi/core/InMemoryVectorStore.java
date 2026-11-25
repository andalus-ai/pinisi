package ai.andalus.pinisi.core;

import ai.andalus.pinisi.api.CollectionDescriptor;
import ai.andalus.pinisi.api.EmbeddingVector;
import ai.andalus.pinisi.api.MediaType;
import ai.andalus.pinisi.api.VectorMatch;
import ai.andalus.pinisi.api.VectorRecord;
import ai.andalus.pinisi.spi.VectorStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Thread-safe, process-local vector store intended for embedded use and tests. */
public final class InMemoryVectorStore implements VectorStore {
    private final ConcurrentMap<Key, VectorRecord> records = new ConcurrentHashMap<>();

    @Override
    public void upsert(CollectionDescriptor collection, VectorRecord record) {
        upsertAll(collection, List.of(record));
    }

    @Override
    public synchronized void upsertAll(CollectionDescriptor collection, Collection<VectorRecord> batch) {
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(batch, "records");
        var validated = new ArrayList<VectorRecord>(batch.size());
        var ids = new java.util.HashSet<String>();
        for (VectorRecord record : batch) {
            validate(collection, record);
            if (!ids.add(record.id())) {
                throw new IllegalArgumentException("Duplicate vector id in batch: " + record.id());
            }
            validated.add(record);
        }
        for (VectorRecord record : validated) {
            records.put(new Key(collection.id(), record.id()), record);
        }
    }

    @Override
    public synchronized List<VectorMatch> search(CollectionDescriptor collection, EmbeddingVector query, int limit) {
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(query, "query");
        if (limit < 0) throw new IllegalArgumentException("limit must not be negative");
        validateSpace(collection, query);
        if (limit == 0) return List.of();
        return records.entrySet().stream()
                .filter(entry -> entry.getKey().collectionId.equals(collection.id()))
                .map(entry -> entry.getValue())
                .filter(record -> record.embedding().space().equals(collection.definition().embeddingSpace()))
                .map(record -> new VectorMatch(record, similarity(query, record.embedding(),
                        collection.definition().embeddingSpace().metric())))
                .sorted(Comparator.comparingDouble(VectorMatch::score).reversed()
                        .thenComparing(match -> match.record().id()))
                .limit(limit)
                .toList();
    }

    @Override
    public synchronized boolean delete(String collectionId, String vectorId) {
        return records.remove(new Key(requireText(collectionId, "collectionId"),
                requireText(vectorId, "vectorId"))) != null;
    }

    @Override
    public synchronized long count(String collectionId) {
        String id = requireText(collectionId, "collectionId");
        return records.keySet().stream().filter(key -> key.collectionId.equals(id)).count();
    }

    @Override
    public synchronized long deleteCollection(String collectionId) {
        String id = requireText(collectionId, "collectionId");
        long before = count(id);
        records.keySet().removeIf(key -> key.collectionId.equals(id));
        return before;
    }

    private static void validate(CollectionDescriptor collection, VectorRecord record) {
        Objects.requireNonNull(record, "record");
        if (!collection.id().equals(record.collectionId())) {
            throw new IllegalArgumentException("Vector record belongs to a different collection");
        }
        if (!collection.definition().indexedModalities().contains(record.embedding().modality())) {
            throw new IllegalArgumentException("Collection does not index modality: " + record.embedding().modality());
        }
        validateSpace(collection, record.embedding());
    }

    private static void validateSpace(CollectionDescriptor collection, EmbeddingVector vector) {
        if (!collection.definition().embeddingSpace().equals(vector.space())) {
            throw new EmbeddingSpaceMismatchException("Vector embedding space does not match collection "
                    + collection.id());
        }
    }

    private static double similarity(EmbeddingVector query, EmbeddingVector candidate, String metric) {
        if (!"cosine".equalsIgnoreCase(metric)) {
            throw new IllegalArgumentException("Unsupported in-memory similarity metric: " + metric);
        }
        float[] left = query.values();
        float[] right = candidate.values();
        double dot = 0, leftNorm = 0, rightNorm = 0;
        for (int i = 0; i < left.length; i++) {
            dot += (double) left[i] * right[i];
            leftNorm += (double) left[i] * left[i];
            rightNorm += (double) right[i] * right[i];
        }
        return leftNorm == 0 || rightNorm == 0
                ? 0 : dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }

    private record Key(String collectionId, String vectorId) {}
}
