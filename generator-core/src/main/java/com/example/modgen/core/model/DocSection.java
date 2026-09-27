package com.example.modgen.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured documentation extracted from a single Markdown/HTML source file:
 * title, code examples, API references, tables, notes (admonitions) and config properties.
 * The {@code module} is assigned by the parser during the merge phase.
 */
public final class DocSection {

    private final String module;
    private final String title;
    private final String path;
    private final List<CodeExample> codeExamples;
    private final List<ApiReference> apiReferences;
    private final List<DocTable> tables;
    private final List<String> notes;
    private final List<ConfigProperty> configProperties;

    private DocSection(Builder builder) {
        this.module = builder.module;
        this.title = builder.title;
        this.path = builder.path;
        this.codeExamples = List.copyOf(builder.codeExamples);
        this.apiReferences = List.copyOf(builder.apiReferences);
        this.tables = List.copyOf(builder.tables);
        this.notes = List.copyOf(builder.notes);
        this.configProperties = List.copyOf(builder.configProperties);
    }

    public String module() {
        return module;
    }

    public String title() {
        return title;
    }

    public String path() {
        return path;
    }

    public List<CodeExample> codeExamples() {
        return codeExamples;
    }

    public List<ApiReference> apiReferences() {
        return apiReferences;
    }

    public List<DocTable> tables() {
        return tables;
    }

    public List<String> notes() {
        return notes;
    }

    public List<ConfigProperty> configProperties() {
        return configProperties;
    }

    /**
     * Returns a copy with the given module assigned (to the section and its config properties).
     */
    public DocSection withModule(String newModule) {
        return builder()
                .module(newModule)
                .title(title)
                .path(path)
                .codeExamples(codeExamples)
                .apiReferences(apiReferences)
                .tables(tables)
                .notes(notes)
                .configProperties(configProperties.stream().map(p -> p.withModule(newModule)).toList())
                .build();
    }

    /**
     * Returns a copy with the given (resolved) API references.
     */
    public DocSection withApiReferences(List<ApiReference> newApiReferences) {
        return builder()
                .module(module)
                .title(title)
                .path(path)
                .codeExamples(codeExamples)
                .apiReferences(newApiReferences)
                .tables(tables)
                .notes(notes)
                .configProperties(configProperties)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String module = "";
        private String title = "";
        private String path = "";
        private final List<CodeExample> codeExamples = new ArrayList<>();
        private final List<ApiReference> apiReferences = new ArrayList<>();
        private final List<DocTable> tables = new ArrayList<>();
        private final List<String> notes = new ArrayList<>();
        private final List<ConfigProperty> configProperties = new ArrayList<>();

        private Builder() {
        }

        public Builder module(String module) {
            this.module = module;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder codeExamples(List<CodeExample> codeExamples) {
            this.codeExamples.addAll(codeExamples);
            return this;
        }

        public Builder apiReferences(List<ApiReference> apiReferences) {
            this.apiReferences.addAll(apiReferences);
            return this;
        }

        public Builder tables(List<DocTable> tables) {
            this.tables.addAll(tables);
            return this;
        }

        public Builder notes(List<String> notes) {
            this.notes.addAll(notes);
            return this;
        }

        public Builder configProperties(List<ConfigProperty> configProperties) {
            this.configProperties.addAll(configProperties);
            return this;
        }

        public DocSection build() {
            return new DocSection(this);
        }
    }
}
