package com.example.modgen.core.model;

import java.util.List;

/**
 * A method signature extracted from Java source via QDox.
 *
 * @param name       method name
 * @param signature  full textual signature, e.g.
 *                   {@code public static <B extends Block> DeferredHolder<Block, B> registerBlock(String name, Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties properties)}
 * @param parameters ordered parameter list
 * @param returnType generic return type, may be empty for void
 * @param javaDoc    Javadoc of the method, may be empty
 * @param isStatic   whether the method is static
 * @param modifiers  modifier keywords in declaration order (public/private/protected/static/final/abstract/...)
 */
public record ApiMethod(
        String name,
        String signature,
        List<ApiParameter> parameters,
        String returnType,
        String javaDoc,
        boolean isStatic,
        List<String> modifiers) {

    public ApiMethod {
        parameters = List.copyOf(parameters);
        modifiers = List.copyOf(modifiers);
        javaDoc = javaDoc == null ? "" : javaDoc;
        returnType = returnType == null ? "" : returnType;
    }
}
