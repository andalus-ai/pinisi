package ai.andalus.pinisi.api;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EmbeddingRequestTest {
    @Test
    void binaryContentDefensivelyCopiesPayload() {
        byte[] source = {1, 2, 3};
        var content = new BinaryContent(MediaType.IMAGE, "image/png", source);
        source[0] = 9;

        byte[] returned = content.data();
        assertArrayEquals(new byte[] {1, 2, 3}, returned);
        returned[1] = 9;
        assertArrayEquals(new byte[] {1, 2, 3}, content.data());
    }

    @Test
    void requestSupportsTextImageAudioAndGenericBinaryInputsInOneBatch() {
        var request = new EmbeddingRequest(List.of(
                new EmbeddingInput("text-1", new TextContent("find a red car")),
                new EmbeddingInput("image-1", new BinaryContent(MediaType.IMAGE, "image/jpeg", new byte[] {1})),
                new EmbeddingInput("audio-1", new UriContent(MediaType.AUDIO,
                        URI.create("https://media.example.test/sample.wav"), "audio/wav")),
                new EmbeddingInput("binary-1", new BinaryContent(
                        MediaType.BINARY, "application/octet-stream", new byte[] {2}))));

        assertEquals(4, request.inputs().size());
        assertEquals(MediaType.TEXT, request.inputs().get(0).content().mediaType());
        assertEquals(MediaType.IMAGE, request.inputs().get(1).content().mediaType());
        assertEquals(MediaType.AUDIO, request.inputs().get(2).content().mediaType());
        assertEquals(MediaType.BINARY, request.inputs().get(3).content().mediaType());
    }

    @Test
    void requestRejectsDuplicateInputIds() {
        var first = new EmbeddingInput("same", new TextContent("first"));
        var second = new EmbeddingInput("same", new TextContent("second"));

        assertThrows(IllegalArgumentException.class, () -> new EmbeddingRequest(List.of(first, second)));
    }

    @Test
    void capabilitiesExposeCrossModalPairingsAndBatchLimits() {
        var capabilities = new EmbeddingCapabilities(
                "provider", "model", "revision-1",
                Set.of(MediaType.TEXT, MediaType.IMAGE),
                Set.of(new EmbeddingPair(MediaType.TEXT, MediaType.IMAGE)),
                16, true, true);

        assertTrue(capabilities.supportedPairs().contains(
                new EmbeddingPair(MediaType.TEXT, MediaType.IMAGE)));
        assertEquals(16, capabilities.maxBatchSize());
        assertTrue(capabilities.supportsUriReferences());
    }
}
