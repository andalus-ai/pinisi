# Aljabr and AQLI Integration Gate

**Status:** Evidence-based integration decision; no runtime adapter is approved yet.  
**Reviewed:** 2026-11-21

## Purpose

Define the safe integration boundary for real embedding providers without coupling Pinisi core to an inference server or assuming that similarly shaped vectors are semantically compatible.

## Findings from the upstream project documentation

The following findings are based on the upstream repositories' current `main` documentation. They are documentation-level findings, not proof that a particular model, runner, endpoint or artifact has passed an end-to-end runtime test.

### Aljabr

Aljabr describes itself as the reusable Java ML/compute framework. Its documented ownership includes tensors, operators, model execution, format APIs, compute backends and backend capability/readiness mechanisms. Its framework architecture explicitly allows direct model-forward execution by applications and AQLI.

Aljabr's backend ownership decision distinguishes capabilities that are defined, implemented, installed, available, initialized and validated. These states must not be collapsed into a single "supported" flag. The decision also requires explicit resource ownership and explicit fallback policy.

Relevant upstream references:
- [Aljabr README](https://github.com/andalus-ai/aljabr/blob/main/README.md)
- [Framework Architecture](https://github.com/andalus-ai/aljabr/blob/main/docs/architecture/FRAMEWORK_ARCHITECTURE.md)
- [ADR-0001: Backend Discovery, Configuration, and Profiling Ownership](https://github.com/andalus-ai/aljabr/blob/main/docs/architecture/ADR-0001-BACKEND-OWNERSHIP.md)

### AQLI

AQLI describes itself as the inference and serving runtime. It owns request lifecycle, scheduling, model-serving operations, streaming, SDK/CLI/API surfaces and deployment behavior. It consumes Aljabr; Aljabr must not depend on AQLI.

The documented REST API includes:
- `POST /v1/embeddings`, with examples using a string or batch of strings as `input`.
- `POST /v1/chat/completions`, with a vision example that supplies an `image_url` content item for image analysis.

These examples establish two different documented use cases: text embedding and image-conditioned chat. They do **not** establish an image-embedding endpoint, a shared text/image vector space, or a supported cross-modal retrieval model. Vision-language generation must not be treated as image embedding.

Relevant upstream references:
- [AQLI README](https://github.com/andalus-ai/aqli/blob/main/README.md)
- [API Quick Reference](https://github.com/andalus-ai/aqli/blob/main/docs/API_QUICK_REFERENCE.md)
- [API Examples](https://github.com/andalus-ai/aqli/blob/main/docs/API_EXAMPLES.md)
- [Module Ownership Matrix](https://github.com/andalus-ai/aqli/blob/main/docs/architecture/MODULE_OWNERSHIP_MATRIX.md)

## Pinisi integration decision

1. **Keep Pinisi core independent.** Core API, SPI, ingestion and retrieval modules must not depend on AQLI, Aljabr implementation modules, Quarkus, or remote-serving client libraries.
2. **Keep the existing provider seam.** Concrete model/runtime integrations implement Pinisi's embedding-provider SPI in optional adapter modules. Do not move model-specific behavior into filesystem connectors or the search engine.
3. **Prefer direct Aljabr integration for embedded use** only after a public, stable model-execution API and a concrete image encoder path are identified and validated. Pinisi must own neither Aljabr backend selection policy nor native resource lifecycle.
4. **Use AQLI as an optional remote inference provider** only when its deployed API and selected model expose the required modality-specific embedding operations. AQLI's documented text-only embedding examples are insufficient for image indexing.
5. **Do not add an adapter that returns synthetic or unrelated vectors.** A text vector from one model and an image vector from another model are not comparable merely because their dimensions match.
6. **Do not commit to EmbeddingGemma 2 yet.** Validate the exact model variant, supported input modalities, pooling/projection behavior, tokenizer/image preprocessing, output normalization, dimensions, licensing and runtime compatibility.

## Required capability contract

Before enabling a search pairing, an adapter must provide verifiable metadata for each operation:

- Model identifier and immutable revision/digest.
- Input modality and preprocessing contract.
- Output dimension, numeric type and normalization behavior.
- A stable embedding-space identifier that captures model revision and projection semantics.
- Supported query/document modality pairs (for example text-to-text, image-to-image and text-to-image).
- Runtime/provider identity and validated version/platform constraints.

The embedding-space identifier is semantic, not just dimensional. Pinisi must reject comparisons across incompatible spaces.

## Acceptance gates for the first real image-search provider

- [ ] Identify a public Aljabr API or AQLI API/SDK operation that accepts image bytes or a supported image reference and returns an embedding.
- [ ] Verify that the same model family/revision can embed text queries into the compatible shared space, or document a supported asymmetric query/document pairing.
- [ ] Verify preprocessing, crop/resize policy, channel order, normalization, pooling/projection and output dimension against model documentation and implementation.
- [ ] Prove the selected Aljabr backend or AQLI runner is implemented, installed, available and initialized on the target platform.
- [ ] Add a small deterministic fixture set and validate expected nearest-neighbor behavior, not merely vector shape.
- [ ] Test mismatched model revisions/spaces and unsupported modality pairings fail explicitly.
- [ ] Test stream closure, cancellation, exceptions and model/runtime resource release.
- [ ] Run focused upstream and Pinisi tests on the supported JDK/runtime combination.
- [ ] Keep the provider optional and verify that Pinisi core builds and runs without it.

## Recommended implementation sequence

1. Inspect the concrete Aljabr public model-execution and vision-model modules, including their Gradle dependency coordinates and runnable examples. Documentation architecture alone is not an executable API contract.
2. Inspect AQLI's actual embedding resource/SDK implementation and model capability metadata. Confirm whether image embedding is implemented or only documented as a future/vision-chat feature.
3. Select one concrete model/runtime combination and record its validated modality matrix.
4. Implement the smallest optional provider adapter against Pinisi's existing SPI. Avoid introducing a general-purpose Aljabr abstraction inside Pinisi.
5. Add an end-to-end test that indexes image fixtures and retrieves them with compatible text queries.
6. Only then describe semantic image search as implemented.

## Current conclusion

The filesystem ingestion slice can discover images and pass their streams to an image-capable provider, but it is not itself a semantic image-search implementation. Source inspection on 2026-11-21 found substantial Aljabr EmbeddingGemma 2 image-reference components, but the model-family plugin explicitly advertises `supported_modalities=text`, defers image/audio/video, and says the inference runner is not registered. The image path is therefore a float32 reference pipeline, not a runnable registered image embedding model. AQLI exposes an `EmbeddingService` multimodal SPI and a `UnifiedMultimodalRuntime` extension point, but the inspected tree contains no concrete `EmbeddingService` implementation; its compatibility `EmbeddingRequest` still accepts `List<String>`. Thus neither project currently provides a verified, runnable image-to-shared-text-space embedding path. The next code change should follow runner completion and source-level API verification, not precede it.


## Source-level audit (2026-11-21)

The upstream source trees were inspected after the initial documentation review. This materially narrows the integration decision.

### Aljabr: substantial reference implementation, runner not registered

The `models/aljabr-model-embedding-gemma2` module contains:
- `EmbeddingGemma2MultimodalEncoder`, which composes decoded RGB images, a vision pipeline, image-placeholder replacement, and the text backbone.
- A Gemma4-compatible image processor, vision tower, spatial pooler, image-to-text soft-token projection, and multimodal checkpoint mapper.
- Unit tests for multimodal image projection and synthetic checkpoint/image pipeline components.
- A separate `aljabr-safetensor-embedding-gemma2` module whose README describes a **text-only** model bundle.

However, the model-family plugin is explicit:
- `supported_modalities=text`
- `deferred_modalities=image,audio,video`
- `runner_status=not_registered_until_forward_path_is_complete`
- The plugin intentionally does not declare direct Safetensors inference readiness.

The module README likewise says these float32 reference operations are not a complete registered model runner; full parity against a published checkpoint and runner registration remain outstanding. The image processor accepts already-decoded RGB samples, so codec integration is external. The model module uses Java 25, while Pinisi currently targets Java 21; this is a material compatibility blocker unless Aljabr publishes a compatible artifact/toolchain or Pinisi raises its baseline deliberately.

Source references:
- [EmbeddingGemma 2 model module README](https://github.com/andalus-ai/aljabr/blob/main/models/aljabr-model-embedding-gemma2/README.md)
- [Model-family plugin](https://github.com/andalus-ai/aljabr/blob/main/models/aljabr-model-embedding-gemma2/src/main/java/tech/kayys/aljabr/models/embeddinggemma2/EmbeddingGemma2ModelFamilyPlugin.java)
- [Multimodal encoder](https://github.com/andalus-ai/aljabr/blob/main/models/aljabr-model-embedding-gemma2/src/main/java/tech/kayys/aljabr/models/embeddinggemma2/EmbeddingGemma2MultimodalEncoder.java)
- [Safetensors adapter README](https://github.com/andalus-ai/aljabr/blob/main/engine/safetensor/aljabr-safetensor-embedding-gemma2/README.md)
- [Model module Gradle build](https://github.com/andalus-ai/aljabr/blob/main/models/aljabr-model-embedding-gemma2/build.gradle.kts)

### AQLI: SPI exists, concrete provider not found

AQLI contains a multimodal `EmbeddingService` SPI with single-item and batch methods over Aljabr `MultimodalContent`, returning float vectors. It also contains a `UnifiedMultimodalRuntime` ServiceLoader extension point and manifest/plan metadata for input modalities and readiness.

The inspected source tree did not reveal a concrete implementation of `EmbeddingService` or a registered EmbeddingGemma 2 unified runtime. The compatibility inference `EmbeddingRequest` still accepts text `List<String>`; the REST `/v1/embeddings` resource maps to that DTO. Therefore the SPI is an extension contract, not evidence of a running image embedding endpoint.

Source references:
- [AQLI EmbeddingService SPI](https://github.com/andalus-ai/aqli/blob/main/framework/spi/aqli-spi-multimodal/src/main/java/tech/kayys/aqli/spi/embedding/EmbeddingService.java)
- [AQLI UnifiedMultimodalRuntime SPI](https://github.com/andalus-ai/aqli/blob/main/framework/spi/aqli-spi-multimodal/src/main/java/tech/kayys/aqli/spi/multimodal/UnifiedMultimodalRuntime.java)
- [AQLI compatibility EmbeddingRequest](https://github.com/andalus-ai/aqli/blob/main/framework/spi/aqli-spi-inference/src/main/java/tech/kayys/aqli/spi/embedding/EmbeddingRequest.java)
- [AQLI REST EmbeddingsResource](https://github.com/andalus-ai/aqli/blob/main/framework/api/aqli-api/src/main/java/tech/kayys/aqli/server/api/v1/EmbeddingsResource.java)

### Revised recommendation

Do not create a Pinisi Aljabr or AQLI adapter yet. The smallest credible next upstream milestone is to complete and register Aljabr's EmbeddingGemma 2 runner for text and image, advertise the true modality matrix only after runtime validation, and supply a Java 21-compatible integration surface (or an explicit Pinisi Java baseline decision). In parallel, AQLI can add a concrete `EmbeddingService` implementation backed by that runner and expose a modality-aware request contract. Pinisi should then implement a thin optional adapter, with an end-to-end fixture test proving text-to-image retrieval in one verified embedding space.

This audit is source-level only. No upstream build, model checkpoint load, backend initialization, or real-checkpoint parity test was run in this environment.
