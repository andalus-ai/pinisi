package ai.andalus.pinisi.provider.aljabr;

import ai.andalus.pinisi.api.*;
import org.junit.jupiter.api.Test;
import tech.kayys.aljabr.spi.embedding.EmbeddingModelRunner;
import tech.kayys.aljabr.spi.embedding.EmbeddingRequest;
import tech.kayys.aljabr.spi.embedding.EmbeddingResponse;
import tech.kayys.aljabr.spi.embedding.EmbeddingTask;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class AljabrEmbeddingProviderTest {
    @Test
    void mapsTextBatchAndPreservesPinisiInputIds() {
        var runner = new StubRunner(request -> new EmbeddingResponse(
                request.requestId(), request.model(),
                request.inputs().stream().map(text -> new float[]{text.length(), 1, 2}).toList(),
                3, Map.of()));
        var provider = provider(runner);

        var response = provider.embed(new ai.andalus.pinisi.api.EmbeddingRequest(List.of(
                new EmbeddingInput("doc-1", new TextContent("hello")),
                new EmbeddingInput("doc-2", new TextContent("world!")))));

        assertEquals(List.of("doc-1", "doc-2"),
                response.results().stream().map(EmbeddingResult::inputId).toList());
        assertEquals(6f, response.results().get(1).embedding().values()[0]);
        assertEquals("aljabr", response.results().getFirst().embedding().space().provider());
    }

    @Test
    void mapsRetrievalTaskAndTitleParameters() {
        var runner = new StubRunner(request -> {
            assertEquals(EmbeddingTask.RETRIEVAL_QUERY, request.task());
            assertEquals("A title", request.parameters().get("title"));
            return response(request);
        });
        provider(runner).embed(new ai.andalus.pinisi.api.EmbeddingRequest(
                List.of(new EmbeddingInput("query", new TextContent("question"))),
                null, Map.of("task", "retrieval_query", "title", "A title")));
    }

    @Test
    void rejectsNonTextContentAndMismatchedSpace() {
        var runner = new StubRunner(AljabrEmbeddingProviderTest::response);
        var provider = provider(runner);
        assertThrows(IllegalArgumentException.class, () -> provider.embed(
                new ai.andalus.pinisi.api.EmbeddingRequest(List.of(
                        new EmbeddingInput("image", new BinaryContent(MediaType.IMAGE, "image/png", new byte[]{1}))))));
        assertThrows(IllegalArgumentException.class, () -> provider.embed(
                new ai.andalus.pinisi.api.EmbeddingRequest(
                        List.of(new EmbeddingInput("text", new TextContent("hello"))), "other-space", Map.of())));
    }

    @Test
    void rejectsWrongResponseCardinalityAndDimension() {
        var wrongCount = new StubRunner(request -> new EmbeddingResponse(
                request.requestId(), request.model(), List.of(new float[]{1, 2, 3}), 3, Map.of()));
        assertThrows(IllegalStateException.class, () -> provider(wrongCount).embed(
                new ai.andalus.pinisi.api.EmbeddingRequest(List.of(
                        new EmbeddingInput("one", new TextContent("one")),
                        new EmbeddingInput("two", new TextContent("two"))))));
    }

    private static AljabrEmbeddingProvider provider(StubRunner runner) {
        return new AljabrEmbeddingProvider("aljabr", "embedding_gemma2", "0.1.0",
                "embedding-gemma2-text-768", "cosine", 3, 16, runner);
    }

    private static EmbeddingResponse response(EmbeddingRequest request) {
        return new EmbeddingResponse(request.requestId(), request.model(),
                request.inputs().stream().map(text -> new float[]{1, 0, 0}).toList(),
                3, Map.of());
    }

    private static final class StubRunner implements EmbeddingModelRunner {
        private final java.util.function.Function<EmbeddingRequest, EmbeddingResponse> handler;
        private StubRunner(java.util.function.Function<EmbeddingRequest, EmbeddingResponse> handler) {
            this.handler = handler;
        }
        @Override public String modelFamily() { return "embedding_gemma2"; }
        @Override public java.util.concurrent.CompletionStage<EmbeddingResponse> embed(EmbeddingRequest request) {
            return CompletableFuture.completedFuture(handler.apply(request));
        }
    }
}
