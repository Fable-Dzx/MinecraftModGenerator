package com.example.modgen.core.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;

/**
 * Top-level structured API index: the result of the documentation parsing pipeline.
 * Organized by loader + Minecraft version + feature module.
 * Immutable after construction; built through {@link #builder()}.
 */
public final class ApiIndex {

    private final Loader loader;
    private final String minecraftVersion;
    private final String loaderVersion;
    private final SequencedMap<String, ModuleIndex> modules;
    private final Map<String, ApiClass> classesByFqn;

    private ApiIndex(Builder builder) {
        this.loader = builder.loader;
        this.minecraftVersion = builder.minecraftVersion;
        this.loaderVersion = builder.loaderVersion;
        this.modules = java.util.Collections.unmodifiableSequencedMap(
                new LinkedHashMap<>(builder.modules));
        Map<String, ApiClass> flat = new LinkedHashMap<>();
        for (ModuleIndex module : builder.modules.values()) {
            for (ApiClass apiClass : module.classes()) {
                flat.putIfAbsent(apiClass.fqn(), apiClass);
            }
        }
        this.classesByFqn = Map.copyOf(flat);
    }

    public Loader loader() {
        return loader;
    }

    public String minecraftVersion() {
        return minecraftVersion;
    }

    public String loaderVersion() {
        return loaderVersion;
    }

    /**
     * Modules in first-seen order.
     */
    public SequencedMap<String, ModuleIndex> modules() {
        return modules;
    }

    public Map<String, ApiClass> classesByFqn() {
        return classesByFqn;
    }

    public Optional<ApiClass> classByFqn(String fqn) {
        return Optional.ofNullable(classesByFqn.get(fqn));
    }

    /**
     * Overload-aware method lookup: class by FQN, then method by name and parameter count.
     */
    public Optional<ApiMethod> findMethod(String className, String methodName, int parameterCount) {
        return classByFqn(className).flatMap(c -> c.method(methodName, parameterCount));
    }

    public List<CodeExample> examplesForModule(String module) {
        ModuleIndex index = modules.get(module);
        return index == null ? List.of() : index.codeExamples();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Loader loader;
        private String minecraftVersion = "";
        private String loaderVersion = "";
        private final LinkedHashMap<String, ModuleIndex> modules = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder loader(Loader loader) {
            this.loader = loader;
            return this;
        }

        public Builder minecraftVersion(String minecraftVersion) {
            this.minecraftVersion = minecraftVersion;
            return this;
        }

        public Builder loaderVersion(String loaderVersion) {
            this.loaderVersion = loaderVersion;
            return this;
        }

        public Builder addModule(ModuleIndex module) {
            modules.put(module.name(), module);
            return this;
        }

        public boolean hasModule(String name) {
            return modules.containsKey(name);
        }

        public ApiIndex build() {
            return new ApiIndex(this);
        }
    }
}
