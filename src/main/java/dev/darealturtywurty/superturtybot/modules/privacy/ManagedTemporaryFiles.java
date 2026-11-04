package dev.darealturtywurty.superturtybot.modules.privacy;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ManagedTemporaryFiles {
    public static final ManagedTemporaryFiles INSTANCE = new ManagedTemporaryFiles(
        Path.of(System.getProperty("java.io.tmpdir"), "superturtybot"));
    private final Path root;
    private final Set<Path> active = ConcurrentHashMap.newKeySet();

    public ManagedTemporaryFiles(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public Path createDirectory(String prefix) throws IOException {
        prepare();
        Path directory = Files.createTempDirectory(root, prefix);
        active.add(directory);
        return directory;
    }

    public Path createFile(String prefix, String suffix) throws IOException {
        prepare();
        Path file = Files.createTempFile(root, prefix, suffix);
        active.add(file);
        return file;
    }

    public void release(Path path) throws IOException {
        Path target = validate(path);
        try {
            delete(target);
        } finally {
            active.remove(target);
        }
    }

    public long cleanup(Instant now) throws IOException {
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS))
            return 0;
        prepare();
        Instant cutoff = now.minus(Duration.ofHours(24));
        long removed = 0;
        IOException failure = null;
        try (var entries = Files.newDirectoryStream(root)) {
            for (Path entry : entries) {
                Path target = validate(entry);
                if (Files.isSymbolicLink(target) || active.stream().anyMatch(path -> path.startsWith(target)))
                    continue;
                try {
                    if (isOlderThan(target, cutoff)) {
                        delete(target);
                        removed++;
                    }
                } catch (IOException exception) {
                    failure = exception;
                }
            }
        }
        if (failure != null)
            throw failure;
        return removed;
    }

    private void prepare() throws IOException {
        Files.createDirectories(root);
        if (Files.isSymbolicLink(root) || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("The managed temporary directory must not be a symbolic link");
    }

    private Path validate(Path path) throws IOException {
        Path target = path.toAbsolutePath().normalize();
        if (target.equals(root) || !root.equals(target.getParent()))
            throw new IOException("Temporary cleanup target is outside the managed directory");
        if (Files.isSymbolicLink(root))
            throw new IOException("The managed temporary directory must not be a symbolic link");
        return target;
    }

    private static boolean isOlderThan(Path target, Instant cutoff) throws IOException {
        boolean[] old = {true};
        Files.walkFileTree(target, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                if (attributes.lastModifiedTime().toInstant().isAfter(cutoff)) {
                    old[0] = false;
                }
                return old[0] ? FileVisitResult.CONTINUE : FileVisitResult.TERMINATE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                if (attributes.lastModifiedTime().toInstant().isAfter(cutoff)) {
                    old[0] = false;
                }
                return old[0] ? FileVisitResult.CONTINUE : FileVisitResult.TERMINATE;
            }
        });
        return old[0];
    }

    private void delete(Path target) throws IOException {
        validate(target);
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS))
            return;
        Files.walkFileTree(target, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException {
                if (exception != null)
                    throw exception;
                Files.deleteIfExists(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
