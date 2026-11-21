package ai.andalus.pinisi.connector.filesystem;
import ai.andalus.pinisi.api.Asset;
import ai.andalus.pinisi.api.MediaType;
import ai.andalus.pinisi.spi.MediaSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
/** Read-only image source rooted at one local directory. Symbolic links are not followed. */
public final class LocalFileSystemImageSource implements MediaSource {
    private static final Set<String> EXTENSIONS = Set.of("jpg","jpeg","png","gif","webp","bmp","tif","tiff");
    private final String sourceId;
    private final Path root;
    public LocalFileSystemImageSource(String sourceId, Path root) throws IOException {
        if (sourceId == null || sourceId.isBlank()) throw new IllegalArgumentException("sourceId is required");
        this.sourceId = sourceId;
        this.root = Objects.requireNonNull(root, "root").toRealPath();
        if (!Files.isDirectory(this.root)) throw new IllegalArgumentException("root must be a directory");
    }
    @Override public String id() { return sourceId; }
    @Override public List<Asset> discover() throws IOException {
        List<Asset> assets = new ArrayList<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(p -> !p.equals(root))
                    .filter(p -> Files.isRegularFile(p, NOFOLLOW_LINKS))
                    .filter(LocalFileSystemImageSource::isImage).sorted().toList()) {
                Path safe = path.toRealPath(NOFOLLOW_LINKS);
                if (!safe.startsWith(root)) continue;
                BasicFileAttributes a = Files.readAttributes(safe, BasicFileAttributes.class, NOFOLLOW_LINKS);
                String key = root.relativize(safe).toString().replace(FileSystems.getDefault().getSeparator(), "/");
                Map<String,String> metadata = new LinkedHashMap<>();
                metadata.put("sizeBytes", Long.toString(a.size()));
                metadata.put("lastModified", a.lastModifiedTime().toString());
                String contentType = Files.probeContentType(safe);
                if (contentType != null) metadata.put("contentType", contentType);
                assets.add(new Asset(stableId(sourceId + ":" + key), "default", sourceId, key,
                        MediaType.IMAGE, safe.toUri(), metadata));
            }
        }
        return List.copyOf(assets);
    }
    @Override public InputStream open(Asset asset) throws IOException {
        Objects.requireNonNull(asset, "asset");
        if (!sourceId.equals(asset.sourceId())) throw new IllegalArgumentException("Asset belongs to a different source");
        Path candidate = root.resolve(asset.sourceKey()).normalize();
        if (!candidate.startsWith(root)) throw new SecurityException("Asset path escapes configured root");
        Path safe = candidate.toRealPath(NOFOLLOW_LINKS);
        if (!safe.startsWith(root) || !Files.isRegularFile(safe, NOFOLLOW_LINKS) || !isImage(safe))
            throw new SecurityException("Asset is not a regular image inside the configured root");
        return Files.newInputStream(safe);
    }
    private static boolean isImage(Path p) {
        String name = p.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 && EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }
    private static String stableId(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}