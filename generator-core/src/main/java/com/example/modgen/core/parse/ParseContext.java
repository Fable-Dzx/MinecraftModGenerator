package com.example.modgen.core.parse;

import com.example.modgen.core.model.Loader;
import com.example.modgen.core.source.DocumentationSource;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Immutable input to {@link DocumentationParser#parse(ParseContext)}.
 *
 * @param source          documentation repository source
 * @param loader          mod loader (must match the parser implementation)
 * @param minecraftVersion Minecraft version, e.g. {@code 1.21.1}
 * @param docsPathPrefix  documentation directory prefix, e.g. {@code versioned_docs/version-1.21.1}
 *                        (or {@code docs} for the latest docs on the default branch)
 * @param modules         requested feature modules; an empty set means "parse everything"
 * @param javaSourceDirs  extracted NeoForge source directories for QDox; may be empty for docs-only parsing
 * @param loaderVersion   loader version, e.g. {@code 21.1.86} (used for provenance and source jar lookup)
 */
public record ParseContext(
        DocumentationSource source,
        Loader loader,
        String minecraftVersion,
        String docsPathPrefix,
        Set<String> modules,
        List<Path> javaSourceDirs,
        String loaderVersion) {

    public ParseContext {
        modules = modules == null ? Set.of() : Set.copyOf(modules);
        javaSourceDirs = javaSourceDirs == null ? List.of() : List.copyOf(javaSourceDirs);
        docsPathPrefix = docsPathPrefix == null ? "" : docsPathPrefix;
        loaderVersion = loaderVersion == null ? "" : loaderVersion;
    }

    /**
     * Whether the given module should be parsed. Empty module set = all modules.
     */
    public boolean requestsModule(String module) {
        return modules.isEmpty() || modules.contains(module);
    }
}
