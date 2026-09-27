package com.example.modgen.core.model;

/**
 * A reference to an API type found inside documentation (inline code or Javadoc link).
 * The {@code fqn} is resolved during the merge phase: first against QDox-parsed classes,
 * then against the built-in {@link com.example.modgen.core.parse.neoforge.KnownApiDictionary}.
 *
 * @param simpleName simple class name as written in the documentation, e.g. {@code DeferredRegister}
 * @param fqn        resolved fully qualified name; empty string when unresolvable
 * @param context    surrounding heading/paragraph used to disambiguate the reference
 * @param sourcePath documentation file for provenance
 */
public record ApiReference(String simpleName, String fqn, String context, String sourcePath) {

    public ApiReference {
        simpleName = simpleName == null ? "" : simpleName;
        fqn = fqn == null ? "" : fqn;
        context = context == null ? "" : context;
        sourcePath = sourcePath == null ? "" : sourcePath;
    }

    public boolean resolved() {
        return !fqn.isEmpty();
    }

    public ApiReference withFqn(String resolvedFqn) {
        return new ApiReference(simpleName, resolvedFqn, context, sourcePath);
    }
}
