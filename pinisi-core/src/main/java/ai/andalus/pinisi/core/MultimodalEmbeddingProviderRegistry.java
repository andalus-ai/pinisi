package ai.andalus.pinisi.core;

import ai.andalus.pinisi.api.EmbeddingCapabilities;
import ai.andalus.pinisi.api.EmbeddingContent;
import ai.andalus.pinisi.api.EmbeddingInput;
import ai.andalus.pinisi.api.EmbeddingPair;
import ai.andalus.pinisi.api.EmbeddingRequest;
import ai.andalus.pinisi.api.EmbeddingResponse;
import ai.andalus.pinisi.api.EmbeddingResult;
import ai.andalus.pinisi.api.EmbeddingSpace;
import ai.andalus.pinisi.api.MediaType;
import ai.andalus.pinisi.spi.MultimodalEmbeddingProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves multimodal providers and executes requests within their declared capabilities.
 * This registry deliberately knows nothing about Aljabr, AQLI or any concrete model runtime.
 */
public final class MultimodalEmbeddingProviderRegistry {
    private final Map<String, MultimodalEmbeddingProvider> providers;

    public MultimodalEmbeddingProviderRegistry(Collection<? extends MultimodalEmbeddingProvider> providers) {
        Objects.requireNonNull(providers, "providers");
        Map<String, MultimodalEmbeddingProvider> registered = new HashMap<>();
        for (MultimodalEmbeddingProvider provider : providers) {
            Objects.requireNonNull(provider, "provider");
            String id = requireText(provider.id(), "provider.id");
            EmbeddingCapabilities capabilities = Objects.requireNonNull(provider.capabilities(), "provider.capabilities");
            if (!id.equals(capabilities.providerId())) {
                throw new IllegalArgumentException("Provider id must match capabilities.providerId: " + id);
            }
            if (registered.putIfAbsent(id, provider) != null) {
                throw new IllegalArgumentException("Duplicate multimodal embedding provider: " + id);
            }
        }
        this.providers = Map.copyOf(registered);
    }

    public Optional<MultimodalEmbeddingProvider> find(String providerId) {
        return Optional.ofNullable(providers.get(requireText(providerId, "providerId")));
    }

    public MultimodalEmbeddingProvider require(String providerId) {
        return find(providerId).orElseThrow(() ->
                new IllegalArgumentException("Unknown multimodal embedding provider: " + providerId));
    }

    public List<MultimodalEmbeddingProvider> providers() {
        return List.copyOf(providers.values());
    }

    public boolean supportsPair(String providerId, EmbeddingPair pair) {
        Objects.requireNonNull(pair, "pair");
        return require(providerId).capabilities().supportedPairs().contains(pair);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
