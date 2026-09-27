package com.example.modgen.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Per-module aggregation inside an {@link ApiIndex}: parsed API classes, documentation
 * sections and configuration properties for one feature module (blocks, items, registration...).
 */
public final class ModuleIndex {

    private final String name;
    private final String displayName;
    private final String description;
    private final List<ApiClass> classes;
    private final List<DocSection> docs;
    private final List<ConfigProperty> configProperties;

    private ModuleIndex(Builder builder) {
        this.name = builder.name;
        this.displayName = builder.displayName == null ? builder.name : builder.displayName;
        this.description = builder.description == null ? "" : builder.description;
        this.classes = List.copyOf(builder.classes);
        this.docs = List.copyOf(builder.docs);
        this.configProperties = List.copyOf(builder.configProperties);
    }

    public String name() {
        return name;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public List<ApiClass> classes() {
        return classes;
    }

    public List<DocSection> docs() {
        return docs;
    }

    public List<ConfigProperty> configProperties() {
        return configProperties;
    }

    public Optional<ApiClass> classBySimpleName(String simpleName) {
        return classes.stream().filter(c -> c.simpleName().equals(simpleName)).findFirst();
    }

    public List<CodeExample> codeExamples() {
        return docs.stream().flatMap(d -> d.codeExamples().stream()).toList();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private String displayName;
        private String description;
        private final List<ApiClass> classes = new ArrayList<>();
        private final List<DocSection> docs = new ArrayList<>();
        private final List<ConfigProperty> configProperties = new ArrayList<>();

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder classes(List<ApiClass> classes) {
            this.classes.addAll(classes);
            return this;
        }

        public Builder docs(List<DocSection> docs) {
            this.docs.addAll(docs);
            return this;
        }

        public Builder configProperties(List<ConfigProperty> configProperties) {
            this.configProperties.addAll(configProperties);
            return this;
        }

        public ModuleIndex build() {
            return new ModuleIndex(this);
        }
    }
}
