package ai.andalus.pinisi.core;

import ai.andalus.pinisi.api.*;
import ai.andalus.pinisi.spi.VectorIndex;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryVectorIndex implements VectorIndex {
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    @Override public void upsert(Asset asset, EmbeddingVector vector) {
        Objects.requireNonNull(asset); Objects.requireNonNull(vector);
        if (asset.mediaType() != MediaType.IMAGE) throw new IllegalArgumentException("This initial index accepts IMAGE assets only");
        entries.put(asset.id(), new Entry(asset, vector));
    }
    @Override public List<Match> search(String collectionId, EmbeddingVector query, int limit) {
        return entries.values().stream().filter(e -> e.asset.collectionId().equals(collectionId))
            .filter(e -> e.vector.space().equals(query.space()))
            .map(e -> new Match(e.asset, cosine(query.values(), e.vector.values())))
            .sorted(Comparator.comparingDouble(Match::score).reversed()).limit(limit).toList();
    }
    @Override public void delete(String assetId) { entries.remove(assetId); }
    private static double cosine(float[] a, float[] b) {
        double dot=0, aa=0, bb=0;
        for(int i=0;i<a.length;i++){ dot+=(double)a[i]*b[i]; aa+=(double)a[i]*a[i]; bb+=(double)b[i]*b[i]; }
        return aa == 0 || bb == 0 ? 0 : dot/(Math.sqrt(aa)*Math.sqrt(bb));
    }
    private record Entry(Asset asset, EmbeddingVector vector) {}
}
