package com.example.modgen.core.parse.neoforge;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Configuration for the NeoForge documentation parser.
 *
 * @param owner            GitHub owner of the documentation repository
 * @param repository       GitHub repository name
 * @param branch           GitHub branch (default {@code main})
 * @param docsPathPrefix   documentation directory prefix, e.g. {@code versioned_docs/version-1.21.1}
 * @param minecraftVersion Minecraft version, e.g. {@code 1.21.1}
 * @param loaderVersion    NeoForge loader version, e.g. {@code 21.1.86}
 * @param modules          requested feature modules; empty set = parse everything
 * @param javaSourceDirs   extracted NeoForge source directories consumed by QDox
 */
public record NeoForgeDocConfig(
        String owner,
        String repository,
        String branch,
        String docsPathPrefix,
        String minecraftVersion,
        String loaderVersion,
        Set<String> modules,
        List<Path> javaSourceDirs) {

    public NeoForgeDocConfig {
        modules = modules == null ? Set.of() : Set.copyOf(modules);
        javaSourceDirs = javaSourceDirs == null ? List.of() : List.copyOf(javaSourceDirs);
    }

    /**
     * Default configuration for a versioned docs tree of a specific Minecraft version.
     */
    public static NeoForgeDocConfig forMinecraftVersion(
            String minecraftVersion, String loaderVersion, Set<String> modules, List<Path> javaSourceDirs) {
        return new NeoForgeDocConfig(
                "neoforged",
                "Documentation",
                "main",
                "versioned_docs/version-" + minecraftVersion,
                minecraftVersion,
                loaderVersion,
                modules,
                javaSourceDirs);
    }

    /**
     * Configuration for the un-versioned "latest" docs on the default branch.
     */
    public static NeoForgeDocConfig forLatestDocs(
            String loaderVersion, Set<String> modules, List<Path> javaSourceDirs) {
        return new NeoForgeDocConfig(
                "neoforged",
                "Documentation",
                "main",
                "docs",
                "latest",
                loaderVersion,
                modules,
                javaSourceDirs);
    }
}
