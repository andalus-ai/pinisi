package ai.andalus.pinisi.provider.aljabr;

import ai.andalus.pinisi.api.BinaryContent;
import ai.andalus.pinisi.api.EmbeddingCapabilities;
import ai.andalus.pinisi.api.EmbeddingInput;
import ai.andalus.pinisi.api.EmbeddingPair;
import ai.andalus.pinisi.api.EmbeddingRequest;
import ai.andalus.pinisi.api.EmbeddingResponse;
import ai.andalus.pinisi.api.EmbeddingResult;
import ai.andalus.pinisi.api.EmbeddingSpace;
import ai.andalus.pinisi.api.EmbeddingVector;
import ai.andalus.pinisi.api.MediaType;
import ai.andalus.pinisi.api.TextContent;
import ai.andalus.pinisi.api.UriContent;
import ai.andalus.pinisi.spi.MultimodalEmbeddingProvider;
import tech.kayys.aljabr.spi.embedding.EmbeddingModelRunner;
import tech.kayys.aljabr.spi.embedding.EmbeddingTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletionException;

/**
 * Pinisi adapter for Aljabr's model-family embedding runner SPI.
 *
 * <p>This first integration intentionally supports text inputs only. Aljabr's
 * current model-family runner request is text-only; the EmbeddingGemma 2 image
 * entry point is a separate model-level API and is not yet wired into that SPI.
 * The loaded runner and its lifecycle are supplied by the host (for example,
 * AQLI); this adapter does not load checkpoints or own runtime resources.</p>
 *
 * <p>The adapter is synchronous because Pinisi's current provider SPI is
 * synchronous. It waits for Aljabr's CompletionStage and propagates failures.</p>
 */
public final class AljabrEmbeddingProvider implements MultimodalEmbeddingProvider {
    private static final EmbeddingPair TEXT_TO_TEXT =
            new EmbeddingPair(MediaType.TEXT, MediaType.TEXT);

    private final String providerId;
    private final String modelId;
    private final String modelRevision;
    private final String embeddingSpaceId;
    private final String metric;
    private final int outputDimension;
    private final int maxBatchSize;
    private final EmbeddingModelRunner runner;
    private final EmbeddingCapabilities capabilities;

    public AljabrEmbeddingProvider(
            String providerId,
            String modelId,
            String modelRevision,
            String embeddingSpaceId,
            String metric,
            int outputDimension,
            int maxBatchSize,
            EmbeddingModelRunner runner) {
        this.providerId = requireText(providerId, "providerId");
        this.modelId = requireText(modelId, "modelId");
        this.modelRevision = modelRevision == null ? "unspecified" : modelRevision;
        this.embeddingSpaceId = requireText(embeddingSpaceId, "embeddingSpaceId");
        this.metric = requireText(metric, "metric");
        if (outputDimension <= 0) throw new IllegalArgumentException("outputDimension must be positive");
        if (maxBatchSize <= 0) throw new IllegalArgumentException("maxBatchSize must be positive");
        this.outputDimension = outputDimension;
        this.maxBatchSize = maxBatchSize;
        this.runner = Objects.requireNonNull(runner, "runner");
        this.capabilities = new EmbeddingCapabilities(
                this.providerId, this.modelId, this.modelRevision,
                java.util.Set.of(MediaType.TEXT), java.util.Set.of(TEXT_TO_TEXT),
                this.maxBatchSize, false, false);
    }

    @Override public String id() { return providerId; }

    @Override public EmbeddingCapabilities capabilities() { return capabilities; }

    @Override
    public EmbeddingResponse embed(EmbeddingRequest request) {
        Objects.requireNonNull(request, "request");
        if (request.targetEmbeddingSpaceId() != null
                && !embeddingSpaceId.equals(request.targetEmbeddingSpaceId())) {
            throw new IllegalArgumentException("Requested embedding space " + request.targetEmbeddingSpaceId()
                    + " does not match configured Aljabr space " + embeddingSpaceId);
        }

        List<String> texts = new ArrayList<>(request.inputs().size());
        for (EmbeddingInput input : request.inputs()) {
            if (input.content() instanceof TextContent text) {
                texts.add(text.text());
            } else if (input.content() instanceof BinaryContent || input.content() instanceof UriContent) {
                throw new IllegalArgumentException("Aljabr text runner accepts TextContent only");
            } else {
                throw new IllegalArgumentException("Unsupported Pinisi embedding content: "
                        + input.content().getClass().getName());
            }
        }

        EmbeddingTask task = taskFrom(request.parameters());
        Map<String, Object> parameters = new java.util.HashMap<>();
        request.parameters().forEach((key, value) -> {
            if (!"task".equals(key)) parameters.put(key, value);
        });
        String requestId = UUID.randomUUID().toString();
        tech.kayys.aljabr.spi.embedding.EmbeddingRequest aljabrRequest = new tech.kayys.aljabr.spi.embedding.EmbeddingRequest(
                requestId, modelId, texts, Map.copyOf(parameters), task, outputDimension);

        tech.kayys.aljabr.spi.embedding.EmbeddingResponse aljabrResponse;
        try {
            aljabrResponse = Objects.requireNonNull(
                    runner.embed(aljabrRequest), "Aljabr runner returned a null CompletionStage")
                    .toCompletableFuture().join();
        } catch (CompletionException failure) {
            if (failure.getCause() instanceof RuntimeException runtime) throw runtime;
            throw failure;
        }
        validateResponse(aljabrResponse, requestId, texts.size());

        List<EmbeddingResult> results = new ArrayList<>(request.inputs().size());
        for (int index = 0; index < request.inputs().size(); index++) {
            EmbeddingInput input = request.inputs().get(index);
            EmbeddingSpace space = new EmbeddingSpace(
                    embeddingSpaceId, providerId, modelId, modelRevision,
                    MediaType.TEXT.name(), outputDimension, metric);
            results.add(new EmbeddingResult(input.id(),
                    new EmbeddingVector(space, aljabrResponse.embeddings().get(index))));
        }
        return new EmbeddingResponse(results);
    }

    private void validateResponse(tech.kayys.aljabr.spi.embedding.EmbeddingResponse response, String requestId, int inputCount) {
        Objects.requireNonNull(response, "Aljabr runner returned a null response");
        if (!requestId.equals(response.requestId())) {
            throw new IllegalStateException("Aljabr response requestId does not match submitted request");
        }
        if (!modelId.equals(response.model())) {
            throw new IllegalStateException("Aljabr response model does not match configured model");
        }
        if (response.embeddings() == null || response.embeddings().size() != inputCount) {
            throw new IllegalStateException("Aljabr must return exactly one embedding per input");
        }
        if (response.dimension() != outputDimension) {
            throw new IllegalStateException("Aljabr response dimension " + response.dimension()
                    + " does not match configured dimension " + outputDimension);
        }
        for (float[] vector : response.embeddings()) {
            if (vector == null || vector.length != outputDimension) {
                throw new IllegalStateException("Aljabr returned a vector with an unexpected dimension");
            }
        }
    }

    private static EmbeddingTask taskFrom(Map<String, String> parameters) {
        String value = parameters.get("task");
        if (value == null || value.isBlank()) return EmbeddingTask.UNSPECIFIED;
        try {
            return EmbeddingTask.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("Unsupported Aljabr embedding task: " + value, failure);
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
