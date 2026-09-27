package com.example.modgen.core.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable description of a Java class/interface/enum extracted from NeoForge sources (QDox).
 * FQNs use the canonical dotted form ({@code net.neoforged.neoforge.registries.DeferredRegister.Blocks}),
 * independent of the nested-class separator used by the underlying parser.
 */
public final class ApiClass {

    private final String fqn;
    private final String simpleName;
    private final String packageName;
    private final ClassKind kind;
    private final String javaDoc;
    private final String sourcePath;
    private final List<String> typeParameters;
    private final List<ApiMethod> methods;
    private final List<ApiField> fields;

    private ApiClass(Builder builder) {
        this.fqn = Objects.requireNonNull(builder.fqn, "fqn");
        this.simpleName = Objects.requireNonNull(builder.simpleName, "simpleName");
        this.packageName = builder.packageName == null ? "" : builder.packageName;
        this.kind = builder.kind == null ? ClassKind.CLASS : builder.kind;
        this.javaDoc = builder.javaDoc == null ? "" : builder.javaDoc;
        this.sourcePath = builder.sourcePath == null ? "" : builder.sourcePath;
        this.typeParameters = List.copyOf(builder.typeParameters);
        this.methods = List.copyOf(builder.methods);
        this.fields = List.copyOf(builder.fields);
    }

    public String fqn() {
        return fqn;
    }

    public String simpleName() {
        return simpleName;
    }

    public String packageName() {
        return packageName;
    }

    public ClassKind kind() {
        return kind;
    }

    public String javaDoc() {
        return javaDoc;
    }

    public String sourcePath() {
        return sourcePath;
    }

    public List<String> typeParameters() {
        return typeParameters;
    }

    public List<ApiMethod> methods() {
        return methods;
    }

    public List<ApiField> fields() {
        return fields;
    }

    /**
     * Finds the first method with the given name and parameter count (overload resolution).
     */
    public Optional<ApiMethod> method(String name, int parameterCount) {
        return methods.stream()
                .filter(m -> m.name().equals(name) && m.parameters().size() == parameterCount)
                .findFirst();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String fqn;
        private String simpleName;
        private String packageName;
        private ClassKind kind;
        private String javaDoc;
        private String sourcePath;
        private final List<String> typeParameters = new java.util.ArrayList<>();
        private final List<ApiMethod> methods = new java.util.ArrayList<>();
        private final List<ApiField> fields = new java.util.ArrayList<>();

        private Builder() {
        }

        public Builder fqn(String fqn) {
            this.fqn = fqn;
            return this;
        }

        public Builder simpleName(String simpleName) {
            this.simpleName = simpleName;
            return this;
        }

        public Builder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder kind(ClassKind kind) {
            this.kind = kind;
            return this;
        }

        public Builder javaDoc(String javaDoc) {
            this.javaDoc = javaDoc;
            return this;
        }

        public Builder sourcePath(String sourcePath) {
            this.sourcePath = sourcePath;
            return this;
        }

        public Builder typeParameters(List<String> typeParameters) {
            this.typeParameters.addAll(typeParameters);
            return this;
        }

        public Builder methods(List<ApiMethod> methods) {
            this.methods.addAll(methods);
            return this;
        }

        public Builder fields(List<ApiField> fields) {
            this.fields.addAll(fields);
            return this;
        }

        public ApiClass build() {
            return new ApiClass(this);
        }
    }
}
