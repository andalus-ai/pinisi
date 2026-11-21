package ai.andalus.pinisi.api;

/** Typed content accepted by an embedding provider. URI references are not fetched by Pinisi core. */
public sealed interface EmbeddingContent permits TextContent, BinaryContent, UriContent {
    MediaType mediaType();
}
