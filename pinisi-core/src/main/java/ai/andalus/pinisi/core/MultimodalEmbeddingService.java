package ai.andalus.pinisi.core;

import ai.andalus.pinisi.api.BinaryContent;
import ai.andalus.pinisi.api.EmbeddingCapabilities;
import ai.andalus.pinisi.api.EmbeddingInput;
import ai.andalus.pinisi.api.EmbeddingRequest;
import ai.andalus.pinisi.api.EmbeddingResponse;
import ai.andalus.pinisi.api.EmbeddingResult;
import ai.andalus.pinisi.api.EmbeddingSpace;
import ai.andalus.pinisi.api.MediaType;
import ai.andalus.pinisi.api.UriContent;
import ai.andalus.pinisi.spi.MultimodalEmbeddingProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Capability validation, bounded batching and response validation for multimodal embedding. */
public final class MultimodalEmbeddingService {
    private final MultimodalEmbeddingProviderRegistry providers;

    public MultimodalEmbeddingService(MultimodalEmbeddingProviderRegistry providers) {
        this.providers = Objects.requireNonNull(providers, "providers");
    }

    public EmbeddingResponse embed(String providerId, EmbeddingRequest request) {
        Objects.requireNonNull(request, "request");
        MultimodalEmbeddingProvider provider = providers.require(providerId);
        EmbeddingCapabilities capabilities = provider.capabilities();
        validateRequest(capabilities, request);

        List<EmbeddingResult> orderedResults = new ArrayList<>(request.inputs().size());
        int batchSize = capabilities.maxBatchSize();
        for (int start = 0; start < request.inputs().size(); start += batchSize) {
            List<EmbeddingInput> batch = request.inputs().subList(
                    start, Math.min(start + batchSize, request.inputs().size()));
            EmbeddingRequest batchRequest = new EmbeddingRequest(
                    batch, request.targetEmbeddingSpaceId(), request.parameters());
            EmbeddingResponse response = Objects.requireNonNull(
                    provider.embed(batchRequest), "Provider returned a null embedding response");
            orderedResults.addAll(validateResponse(capabilities, batchRequest, response));
        }
        return new EmbeddingResponse(orderedResults);
    }

    private static void validateRequest(EmbeddingCapabilities capabilities, EmbeddingRequest request) {
        for (EmbeddingInput input : request.inputs()) {
            MediaType modality = input.content().mediaType();
            if (!capabilities.inputModalities().contains(modality)) {
                throw new IllegalArgumentException("Provider " + capabilities.providerId()
                        + " does not accept modality " + modality);
            }
            if (input.content() instanceof BinaryContent && !capabilities.supportsBinaryContent()) {
                throw new IllegalArgumentException("Provider " + capabilities.providerId()
                        + " does not accept binary content");
            }
            if (input.content() instanceof UriContent && !capabilities.supportsUriReferences()) {
                throw new IllegalArgumentException("Provider " + capabilities.providerId()
                        + " does not accept URI references");
            }
        }
    }

    private static List<EmbeddingResult> validateResponse(
            EmbeddingCapabilities capabilities, EmbeddingRequest request, EmbeddingResponse response) {
        Map<String, EmbeddingResult> byId = new HashMap<>();
        Set<String> requestedIds = new HashSet<>();
        for (EmbeddingInput input : request.inputs()) requestedIds.add(input.id());

        for (EmbeddingResult result : response.results()) {
            if (!requestedIds.contains(result.inputId())) {
                throw new IllegalStateException("Provider returned an unknown input id: " + result.inputId());
            }
            if (byId.putIfAbsent(result.inputId(), result) != null) {
                throw new IllegalStateException("Provider returned duplicate input id: " + result.inputId());
            }
            validateEmbeddingSpace(capabilities, request, result);
        }
        if (!byId.keySet().equals(requestedIds)) {
            Set<String> missing = new HashSet<>(requestedIds);
            missing.removeAll(byId.keySet());
            throw new IllegalStateException("Provider omitted embedding results for input ids: " + missing);
        }

        List<EmbeddingResult> ordered = new ArrayList<>(request.inputs().size());
        for (EmbeddingInput input : request.inputs()) ordered.add(byId.get(input.id()));
        return ordered;
    }

    private static void validateEmbeddingSpace(
            EmbeddingCapabilities capabilities, EmbeddingRequest request, EmbeddingResult result) {
        EmbeddingSpace space = result.embedding().space();
        if (!capabilities.providerId().equals(space.provider())
                || !capabilities.modelId().equals(space.model())
                || !capabilities.modelRevision().equals(space.revision())) {
            throw new IllegalStateException("Provider returned a vector from an undeclared embedding provider/model/revision");
        }
        if (request.targetEmbeddingSpaceId() != null
                && !request.targetEmbeddingSpaceId().equals(space.id())) {
            throw new IllegalStateException("Provider returned embedding space " + space.id()
                    + " but request targeted " + request.targetEmbeddingSpaceId());
        }

        MediaType requestedModality = request.inputs().stream()
                .filter(input -> input.id().equals(result.inputId()))
                .findFirst().orElseThrow().content().mediaType();
        if (requestedModality != result.embedding().modality()) {
            throw new IllegalStateException("Embedding vector modality " + result.embedding().modality()
                    + " does not match input modality " + requestedModality
                    + " for " + result.inputId());
        }
    }
}
