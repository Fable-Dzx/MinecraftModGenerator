package com.example.modgen.core.model;

/**
 * A field extracted from Java source via QDox.
 *
 * @param name     field name
 * @param type     generic type name
 * @param javaDoc  Javadoc of the field, may be empty
 * @param isStatic whether the field is static
 */
public record ApiField(String name, String type, String javaDoc, boolean isStatic) {

    public ApiField {
        javaDoc = javaDoc == null ? "" : javaDoc;
    }
}
