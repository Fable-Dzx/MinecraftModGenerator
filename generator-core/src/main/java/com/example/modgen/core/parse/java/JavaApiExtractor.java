package com.example.modgen.core.parse.java;

import com.example.modgen.core.model.ApiClass;
import com.example.modgen.core.model.ApiField;
import com.example.modgen.core.model.ApiMethod;
import com.example.modgen.core.model.ApiParameter;
import com.example.modgen.core.model.ClassKind;
import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameter;
import com.thoughtworks.qdox.model.JavaType;
import com.thoughtworks.qdox.model.JavaTypeVariable;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Extracts class/method signatures and Javadoc from Java sources (NeoForge sources or
 * decompiled artifacts) using QDox.
 *
 * <p>The extractor only reads declared members of the requested classes; inherited members
 * are intentionally excluded so that the ApiIndex reflects the API surface a mod developer
 * actually calls on these classes.</p>
 */
public final class JavaApiExtractor {

    private static final Logger LOG = System.getLogger(JavaApiExtractor.class.getName());

    private final JavaProjectBuilder builder;
    private final Map<String, JavaClass> classesByFqn;

    /**
     * @param sourceDirectories directories whose {@code .java} files form the QDox source tree
     */
    public JavaApiExtractor(List<Path> sourceDirectories) throws IOException {
        this.builder = new JavaProjectBuilder();
        for (Path directory : sourceDirectories) {
            if (!Files.isDirectory(directory)) {
                throw new IOException("Source directory does not exist: " + directory);
            }
            builder.addSourceTree(directory.toFile());
        }
        this.classesByFqn = indexRealClasses(builder);
    }

    /**
     * Indexes only classes actually parsed from the source trees. QDox's
     * {@code getClassByName} fabricates unresolved placeholder classes for unknown names,
     * which pollutes extraction; {@code getClasses()} never does.
     */
    private static Map<String, JavaClass> indexRealClasses(JavaProjectBuilder builder) {
        Map<String, JavaClass> index = new HashMap<>();
        for (JavaClass javaClass : builder.getClasses()) {
            String fqn = javaClass.getFullyQualifiedName();
            index.putIfAbsent(fqn, javaClass);
            // also index the $-separated form so nested-class lookups are unambiguous
            index.putIfAbsent(dollarSeparated(fqn), javaClass);
        }
        return Map.copyOf(index);
    }

    /**
     * Extracts the requested classes by fully qualified name. Missing classes are logged and
     * skipped; nested classes use the canonical dotted form (e.g. {@code DeferredRegister.Blocks}).
     *
     * @param classFqns canonical dotted FQNs, nested classes may use dots (e.g. {@code DeferredRegister.Blocks})
     */
    public List<ApiClass> extract(List<String> classFqns) {
        return classFqns.stream().map(this::extractOne).flatMap(Optional::stream).toList();
    }

    private Optional<ApiClass> extractOne(String fqn) {
        JavaClass javaClass = classesByFqn.get(fqn);
        if (javaClass == null) {
            LOG.log(Level.WARNING, "Class not found in source tree: {0}", fqn);
            return Optional.empty();
        }
        return Optional.of(toApiClass(javaClass, fqn));
    }

    /** {@code a.b.Outer.Inner} -> {@code a.b.Outer$Inner} (QDox nested-class separator). */
    private static String dollarSeparated(String fqn) {
        int dot = fqn.lastIndexOf('.');
        if (dot < 0) {
            return fqn;
        }
        return fqn.substring(0, dot) + "$" + fqn.substring(dot + 1);
    }

    private ApiClass toApiClass(JavaClass javaClass, String fqn) {
        List<ApiMethod> methods = javaClass.getMethods().stream().map(this::toApiMethod).toList();
        List<ApiField> fields = javaClass.getFields().stream().map(this::toApiField).toList();
        List<String> typeParameters = javaClass.getTypeParameters().stream()
                .map(JavaTypeVariable::getName)
                .toList();
        return ApiClass.builder()
                .fqn(fqn)
                .simpleName(javaClass.getName())
                .packageName(javaClass.getPackageName())
                .kind(classKind(javaClass))
                .javaDoc(clean(javaClass.getComment()))
                .sourcePath(javaClass.getSource() == null ? "" : String.valueOf(javaClass.getSource().getURL()))
                .typeParameters(typeParameters)
                .methods(methods)
                .fields(fields)
                .build();
    }

    private ApiMethod toApiMethod(JavaMethod method) {
        List<ApiParameter> parameters = method.getParameters().stream()
                .map(p -> new ApiParameter(
                        p.getType().getGenericValue(),
                        p.getName(),
                        clean(p.getComment())))
                .toList();
        String typeParameters = method.getTypeParameters().isEmpty()
                ? ""
                : method.getTypeParameters().stream()
                        .map(this::typeParameterText)
                        .collect(Collectors.joining(", ", "<", ">"));
        String parametersText = parameters.stream()
                .map(ApiParameter::declaration)
                .collect(Collectors.joining(", "));
        List<String> signatureParts = new ArrayList<>(modifiersOf(method));
        if (!typeParameters.isEmpty()) {
            signatureParts.add(typeParameters);
        }
        signatureParts.add(method.getReturnType().getGenericValue());
        signatureParts.add(method.getName() + "(" + parametersText + ")");
        return new ApiMethod(
                method.getName(),
                String.join(" ", signatureParts),
                parameters,
                method.getReturnType().getGenericValue(),
                clean(method.getComment()),
                method.isStatic(),
                modifiersOf(method));
    }

    /** Renders a type parameter including bounds, e.g. {@code B extends Block}. */
    private String typeParameterText(JavaTypeVariable<?> typeVariable) {
        String name = typeVariable.getName();
        List<JavaType> bounds = typeVariable.getBounds();
        if (bounds == null || bounds.isEmpty()) {
            return name;
        }
        String boundText = bounds.stream()
                .map(type -> type.getGenericValue())
                .collect(Collectors.joining(" & "));
        return name + " extends " + boundText;
    }

    private ApiField toApiField(JavaField field) {
        return new ApiField(
                field.getName(),
                field.getType().getGenericValue(),
                clean(field.getComment()),
                field.isStatic());
    }

    private static ClassKind classKind(JavaClass javaClass) {
        if (javaClass.isAnnotation()) {
            return ClassKind.ANNOTATION;
        }
        if (javaClass.isEnum()) {
            return ClassKind.ENUM;
        }
        if (javaClass.isInterface()) {
            return ClassKind.INTERFACE;
        }
        return ClassKind.CLASS;
    }

    private static List<String> modifiersOf(JavaMethod method) {
        List<String> modifiers = new ArrayList<>();
        if (method.isPublic()) {
            modifiers.add("public");
        } else if (method.isProtected()) {
            modifiers.add("protected");
        } else if (method.isPrivate()) {
            modifiers.add("private");
        }
        if (method.isStatic()) {
            modifiers.add("static");
        }
        if (method.isFinal()) {
            modifiers.add("final");
        }
        if (method.isAbstract()) {
            modifiers.add("abstract");
        }
        if (method.isSynchronized()) {
            modifiers.add("synchronized");
        }
        return List.copyOf(modifiers);
    }

    private static String clean(String comment) {
        return comment == null ? "" : comment.strip();
    }
}
