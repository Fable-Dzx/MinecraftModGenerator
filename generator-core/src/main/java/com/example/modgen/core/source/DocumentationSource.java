package com.example.modgen.core.source;

import java.io.IOException;
import java.util.List;

/**
 * Abstraction over a documentation repository. Implementations may read from a local
 * checkout ({@link LocalDirectorySource}) or from the GitHub API ({@link GitHubApiSource}).
 * All paths are relative and use {@code '/'} separators.
 */
public sealed interface DocumentationSource permits LocalDirectorySource, GitHubApiSource {

    /**
     * Human readable name used in error messages and provenance tracking.
     */
    String name();

    /**
     * All Markdown ({@code .md}) file paths under the given directory prefix,
     * e.g. {@code versioned_docs/version-1.21.1}.
     */
    List<String> listMarkdownFiles(String directoryPrefix) throws IOException;

    /**
     * Raw text content of the file at the given relative path.
     */
    String read(String path) throws IOException;
}
