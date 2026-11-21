# Search API

This document describes the intended public API shape. It is a design contract, not a statement that these Java types are already implemented. Names and signatures may be refined during implementation while preserving the principles below.

## Design principles
- Framework-neutral Java API.
- Immutable request/response value objects where practical.
- Typed modality, identifier, filter and position values.
- Synchronous APIs for simple embedded use; asynchronous APIs for network and long-running work.
- Explicit pagination, timeout, cancellation and error semantics.
- No provider-specific types in public signatures.

## Engine and collection
Conceptual entry points:

```java
public interface SearchEngine extends AutoCloseable {
    SearchCollection createCollection(CollectionDefinition definition);
    Optional<SearchCollection> getCollection(CollectionId id);
    SearchResponse search(SearchQuery query);
    CompletionStage<SearchResponse> searchAsync(SearchQuery query);
    SearchEngineCapabilities capabilities();
    void close();
}

public interface SearchCollection {
    CollectionId id();
    CollectionDefinition definition();
    IngestionJob ingest(IngestionRequest request);
    SearchResponse search(SearchQuery query);
    Optional<MediaAsset> getAsset(AssetId id);
    List<MediaSegment> getSegments(AssetId id);
}
```

The final API must define collection deletion/update, pagination cursors, timeouts, error types, resource ownership and async cancellation before declaring a stable release.

## Query model
A query contains:
1. Collection scope.
2. Typed input: text, image, audio, video or a combination.
3. Target modalities to search.
4. Structured filters and permission context.
5. Retrieval options: limit, cursor, strategy, reranking and timeout.

Query input modality is distinct from target modality. Cross-modal execution is permitted only when the selected provider declares support.

## Result model
A result identifies the asset and optional segment, matched modality, score, typed position, highlights and safe metadata. Scores are ranking signals, not probabilities unless explicitly calibrated. Do not return raw embeddings by default.

## Error model
Use stable typed errors for invalid query, unsupported modality pairing, missing collection/asset, provider unavailable, incompatible embedding space, source authorization failure, timeout, cancellation and partial indexing. Preserve causes for diagnostics without leaking credentials or sensitive source data.

## Example (illustrative)
```java
SearchQuery query = SearchQuery.builder()
    .collection(collectionId)
    .input(new QueryInput.Text("a red car parked beside a lake"))
    .targetModalities(Set.of(MediaType.IMAGE, MediaType.VIDEO))
    .limit(20)
    .build();

SearchResponse response = engine.search(query);
```
