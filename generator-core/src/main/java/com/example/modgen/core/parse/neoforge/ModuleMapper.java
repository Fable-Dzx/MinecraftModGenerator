package com.example.modgen.core.parse.neoforge;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Maps documentation file paths (relative to the docs prefix) to feature modules.
 * By default the first path segment is the module; exact-path overrides special-case
 * files like {@code concepts/registries.md} -> {@code registration}.
 */
public final class ModuleMapper {

    private final Map<String, String> overrides;
    private final String fallback;

    public ModuleMapper(Map<String, String> overrides, String fallback) {
        this.overrides = new LinkedHashMap<>(Objects.requireNonNull(overrides, "overrides"));
        this.fallback = Objects.requireNonNull(fallback, "fallback");
    }

    /**
     * The default NeoForge mapping used by {@link NeoForgeDocumentationParser}.
     */
    public static ModuleMapper neoforgeDefault() {
        return new ModuleMapper(Map.of(
                "concepts/registries.md", "registration",
                "concepts/registries/index.md", "registration"), "concepts");
    }

    /**
     * Resolves the module for a doc path like {@code blocks/index.md}.
     */
    public String moduleFor(String relativePath) {
        String exact = overrides.get(relativePath);
        if (exact != null) {
            return exact;
        }
        int slash = relativePath.indexOf('/');
        String firstSegment = slash < 0 ? relativePath : relativePath.substring(0, slash);
        return firstSegment.isEmpty() ? fallback : firstSegment;
    }
}
