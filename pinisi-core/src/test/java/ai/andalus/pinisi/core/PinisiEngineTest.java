package ai.andalus.pinisi.core;

import ai.andalus.pinisi.api.*;
import ai.andalus.pinisi.spi.*;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PinisiEngineTest {
    private static final EmbeddingSpace SPACE = new EmbeddingSpace("fixture-v1","test","fixture","1","text/image",3,"cosine");
    @Test void indexesAndSearchesOnlyCompatibleImageVectors() {
        EmbeddingProvider provider = new EmbeddingProvider() {
            public String id(){ return "fixture"; }
            public boolean supports(MediaType in, MediaType out){ return in == MediaType.TEXT && out == MediaType.IMAGE; }
            public EmbeddingVector embedText(String text, MediaType target){ return new EmbeddingVector(SPACE, new float[]{1,0,0}); }
        };
        var index = new InMemoryVectorIndex();
        var engine = new PinisiEngine(provider,index);
        var image = new Asset("a1","c1","fs","/tmp/cat.jpg",MediaType.IMAGE,URI.create("file:///tmp/cat.jpg"),Map.of());
        engine.indexImage(image,new EmbeddingVector(SPACE,new float[]{1,0,0}));
        var results = engine.searchImages(new SearchRequest("c1","a cat",10,MediaType.IMAGE));
        assertEquals(1,results.size()); assertEquals("a1",results.getFirst().asset().id());
        assertEquals(1.0,results.getFirst().score(),1e-9);
    }
    @Test void refusesToCompareDifferentEmbeddingSpaces() {
        var index = new InMemoryVectorIndex();
        var asset = new Asset("a1","c1","fs","cat.jpg",MediaType.IMAGE,URI.create("file:///cat.jpg"),Map.of());
        index.upsert(asset,new EmbeddingVector(SPACE,new float[]{1,0,0}));
        var other = new EmbeddingSpace("other","test","other","1","text/image",3,"cosine");
        assertTrue(index.search("c1",new EmbeddingVector(other,new float[]{1,0,0}),5).isEmpty());
    }
}
