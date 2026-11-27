package ai.andalus.pinisi.core;

import ai.andalus.pinisi.spi.CollectionDeletionJournal;
import ai.andalus.pinisi.spi.CollectionDeletionOperation;
import ai.andalus.pinisi.spi.CollectionDeletionStatus;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * File-backed deletion journal. Each operation is atomically replaced and all
 * mutations are serialized across threads and processes using a directory lock.
 */
public final class FileCollectionDeletionJournal implements CollectionDeletionJournal {
    private static final ConcurrentMap<Path, Object> JVM_LOCKS = new ConcurrentHashMap<>();
    private final Path directory;
    private final Path lockFile;
    private final Object jvmLock;

    public FileCollectionDeletionJournal(Path directory) {
        this.directory = Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
        this.lockFile = this.directory.resolve(".journal.lock");
        this.jvmLock = JVM_LOCKS.computeIfAbsent(this.lockFile, ignored -> new Object());
        try {
            Files.createDirectories(this.directory);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create deletion journal directory", e);
        }
    }

    @Override public boolean create(CollectionDeletionOperation operation) {
        Objects.requireNonNull(operation, "operation");
        return locked(() -> {
            Path target = operationPath(operation.collectionId());
            if (Files.exists(target)) return false;
            writeAtomically(target, encode(operation));
            return true;
        });
    }

    @Override public Optional<CollectionDeletionOperation> find(String collectionId) {
        return locked(() -> {
            Path path = operationPath(requireId(collectionId));
            return Files.exists(path) ? Optional.of(read(path)) : Optional.empty();
        });
    }

    @Override public List<CollectionDeletionOperation> listIncomplete() {
        return locked(() -> {
            var result = new ArrayList<CollectionDeletionOperation>();
            try (var paths = Files.list(directory)) {
                paths.filter(path -> path.getFileName().toString().endsWith(".properties"))
                        .sorted().map(this::read)
                        .filter(operation -> operation.status() != CollectionDeletionStatus.COMPLETED)
                        .forEach(result::add);
            }
            return List.copyOf(result);
        });
    }

    @Override public boolean replace(CollectionDeletionOperation expected,
                                     CollectionDeletionOperation replacement) {
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(replacement, "replacement");
        if (!expected.collectionId().equals(replacement.collectionId()))
            throw new IllegalArgumentException("Cannot replace an operation for another collection");
        return locked(() -> {
            Path path = operationPath(expected.collectionId());
            if (!Files.exists(path)) return false;
            CollectionDeletionOperation current = read(path);
            if (current.revision() != expected.revision()) return false;
            if (replacement.revision() != expected.revision() + 1)
                throw new IllegalArgumentException("Replacement revision must increment by one");
            writeAtomically(path, encode(replacement));
            return true;
        });
    }

    private CollectionDeletionOperation read(Path path) {
        Properties p = new Properties();
        try (var input = Files.newInputStream(path)) {
            p.load(input);
            int count = Integer.parseInt(required(p, "completed.count"));
            if (count < 0 || count > 100_000) throw new IllegalStateException("Invalid completed-store count");
            var completed = new ArrayList<String>(count);
            for (int i = 0; i < count; i++) completed.add(required(p, "completed." + i));
            return new CollectionDeletionOperation(required(p, "collectionId"),
                    CollectionDeletionStatus.valueOf(required(p, "status")), completed,
                    p.getProperty("failedComponentId"), p.getProperty("failureType"),
                    p.getProperty("failureMessage"), Long.parseLong(required(p, "revision")),
                    Instant.parse(required(p, "createdAt")), Instant.parse(required(p, "updatedAt")));
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Cannot read collection deletion journal record " + path, e);
        }
    }

    private static byte[] encode(CollectionDeletionOperation operation) {
        Properties p = new Properties();
        p.setProperty("collectionId", operation.collectionId());
        p.setProperty("status", operation.status().name());
        p.setProperty("revision", Long.toString(operation.revision()));
        p.setProperty("createdAt", operation.createdAt().toString());
        p.setProperty("updatedAt", operation.updatedAt().toString());
        p.setProperty("completed.count", Integer.toString(operation.completedStoreIds().size()));
        for (int i = 0; i < operation.completedStoreIds().size(); i++)
            p.setProperty("completed." + i, operation.completedStoreIds().get(i));
        putIfPresent(p, "failedComponentId", operation.failedComponentId());
        putIfPresent(p, "failureType", operation.failureType());
        putIfPresent(p, "failureMessage", operation.failureMessage());
        try {
            var output = new java.io.ByteArrayOutputStream();
            p.store(output, "Pinisi collection deletion journal");
            return output.toByteArray();
        } catch (IOException impossible) {
            throw new UncheckedIOException(impossible);
        }
    }

    private void writeAtomically(Path target, byte[] content) {
        Path temporary = null;
        try {
            temporary = Files.createTempFile(directory, ".deletion-", ".tmp");
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                var buffer = java.nio.ByteBuffer.wrap(content);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot persist collection deletion journal", e);
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
        }
    }

    private <T> T locked(java.util.concurrent.Callable<T> action) {
        synchronized (jvmLock) {
            try (FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock ignored = channel.lock()) {
                return action.call();
            } catch (IOException e) {
                throw new UncheckedIOException("Collection deletion journal I/O failure", e);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException("Collection deletion journal operation failed", e);
            }
        }
    }

    private Path operationPath(String id) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(id.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return directory.resolve(HexFormat.of().formatHex(digest) + ".properties");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static String required(Properties p, String key) {
        String value = p.getProperty(key);
        if (value == null) throw new IllegalStateException("Missing journal property: " + key);
        return value;
    }
    private static void putIfPresent(Properties p, String key, String value) {
        if (value != null) p.setProperty(key, value);
    }
    private static String requireId(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("collectionId must not be blank");
        return id;
    }
}
