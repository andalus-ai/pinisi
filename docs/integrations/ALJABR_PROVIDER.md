# Aljabr provider integration

## Current boundary

`pinisi-provider-aljabr` is an optional Java 25 integration module. The Pinisi API, SPI and core remain Java 21 and do not acquire Aljabr dependencies.

The adapter consumes Aljabr's `EmbeddingModelRunner` SPI, not EmbeddingGemma 2 implementation classes. The host supplies an already-loaded runner and owns model loading, resource lifecycle, scheduling and shutdown. This allows other Aljabr model-family runners to be used without coupling Pinisi to a particular model implementation.

## First supported path: text

The current Aljabr `EmbeddingRequest` accepts `List<String>` inputs and the current `EmbeddingModelRunner` returns ordered `float[]` vectors. The adapter therefore advertises text input only, maps Pinisi request IDs by the guaranteed output order, and validates response request ID, model, cardinality and dimensions.

Embedding task intent is supplied through the Pinisi request parameter named `task` using an Aljabr `EmbeddingTask` enum name. Other string parameters are forwarded as model parameters (for example, EmbeddingGemma 2's `title` / `titles`).

The embedding-space ID, provider/model/revision identity, metric, output dimension and batch limit are explicit adapter configuration. A caller may target only the configured space.

## Why image is not advertised yet

Aljabr PR #1021 added `EmbeddingGemma2ImageEmbeddingRunner`, but that entry point accepts pre-expanded token IDs and decoded RGB images. It is not an implementation of `EmbeddingModelRunner`; tokenizer prompt expansion, image decoding, checkpoint/model loading and runner lifecycle remain outside that API. The generic Aljabr embedding SPI is still text-only.

Pinisi must not claim image support merely because a model-level image method exists. A future Aljabr typed multimodal embedding SPI or a separately validated model-host adapter must bridge those boundaries before Pinisi advertises text-to-image or image-to-image capability.

## Build

The module is excluded from the default Java 21 reactor. On JDK 25, build it explicitly:

```bash
mvn -Dpinisi.aljabr=true -Paljabr-provider -pl pinisi-provider-aljabr -am test
```

The Aljabr `aljabr-spi-inference` artifact must be available from the configured GitHub Packages repository or the local Maven repository. Configure GitHub Packages credentials in Maven `settings.xml`; never commit credentials.

The adapter blocks on Aljabr's `CompletionStage` because Pinisi's current `MultimodalEmbeddingProvider` contract is synchronous. It does not introduce hidden retries or scheduling. A future async Pinisi provider contract can remove this bridge if asynchronous embedding becomes a core requirement.
