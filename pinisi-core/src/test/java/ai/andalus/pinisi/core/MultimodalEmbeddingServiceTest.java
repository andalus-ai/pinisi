package ai.andalus.pinisi.core;

import ai.andalus.pinisi.api.*;
import ai.andalus.pinisi.spi.MultimodalEmbeddingProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MultimodalEmbeddingServiceTest {
    private static final EmbeddingCapabilities CAPABILITIES = new EmbeddingCapabilities(
            "test-provider", "test-model", "1",
            Set.of(MediaType.TEXT, MediaType.IMAGE),
            Set.of(new EmbeddingPair(MediaType.TEXT, MediaType.IMAGE)),
            2, true, true);

    @Test
    void splitsBatchesAndRestoresOriginalInputOrder() {
        var provider = new TestProvider(request -> new EmbeddingResponse(
                request.inputs().stream().map(input -> result(input.id(), input.content().mediaType())).toList()));
        var service = service(provider);
        var response = service.embed("test-provider", request(
                input("first", new TextContent("hello")),
                input("second", new BinaryContent(MediaType.IMAGE, "image/png", new byte[]{1})),
                input("third", new TextContent("world"))));

        assertEquals(List.of("first", "second", "third"),
                response.results().stream().map(EmbeddingResult::inputId).toList());
        assertEquals(2, provider.calls);
        assertEquals(List.of(2, 1), provider.batchSizes);
    }

    @Test
    void rejectsUnsupportedModalityBeforeCallingProvider() {
        var provider = new TestProvider(request -> fail("provider must not be called"));
        var service = service(provider);
        assertThrows(IllegalArgumentException.class,
                () -> service.embed("test-provider", request(input("audio", new BinaryContent(
                        MediaType.AUDIO, "audio/wav", new byte[]{1})))));
        assertEquals(0, provider.calls);
    }

    @Test
    void rejectsProviderOmittingResults() {
        var provider = new TestProvider(request -> new EmbeddingResponse(
                List.of(result(request.inputs().getFirst().id(), request.inputs().getFirst().content().mediaType()))));
        var service = service(provider);
        assertThrows(IllegalStateException.class, () -> service.embed("test-provider", request(
                input("one", new TextContent("one")), input("two", new TextContent("two")))));
    }

    @Test
    void rejectsResultsFromWrongEmbeddingSpace() {
        var provider = new TestProvider(request -> new EmbeddingResponse(List.of(
                new EmbeddingResult(request.inputs().getFirst().id(),
                        new EmbeddingVector(new EmbeddingSpace("wrong", "test-provider", "test-model",
                                "1", "TEXT", 3, "cosine"), new float[]{1, 0, 0})))));
        var service = service(provider);
        assertThrows(IllegalStateException.class, () -> service.embed("test-provider",
                new EmbeddingRequest(List.of(input("one", new TextContent("one"))), "expected-space", Map.of())));
    }

    @Test
    void rejectsUriWhenProviderDoesNotDeclareUriSupport() {
        var noUri = new TestProvider(request -> fail("provider must not be called"),
                new EmbeddingCapabilities("test-provider", "test-model", "1",
                        Set.of(MediaType.IMAGE), Set.of(), 2, true, false));
        var service = service(noUri);
        assertThrows(IllegalArgumentException.class, () -> service.embed("test-provider",
                request(input("uri", new UriContent(MediaType.IMAGE,
                        java.net.URI.create("https://example.test/image.png"), "image/png")))));
    }

    @Test
    void rejectsDuplicateProviderRegistration() {
        var provider = new TestProvider(request -> new EmbeddingResponse(
                request.inputs().stream().map(input -> result(input.id(), input.content().mediaType())).toList()));
        assertThrows(IllegalArgumentException.class,
                () -> new MultimodalEmbeddingProviderRegistry(List.of(provider, provider)));
    }

    private static MultimodalEmbeddingService service(TestProvider provider) {
        return new MultimodalEmbeddingService(new MultimodalEmbeddingProviderRegistry(List.of(provider)));
    }

    private static EmbeddingRequest request(EmbeddingInput... inputs) {
        return new EmbeddingRequest(List.of(inputs));
    }

    private static EmbeddingInput input(String id, EmbeddingContent content) {
        return new EmbeddingInput(id, content);
    }

    private static EmbeddingResult result(String id, MediaType modality) {
        return new EmbeddingResult(id, new EmbeddingVector(
                new EmbeddingSpace("space-" + modality, "test-provider", "test-model",
                        "1", modality.name(), 3, "cosine"), new float[]{1, 0, 0}));
    }

    private static final class TestProvider implements MultimodalEmbeddingProvider {
        private final java.util.function.Function<EmbeddingRequest, EmbeddingResponse> handler;
        private final EmbeddingCapabilities capabilities;
        private int calls;
        private final java.util.ArrayList<Integer> batchSizes = new java.util.ArrayList<>();

        private TestProvider(java.util.function.Function<EmbeddingRequest, EmbeddingResponse> handler) {
            this(handler, CAPABILITIES);
        }

        private TestProvider(java.util.function.Function<EmbeddingRequest, EmbeddingResponse> handler,
                             EmbeddingCapabilities capabilities) {
            this.handler = handler;
            this.capabilities = capabilities;
        }

        @Override public String id() { return capabilities.providerId(); }
        @Override public EmbeddingCapabilities capabilities() { return capabilities; }
        @Override public EmbeddingResponse embed(EmbeddingRequest request) {
            calls++;
            batchSizes.add(request.inputs().size());
            return handler.apply(request);
        }
    }
}
