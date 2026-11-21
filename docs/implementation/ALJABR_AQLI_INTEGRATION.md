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

The filesystem ingestion slice can discover images and pass their streams to an image-capable provider, but it is not itself a semantic image-search implementation. Based on the upstream documentation reviewed on 2026-11-21, neither an Aljabr image-embedding Java call path nor an AQLI image-embedding API has yet been established. The next code change should follow source-level API verification, not precede it.
