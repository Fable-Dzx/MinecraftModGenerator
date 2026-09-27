package com.example.modgen.core.model;

/**
 * A single method parameter extracted from either Java source (QDox) or documentation text.
 *
 * @param type    fully qualified or generic type name, e.g. {@code DeferredRegister.Blocks} or {@code Supplier<? extends T>}
 * @param name    parameter name (may be empty when inferred from documentation)
 * @param javaDoc parameter Javadoc, may be empty
 */
public record ApiParameter(String type, String name, String javaDoc) {

    public ApiParameter {
        name = name == null ? "" : name;
        javaDoc = javaDoc == null ? "" : javaDoc;
    }

    /**
     * Human readable declaration of this parameter, e.g. {@code String modid}.
     */
    public String declaration() {
        return name.isEmpty() ? type : type + " " + name;
    }
}
