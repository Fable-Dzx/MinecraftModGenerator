package com.example.modgen.core.source;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * {@link DocumentationSource} backed by a local directory (a git checkout or a test fixture).
 * Markdown listing walks the directory tree non-recursively below the requested prefix.
 */
public final class LocalDirectorySource implements DocumentationSource {

    private final Path root;
    private final String name;

    public LocalDirectorySource(Path root) {
        this(root, root.getFileName().toString());
    }

    public LocalDirectorySource(Path root, String name) {
        this.root = root.toAbsolutePath().normalize();
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<String> listMarkdownFiles(String directoryPrefix) throws IOException {
        Path dir = root.resolve(directoryPrefix).normalize();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            return paths.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".md"))
                    .map(p -> root.relativize(p).toString().replace('\\', '/'))
                    .sorted()
                    .toList();
        }
    }

    @Override
    public String read(String path) throws IOException {
        Path file = root.resolve(path).normalize();
        if (!file.startsWith(root)) {
            throw new IOException("Path escapes source root: " + path);
        }
        try {
            return Files.readString(file);
        } catch (UncheckedIOException | IOException e) {
            throw new IOException("Failed to read " + path + " from " + name, e);
        }
    }
}
